package com.example.speechtotext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;

import java.util.List;

public class OverlayView extends View {

    // Each detected hand is one List<NormalizedLandmark>
    private List<List<NormalizedLandmark>> allHands;

    // Actual camera frame dimensions from ImageProxy
    private int camW = 640;
    private int camH = 480;

    // Rotation of camera frame relative to screen (0, 90, 180, 270)
    private int rotationDegrees = 0;

    private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // MediaPipe 21-point hand skeleton connections
    private static final int[][] CONNECTIONS = {
            {0,1},{1,2},{2,3},{3,4},           // thumb
            {0,5},{5,6},{6,7},{7,8},           // index
            {0,9},{9,10},{10,11},{11,12},      // middle
            {0,13},{13,14},{14,15},{15,16},    // ring
            {0,17},{17,18},{18,19},{19,20},    // pinky
            {5,9},{9,13},{13,17}               // palm base
    };

    public OverlayView(@NonNull Context context) {
        super(context);
        init();
    }

    public OverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null); // GPU-accelerated, smooth 30+ fps

        dotPaint.setColor(Color.GREEN);
        dotPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setAlpha(220);
    }

    /**
     * @param hands          all detected hands (each is a list of 21 landmarks)
     * @param cameraWidth    ImageProxy.getWidth()  — actual frame width
     * @param cameraHeight   ImageProxy.getHeight() — actual frame height
     * @param rotDegrees     ImageProxy rotation degrees (0, 90, 180, 270)
     */
    public void setLandmarks(@NonNull List<List<NormalizedLandmark>> hands,
                             int cameraWidth, int cameraHeight, int rotDegrees) {
        this.allHands       = hands;
        this.camW           = cameraWidth;
        this.camH           = cameraHeight;
        this.rotationDegrees = rotDegrees;
        invalidate(); // zero allocation — just triggers onDraw()
    }

    public void clear() {
        this.allHands = null;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (allHands == null || allHands.isEmpty()) return;

        int viewW = getWidth();
        int viewH = getHeight();
        if (viewW == 0 || viewH == 0) return;

        // ── After rotation, effective frame dimensions on screen ──────────────
        // If 90° or 270°, the sensor width/height are swapped relative to screen
        int effW = (rotationDegrees == 90 || rotationDegrees == 270) ? camH : camW;
        int effH = (rotationDegrees == 90 || rotationDegrees == 270) ? camW : camH;

        // ── Match PreviewView FILL_CENTER scaling ─────────────────────────────
        float scaleX  = (float) viewW / effW;
        float scaleY  = (float) viewH / effH;
        float scale   = Math.max(scaleX, scaleY);
        float offsetX = (viewW - effW * scale) / 2f;
        float offsetY = (viewH - effH * scale) / 2f;

        for (List<NormalizedLandmark> landmarks : allHands) {
            // Draw skeleton lines
            for (int[] c : CONNECTIONS) {
                float[] p1 = toScreenXY(landmarks.get(c[0]).x(), landmarks.get(c[0]).y(),
                        effW, effH, scale, offsetX, offsetY);
                float[] p2 = toScreenXY(landmarks.get(c[1]).x(), landmarks.get(c[1]).y(),
                        effW, effH, scale, offsetX, offsetY);
                canvas.drawLine(p1[0], p1[1], p2[0], p2[1], linePaint);
            }

            // Draw landmark dots
            for (NormalizedLandmark point : landmarks) {
                float[] p = toScreenXY(point.x(), point.y(),
                        effW, effH, scale, offsetX, offsetY);
                canvas.drawCircle(p[0], p[1], 14f, dotPaint);
            }
        }
    }

    /**
     * Converts a normalized landmark (nx, ny) from MediaPipe's frame space
     * into screen pixel coordinates, accounting for camera rotation and
     * PreviewView scaling.
     *
     * Rotation mapping (MediaPipe landmark → screen-oriented coordinate):
     *   0°:   nx, ny              (no change)
     *   90°:  ny, 1-nx            (rotate CCW 90°)
     *   180°: 1-nx, 1-ny          (flip both)
     *   270°: 1-ny, nx            (rotate CW 90°)
     *
     * The front camera is already mirrored (we mirror the bitmap before
     * detection), so no extra horizontal flip is needed here.
     */
    private float[] toScreenXY(float nx, float ny,
                               int effW, int effH,
                               float scale, float offsetX, float offsetY) {
        float rx, ry;
        switch (rotationDegrees) {
            case 90:
                rx = ny;
                ry = 1f - nx;
                break;
            case 180:
                rx = 1f - nx;
                ry = 1f - ny;
                break;
            case 270:
                rx = 1f - ny;
                ry = nx;
                break;
            default: // 0°
                rx = nx;
                ry = ny;
                break;
        }
        float px = rx * effW * scale + offsetX;
        float py = ry * effH * scale + offsetY;
        return new float[]{px, py};
    }
}
