package com.example.speechtotext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class Main2Activity extends AppCompatActivity {

    private TextView txtResult;
    private ImageView imageView;
    private Button btnSpeak;

    private SpeechRecognizer speechRecognizer;
    private Intent intent;

    private HashMap<String, Integer> wordMap = new HashMap<>();
    private HashMap<String, Integer> letterMap = new HashMap<>();
    private HashMap<String, Integer> numberMap = new HashMap<>();
    private HashMap<String, Integer> gifDuration = new HashMap<>();

    private int noInputImage;

    private Handler handler = new Handler();
    private int letterDelay = 700;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResult = findViewById(R.id.txtResult);
        imageView = findViewById(R.id.imageView);
        btnSpeak = findViewById(R.id.btnSpeak);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        initMaps();
        Glide.with(this).load(noInputImage).into(imageView);
        setupSpeechRecognizer();

        btnSpeak.setOnClickListener(v -> startListening());

        // ✅ Add this
        Button btnCamera = findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(Main2Activity.this, CameraActivity.class);
            startActivity(cameraIntent);
        });
    }

    private void setupSpeechRecognizer() {

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                txtResult.setText("Listening...");
            }

            @Override public void onBeginningOfSpeech() {}

            @Override public void onRmsChanged(float rmsdB) {}

            @Override public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                speechRecognizer.stopListening();
            }

            @Override
            public void onError(int error) {
                txtResult.setText("Try again...");
                btnSpeak.setEnabled(true);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (data != null && data.size() > 0) {
                    String speech = data.get(0).toLowerCase();
                    txtResult.setText("");
                    processSpeech(speech);
                } else {
                    btnSpeak.setEnabled(true);
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void startListening() {
        btnSpeak.setEnabled(false);
        txtResult.setText("Listening...");
        speechRecognizer.startListening(intent);
    }

    private void initMaps() {

        // WORD GIFS
        wordMap.put("hello", R.raw.hello);
        wordMap.put("good", R.raw.good);
        wordMap.put("you", R.raw.you);
        wordMap.put("morning", R.raw.morning);

        gifDuration.put("hello", 3000);
        gifDuration.put("good", 3000);
        gifDuration.put("you", 3000);
        gifDuration.put("morning", 3000);

        // LETTERS
        String letters = "abcdefghijklmnopqrstuvwxyz";
        for (char c : letters.toCharArray()) {
            int resId = getResources().getIdentifier(
                    String.valueOf(c),
                    "drawable",
                    getPackageName()
            );
            letterMap.put(String.valueOf(c), resId);
        }

        // NUMBERS
        numberMap.put("0", R.drawable.zero);
        numberMap.put("1", R.drawable.one);
        numberMap.put("2", R.drawable.two);
        numberMap.put("3", R.drawable.three);
        numberMap.put("4", R.drawable.four);
        numberMap.put("5", R.drawable.five);
        numberMap.put("6", R.drawable.six);
        numberMap.put("7", R.drawable.seven);
        numberMap.put("8", R.drawable.eight);
        numberMap.put("9", R.drawable.nine);

        numberMap.put("zero", R.drawable.zero);
        numberMap.put("one", R.drawable.one);
        numberMap.put("two", R.drawable.two);
        numberMap.put("three", R.drawable.three);

        noInputImage = R.drawable.space;
    }

    private void processSpeech(String speech) {

        String[] words = speech.split("\\s+");
        int totalDelay = 0;

        for (String word : words) {

            if (wordMap.containsKey(word)) {

                int gifRes = wordMap.get(word);
                int gifTime = gifDuration.get(word);

                handler.postDelayed(() -> {
                    txtResult.append(word);
                    Glide.with(this).asGif().load(gifRes).into(imageView);
                }, totalDelay);

                totalDelay += gifTime;
            }

            else if (numberMap.containsKey(word)) {

                int numRes = numberMap.get(word);

                handler.postDelayed(() -> {
                    txtResult.append(word);
                    Glide.with(this).load(numRes).into(imageView);
                }, totalDelay);

                totalDelay += letterDelay;
            }

            else {

                for (int i = 0; i < word.length(); i++) {

                    String letter = String.valueOf(word.charAt(i));
                    int resId = letterMap.getOrDefault(letter, noInputImage);

                    handler.postDelayed(() -> {
                        txtResult.append(letter);
                        Glide.with(this).load(resId).into(imageView);
                    }, totalDelay);

                    totalDelay += letterDelay;
                }
            }

            handler.postDelayed(() -> txtResult.append(" "), totalDelay);
            totalDelay += 200;
        }

        // Reset UI after completion
        handler.postDelayed(() -> {
            Glide.with(this).load(noInputImage).into(imageView);
            btnSpeak.setEnabled(true);
        }, totalDelay);
    }
}
