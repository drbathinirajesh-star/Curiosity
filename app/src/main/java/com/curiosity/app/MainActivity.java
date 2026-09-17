package com.curiosity.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status;
    private Button listen;
    private SpeechRecognizer recognizer;
    private TextToSpeech speaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        listen = findViewById(R.id.listen);

        speaker = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                speaker.setLanguage(Locale.getDefault());
            }
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    100
            );
        }

        listen.setOnClickListener(v -> startListening());
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition is not available.");
            return;
        }

        status.setText("Listening...");

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                status.setText("Thinking...");
            }

            @Override
            public void onError(int error) {
                status.setText("Speech error: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null && !matches.isEmpty()) {
                    askAI(matches.get(0));
                } else {
                    status.setText("I didn't hear that.");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        recognizer.startListening(intent);
    }

    private void askAI(String question) {
        status.setText("Thinking...");

        new Thread(() -> {
            try {
                String apiKey = BuildConfig.OPENAI_API_KEY;

                if (apiKey == null || apiKey.isEmpty()) {
                    runOnUiThread(() ->
                            status.setText("API key is not configured.")
                    );
                    return;
                }

                URL url = new URL(
                        "https://api.openai.com/v1/responses"
                );

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + apiKey
                );
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("model", "gpt-5.6-luna");
                body.put("input", question);

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        body.toString().getBytes("UTF-8")
                );
                output.close();

                int responseCode =
                        connection.getResponseCode();

                BufferedReader reader;

                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );
                } else {
                    reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getErrorStream()
                            )
                    );
                }

                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode < 200 || responseCode >= 300) {
                    runOnUiThread(() ->
                            status.setText(
                                    "AI error: " + responseCode
                            )
                    );
                    return;
                }

                JSONObject json =
                        new JSONObject(response.toString());

                String answer = extractText(json);

                runOnUiThread(() -> {
                    status.setText(answer);
                    speaker.speak(
                            answer,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "curiosity"
                    );
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        status.setText(
                                "Connection error: " + e.getMessage()
                        )
                );
            }
        }).start();
    }

    private String extractText(JSONObject json) {
        try {
            JSONArray output = json.getJSONArray("output");

            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.getJSONObject(i);

                if ("message".equals(item.optString("type"))) {
                    JSONArray content =
                            item.getJSONArray("content");

                    for (int j = 0; j < content.length(); j++) {
                        JSONObject part =
                                content.getJSONObject(j);

                        if ("output_text".equals(
                                part.optString("type"))) {

                            return part.optString("text");
                        }
                    }
                }
            }

        } catch (Exception ignored) {
        }

        return "I couldn't understand the AI response.";
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
        }

        if (speaker != null) {
            speaker.stop();
            speaker.shutdown();
        }

        super.onDestroy();
    }
}
