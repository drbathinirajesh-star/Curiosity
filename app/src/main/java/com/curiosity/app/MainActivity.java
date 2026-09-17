package com.curiosity.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private TextView status;
    private Button listen;
    private SpeechRecognizer recognizer;
    private TextToSpeech speaker;

    // PUT YOUR OPENAI API KEY HERE
    private static final String API_KEY = "PASTE_YOUR_API_KEY_HERE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        listen = findViewById(R.id.listen);

        speaker = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                speaker.setLanguage(Locale.US);
            }
        });

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("Listening...");
            }

            @Override
            public void onBeginningOfSpeech() {
                status.setText("I'm listening...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("Thinking...");
            }

            @Override
            public void onError(int error) {
                status.setText("I couldn't hear you. Try again.");
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {

                    String question = matches.get(0);

                    status.setText("You: " + question);

                    askAI(question);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        listen.setOnClickListener(v -> startListening());
    }

    private void startListening() {

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN");

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Talk to Curiosity");

        recognizer.startListening(intent);
    }

    private void askAI(String question) {

        status.setText("Curiosity is thinking...");

        new Thread(() -> {

            try {

                URL url = new URL(
                        "https://api.openai.com/v1/responses");

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + API_KEY);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json");

                connection.setDoOutput(true);

                JSONObject body = new JSONObject();

                body.put("model", "gpt-5.6-luna");

                body.put(
                        "instructions",
                        "You are Curiosity, a friendly helpful voice AI assistant. " +
                        "Answer naturally and clearly. Keep spoken answers reasonably concise.");

                JSONArray input = new JSONArray();

                JSONObject message = new JSONObject();

                message.put("role", "user");
                message.put("content", question);

                input.put(message);

                body.put("input", input);

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        body.toString().getBytes("UTF-8"));

                output.close();

                int responseCode =
                        connection.getResponseCode();

                BufferedReader reader;

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()));

                } else {

                    reader = new BufferedReader(
                            new InputStreamReader(
                                    connection.getErrorStream()));
                }

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(response.toString());

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    String answer =
                            extractAnswer(json);

                    runOnUiThread(() -> speak(answer));

                } else {

                    runOnUiThread(() ->
                            status.setText(
                                    "AI error: " + responseCode));
                }

                connection.disconnect();

            } catch (Exception e) {

                runOnUiThread(() ->
                        status.setText(
                                "Connection error: " +
                                e.getMessage()));
            }

        }).start();
    }

    private String extractAnswer(JSONObject json) {

        try {

            if (json.has("output_text")) {
                return json.getString("output_text");
            }

            JSONArray output =
                    json.getJSONArray("output");

            for (int i = 0; i < output.length(); i++) {

                JSONObject item =
                        output.getJSONObject(i);

                if (item.has("content")) {

                    JSONArray content =
                            item.getJSONArray("content");

                    for (int j = 0; j < content.length(); j++) {

                        JSONObject part =
                                content.getJSONObject(j);

                        if (part.has("text")) {
                            return part.getString("text");
                        }
                    }
                }
            }

        } catch (Exception ignored) {}

        return "Sorry, I couldn't understand the AI response.";
    }

    private void speak(String answer) {

        status.setText(answer);

        if (speaker != null) {

            speaker.speak(
                    answer,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "curiosity_response");
        }
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
