package com.standoffvision.lab;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class DetectionOverlay extends View {
    private static final String PREFS_NAME = "vision_settings";

    public static final class Item {
        public final RectF bounds;
        public final String label;
        public final float score;

        public Item(RectF bounds, String label, float score) {
            this.bounds = new RectF(bounds);
            this.label = label == null ? "object" : label;
            this.score = score;
        }
    }

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private volatile List<Item> items = Collections.emptyList();
    private volatile int frameWidth = 1;
    private volatile int frameHeight = 1;
    private volatile String status = "AI: инициализация модели";

    public DetectionOverlay(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(4f, 1f, 1f, Color.BLACK);
        panelPaint.setColor(Color.argb(178, 8, 13, 20));
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(2.5f);
    }

    public void update(List<Item> newItems, int width, int height, float fps,
                       long inferenceMs, String note) {
        items = Collections.unmodifiableList(new ArrayList<>(newItems));
        frameWidth = Math.max(1, width);
        frameHeight = Math.max(1, height);

        SharedPreferences preferences = getContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int visibleCount = 0;
        int persons = 0;
        float minScore = preferences.getInt("min_confidence_percent", 35) / 100f;
        boolean personsOnly = preferences.getBoolean("persons_only", false);
        for (Item item : newItems) {
            if (!isVisible(item, minScore, personsOnly)) continue;
            visibleCount++;
            if ("person".equalsIgnoreCase(item.label)) persons++;
        }

        String targetState = persons > 0 ? "TARGET: " + persons : "TARGET: —";
        status = String.format(Locale.US, "AI %d obj  |  %.1f FPS  |  %d ms  |  %s",
                visibleCount, fps, inferenceMs, targetState);
        if (note != null && !note.isEmpty()) status += "  |  " + note;
        postInvalidate();
    }

    public void setStatus(String value) {
        status = value == null ? "AI: ожидание" : value;
        postInvalidate();
    }

    private boolean isVisible(Item item, float minScore, boolean personsOnly) {
        if (item.score < minScore) return false;
        return !personsOnly || "person".equalsIgnoreCase(item.label);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float sx = getWidth() / (float) frameWidth;
        final float sy = getHeight() / (float) frameHeight;
        final List<Item> snapshot = items;
        SharedPreferences preferences = getContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean drawBoxes = preferences.getBoolean("draw_boxes", true);
        boolean drawLabels = preferences.getBoolean("draw_labels", true);
        boolean drawGuide = preferences.getBoolean("draw_guide", true);
        boolean personsOnly = preferences.getBoolean("persons_only", false);
        float minScore = preferences.getInt("min_confidence_percent", 35) / 100f;

        List<Item> visibleItems = new ArrayList<>();
        for (Item item : snapshot) {
            if (isVisible(item, minScore, personsOnly)) visibleItems.add(item);
        }

        Item selectedPerson = null;
        float bestDistance = Float.MAX_VALUE;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float fovRadiusDp = preferences.getInt("fov_radius_dp", 180);
        float fovRadiusPx = Math.max(80f, Math.min(320f, fovRadiusDp))
                * getResources().getDisplayMetrics().density;
        float fovRadiusSquared = fovRadiusPx * fovRadiusPx;

        for (Item item : visibleItems) {
            if (!"person".equalsIgnoreCase(item.label)) continue;
            float tx = (item.bounds.left + item.bounds.right) * 0.5f * sx;
            float ty = (item.bounds.top + item.bounds.bottom) * 0.5f * sy;
            float dx = tx - centerX;
            float dy = ty - centerY;
            float distance = dx * dx + dy * dy;
            // The FOV is a visual guide only; it never moves the in-game crosshair.
            if (distance <= fovRadiusSquared && distance < bestDistance) {
                bestDistance = distance;
                selectedPerson = item;
            }
        }

        // Visual guidance only; this overlay never changes the game's input.
        if (drawGuide) {
            guidePaint.setColor(Color.argb(170, 99, 220, 190));
            canvas.drawCircle(centerX, centerY, fovRadiusPx, guidePaint);
            canvas.drawCircle(centerX, centerY, 28f, guidePaint);
            canvas.drawLine(centerX - 40f, centerY, centerX - 14f, centerY, guidePaint);
            canvas.drawLine(centerX + 14f, centerY, centerX + 40f, centerY, guidePaint);
            canvas.drawLine(centerX, centerY - 40f, centerX, centerY - 14f, guidePaint);
            canvas.drawLine(centerX, centerY + 14f, centerX, centerY + 40f, guidePaint);

            if (selectedPerson != null) {
                float tx = (selectedPerson.bounds.left + selectedPerson.bounds.right) * 0.5f * sx;
                float ty = (selectedPerson.bounds.top + selectedPerson.bounds.bottom) * 0.5f * sy;
                guidePaint.setColor(Color.argb(200, 70, 255, 128));
                canvas.drawLine(centerX, centerY, tx, ty, guidePaint);
                canvas.drawCircle(tx, ty, 12f, guidePaint);
            }
        }

        for (Item item : visibleItems) {
            boolean person = "person".equalsIgnoreCase(item.label);
            boolean selected = person && item == selectedPerson;
            float left = item.bounds.left * sx;
            float top = item.bounds.top * sy;
            float right = item.bounds.right * sx;
            float bottom = item.bounds.bottom * sy;

            if (drawBoxes) {
                boxPaint.setColor(selected ? Color.rgb(70, 255, 128)
                        : person ? Color.rgb(99, 220, 190) : Color.rgb(255, 178, 90));
                boxPaint.setStrokeWidth(selected ? 5f : 3f);
                canvas.drawRoundRect(new RectF(left, top, right, bottom), 8f, 8f, boxPaint);
            }

            if (drawLabels) {
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(28f);
                String caption = String.format(Locale.US, "%s %.0f%%",
                        item.label, item.score * 100f);
                float textY = Math.max(38f, top - 8f);
                canvas.drawText(caption, left, textY, textPaint);
            }
        }

        canvas.drawRect(0, 0, getWidth(), 64f, panelPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        canvas.drawText(status, 16f, 39f, textPaint);
    }
}
