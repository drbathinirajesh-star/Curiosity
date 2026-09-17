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
                status.setText("I didn't catch that. Try again.");
            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {

                    String question = matches.get(0);

                    status.setText(question);

                    String answer = getCuriosityAnswer(question);

                    speak(answer);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
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

    private String getCuriosityAnswer(String question) {

        String text = question.toLowerCase();

        if (text.contains("hello") ||
                text.contains("hi") ||
                text.contains("hey")) {

            return "Hello! I'm Curiosity. How can I help you?";
        }

        if (text.contains("how are you")) {

            return "I'm doing great! Thanks for asking.";
        }

        if (text.contains("your name")) {

            return "My name is Curiosity.";
        }

        if (text.contains("who are you")) {

            return "I'm Curiosity, your voice assistant.";
        }

        if (text.contains("thank")) {

            return "You're welcome!";
        }

        if (text.contains("bye")) {

            return "Goodbye! Talk to you later.";
        }

        return "I heard you say: " + question;
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
