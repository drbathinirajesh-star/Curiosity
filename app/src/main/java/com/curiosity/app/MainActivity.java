package com.curiosity.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView status;
    private Button listen;
    private SpeechRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        listen = findViewById(R.id.listen);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    10
            );
        }

        listen.setOnClickListener(v -> listen());

        status.setText("Tap LISTEN and say \"Hello Curiosity\"");
    }

    private void listen() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition is not available.");
            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
        }

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
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                status.setText("Processing...");
            }

            @Override
            public void onError(int error) {
                status.setText("I couldn't hear that. Try again.");
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null && !matches.isEmpty()) {

                    String text = matches.get(0);

                    status.setText("You said: " + text);

                    if (text.toLowerCase().contains("hello curiosity")) {
                        status.setText("Hello! I'm Curiosity.");
                    }
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
                RecognizerIntent.EXTRA_PROMPT,
                "Say something..."
        );

        recognizer.startListening(intent);
    }

    @Override
    protected void onDestroy() {

        if (recognizer != null) {
            recognizer.destroy();
        }

        super.onDestroy();
    }
}
