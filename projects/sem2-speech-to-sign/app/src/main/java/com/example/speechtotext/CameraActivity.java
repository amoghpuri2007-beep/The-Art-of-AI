package com.example.speechtotext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int CAMERA_PERMISSION_CODE = 1;

    private PreviewView previewView;
    private Button btnStart;
    private TextView txtDetected;
    private OverlayView overlayView;

    private TextToSpeech tts;
    private boolean isRunning = false;
    private HandLandmarker handLandmarker;
    private String lastSpokenGesture = "";

    // ── TFLite ────────────────────────────────────────────────────────────────
    private Interpreter tfliteInterpreter;
    private List<String> labels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        btnStart    = findViewById(R.id.btnStart);
        txtDetected = findViewById(R.id.txtDetected);
        overlayView = findViewById(R.id.overlayView);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.ENGLISH);
        });

        setupHandLandmarker();
        setupTFLite();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        btnStart.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                return;
            }
            if (!isRunning) {
                startCamera();
                isRunning = true;
                btnStart.setText(R.string.camera_started);
                txtDetected.setText(R.string.show_hand);
                tts.speak(getString(R.string.camera_started), TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    // ── TFLite setup ──────────────────────────────────────────────────────────

    private void setupTFLite() {
        try {
            // Load model
            MappedByteBuffer modelBuffer = loadModelFile("isl_model.tflite");
            tfliteInterpreter = new Interpreter(modelBuffer);

            // Load labels
            InputStream is = getAssets().open("isl_labels.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) labels.add(line.trim());
            }
            reader.close();
            Log.d(TAG, "TFLite loaded. Labels: " + labels.size());
        } catch (IOException e) {
            Log.e(TAG, "Failed to load TFLite model or labels", e);
        }
    }

    private MappedByteBuffer loadModelFile(String filename) throws IOException {
        android.content.res.AssetFileDescriptor fileDescriptor =
                getAssets().openFd(filename);
        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // ── MediaPipe setup ───────────────────────────────────────────────────────

    private void setupHandLandmarker() {
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build();

        HandLandmarker.HandLandmarkerOptions options =
                HandLandmarker.HandLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setRunningMode(RunningMode.IMAGE)
                        .setNumHands(2)
                        .setMinHandDetectionConfidence(0.5f)
                        .setMinHandPresenceConfidence(0.5f)
                        .setMinTrackingConfidence(0.5f)
                        .build();

        handLandmarker = HandLandmarker.createFromOptions(this, options);
    }

    // ── Camera ────────────────────────────────────────────────────────────────

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        this::analyzeFrame);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera provider failed to start", e);
                Thread.currentThread().interrupt();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ── Frame analysis ────────────────────────────────────────────────────────

    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (handLandmarker == null) {
            imageProxy.close();
            return;
        }

        int frameW = imageProxy.getWidth();
        int frameH = imageProxy.getHeight();
        int rotDeg = imageProxy.getImageInfo().getRotationDegrees();

        Bitmap bitmap = imageProxy.toBitmap();

        Matrix matrix = new Matrix();
        matrix.preScale(-1f, 1f, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
        Bitmap mirrored = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

        MPImage mpImage = new BitmapImageBuilder(mirrored).build();
        HandLandmarkerResult result = handLandmarker.detect(mpImage);

        imageProxy.close();

        if (result == null || result.landmarks().isEmpty()) {
            runOnUiThread(() -> {
                txtDetected.setText(R.string.no_hand_detected);
                overlayView.clear();
            });
            return;
        }

        List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>
                allHands = result.landmarks();

        StringBuilder gestureBuilder = new StringBuilder();
        for (int i = 0; i < allHands.size(); i++) {
            if (i > 0) gestureBuilder.append(" | ");
            gestureBuilder.append(classifyGesture(allHands.get(i)));
        }
        String gesture = gestureBuilder.toString();

        runOnUiThread(() -> {
            overlayView.setLandmarks(allHands, frameW, frameH, rotDeg);
            txtDetected.setText(getString(R.string.gesture_prefix, gesture));
            if (!gesture.equals(lastSpokenGesture)) {
                lastSpokenGesture = gesture;
                tts.speak(gesture, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    // ── TFLite Gesture Classifier ─────────────────────────────────────────────

    private String classifyGesture(
            List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> lm) {

        if (tfliteInterpreter == null || labels.isEmpty()) {
            return "Model not loaded";
        }

        // Build input: 21 landmarks x 3 (x, y, z) = 63 floats
        float[] raw = new float[63];
        for (int i = 0; i < 21; i++) {
            raw[i * 3]     = lm.get(i).x();
            raw[i * 3 + 1] = lm.get(i).y();
            raw[i * 3 + 2] = lm.get(i).z();
        }

        // Normalize: subtract wrist (landmark 0), scale by max abs value
        float wx = raw[0], wy = raw[1], wz = raw[2];
        for (int i = 0; i < 21; i++) {
            raw[i * 3]     -= wx;
            raw[i * 3 + 1] -= wy;
            raw[i * 3 + 2] -= wz;
        }
        float maxVal = 1e-6f;
        for (float v : raw) maxVal = Math.max(maxVal, Math.abs(v));
        for (int i = 0; i < 63; i++) raw[i] /= maxVal;

        // Run inference
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(63 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        for (float v : raw) inputBuffer.putFloat(v);

        float[][] output = new float[1][labels.size()];
        tfliteInterpreter.run(inputBuffer, output);

        // Find highest confidence label
        int bestIdx = 0;
        float bestScore = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > bestScore) {
                bestScore = output[0][i];
                bestIdx = i;
            }
        }

        // Only return label if confidence is high enough
        if (bestScore > 0.7f) {
            return labels.get(bestIdx);
        } else {
            return "?";
        }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            isRunning = true;
            btnStart.setText(R.string.camera_started);
            txtDetected.setText(R.string.show_hand);
        } else {
            txtDetected.setText(R.string.camera_permission_required);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
        if (handLandmarker != null) handLandmarker.close();
        if (tfliteInterpreter != null) tfliteInterpreter.close();
    }
}
