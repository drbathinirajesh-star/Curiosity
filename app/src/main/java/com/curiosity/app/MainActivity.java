package com.curiosity.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
    private ImageView imageView;
    private SpeechRecognizer recognizer;
    private TextToSpeech speaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        listen = findViewById(R.id.listen);

        // Create image area programmatically.
        imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        ViewGroup root = (ViewGroup) status.getParent();

        int index = root.indexOfChild(listen);

        root.addView(
                imageView,
                index,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                )
        );

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
        imageView.setImageDrawable(null);

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

                    String question = matches.get(0);

                    if (isImageRequest(question)) {
                        generateImage(question);
                    } else {
                        askAI(question);
                    }

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

    private boolean isImageRequest(String question) {

        String text = question.toLowerCase(Locale.getDefault());

        return text.contains("make an image")
                || text.contains("make a picture")
                || text.contains("generate an image")
                || text.contains("generate a picture")
                || text.contains("create an image")
                || text.contains("create a picture")
                || text.contains("draw me")
                || text.contains("draw a")
                || text.contains("show me a picture");
    }

    private void generateImage(String request) {

        status.setText("Creating your image...");

        new Thread(() -> {

            try {

                String apiKey = BuildConfig.OPENAI_API_KEY;

                if (apiKey == null || apiKey.isEmpty()) {

                    runOnUiThread(() ->
                            status.setText(
                                    "API key is not configured."
                            )
                    );

                    return;
                }

                URL url = new URL(
                        "https://api.openai.com/v1/images/generations"
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

                body.put(
                        "model",
                        "gpt-image-2"
                );

                body.put(
                        "prompt",
                        request
                );

                body.put(
                        "size",
                        "1024x1024"
                );

                body.put(
                        "quality",
                        "auto"
                );

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        body.toString().getBytes("UTF-8")
                );

                output.close();

                int responseCode =
                        connection.getResponseCode();

                BufferedReader reader;

                if (responseCode >= 200
                        && responseCode < 300) {

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

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode < 200
                        || responseCode >= 300) {

                    runOnUiThread(() ->
                            status.setText(
                                    "Image error: "
                                            + responseCode
                                            + "\n"
                                            + response
                            )
                    );

                    return;
                }

                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );

                JSONArray data =
                        json.getJSONArray("data");

                JSONObject first =
                        data.getJSONObject(0);

                String base64 =
                        first.getString("b64_json");

                byte[] imageBytes =
                        Base64.decode(
                                base64,
                                Base64.DEFAULT
                        );

                Bitmap bitmap =
                        BitmapFactory.decodeByteArray(
                                imageBytes,
                                0,
                                imageBytes.length
                        );

                runOnUiThread(() -> {

                    imageView.setImageBitmap(bitmap);

                    status.setText(
                            "Image created! 🎨"
                    );

                    speaker.speak(
                            "Your image is ready.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "curiosity-image"
                    );
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        status.setText(
                                "Image connection error: "
                                        + e.getMessage()
                        )
                );
            }

        }).start();
    }

    private void askAI(String question) {

        status.setText("Thinking...");

        new Thread(() -> {

            try {

                String apiKey =
                        BuildConfig.OPENAI_API_KEY;

                if (apiKey == null || apiKey.isEmpty()) {

                    runOnUiThread(() ->
                            status.setText(
                                    "API key is not configured."
                            )
                    );

                    return;
                }

                URL url = new URL(
                        "https://api.openai.com/v1/responses"
                );

                HttpURLConnection connection =
                        (HttpURLConnection)
                                url.openConnection();

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

                JSONObject body =
                        new JSONObject();

                body.put(
                        "model",
                        "gpt-5.6-luna"
                );

                body.put(
                        "input",
                        question
                );

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        body.toString().getBytes("UTF-8")
                );

                output.close();

                int responseCode =
                        connection.getResponseCode();

                BufferedReader reader;

                if (responseCode >= 200
                        && responseCode < 300) {

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

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode < 200
                        || responseCode >= 300) {

                    runOnUiThread(() ->
                            status.setText(
                                    "AI error: "
                                            + responseCode
                            )
                    );

                    return;
                }

                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );

                String answer =
                        extractText(json);

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
                                "Connection error: "
                                        + e.getMessage()
                        )
                );
            }
        }).start();
    }

    private String extractText(JSONObject json) {

        try {

            JSONArray output =
                    json.getJSONArray("output");

            for (int i = 0;
                 i < output.length();
                 i++) {

                JSONObject item =
                        output.getJSONObject(i);

                if ("message".equals(
                        item.optString("type"))) {

                    JSONArray content =
                            item.getJSONArray("content");

                    for (int j = 0;
                         j < content.length();
                         j++) {

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
