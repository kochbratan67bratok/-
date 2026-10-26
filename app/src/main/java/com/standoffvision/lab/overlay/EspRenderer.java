package com.standoffvision.lab.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public class EspRenderer {
    private Paint boxPaint;
    private Paint healthBgPaint;
    private Paint healthBarPaint;
    private Paint textPaint;
    private Paint fovPaint;
    private Paint linePaint;
    private Paint skeletonPaint;

    public EspRenderer() {
        boxPaint = new Paint();
        boxPaint.setColor(Color.parseColor("#00A8FF"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(3f);
        boxPaint.setAntiAlias(true);

        healthBgPaint = new Paint();
        healthBgPaint.setColor(Color.parseColor("#88000000"));
        healthBgPaint.setStyle(Paint.Style.FILL);

        healthBarPaint = new Paint();
        healthBarPaint.setColor(Color.parseColor("#00FF66"));
        healthBarPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);

        fovPaint = new Paint();
        fovPaint.setColor(Color.parseColor("#4400A8FF"));
        fovPaint.setStyle(Paint.Style.STROKE);
        fovPaint.setStrokeWidth(2f);
        fovPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#8800A8FF"));
        linePaint.setStrokeWidth(2f);
        linePaint.setAntiAlias(true);

        skeletonPaint = new Paint();
        skeletonPaint.setColor(Color.parseColor("#FFCC00"));
        skeletonPaint.setStrokeWidth(3f);
        skeletonPaint.setAntiAlias(true);
    }

    public void render(Canvas canvas, RectF[] targets, boolean showBox, boolean showHealth, boolean showName, boolean showSkeletons, boolean showSnaplines, boolean showFov, float fovRadius, int screenW, int screenH) {
        if (showFov && screenW > 0 && screenH > 0) {
            canvas.drawCircle(screenW / 2.0f, screenH / 2.0f, fovRadius, fovPaint);
        }

        if (targets == null) return;

        float screenCenterX = screenW / 2.0f;
        float screenBottomY = screenH;

        for (RectF target : targets) {
            if (target == null) continue;

            if (showSnaplines) {
                canvas.drawLine(screenCenterX, screenBottomY, target.centerX(), target.bottom, linePaint);
            }

            if (showBox) {
                canvas.drawRect(target, boxPaint);
            }

            if (showHealth) {
                float barLeft = target.left - 12;
                float barRight = target.left - 4;
                float barTop = target.top;
                float barBottom = target.bottom;

                canvas.drawRect(barLeft, barTop, barRight, barBottom, healthBgPaint);
                canvas.drawRect(barLeft, barTop + (barBottom - barTop) * 0.25f, barRight, barBottom, healthBarPaint);
            }

            if (showName) {
                canvas.drawText("Enemy_Player [12m]", target.left, target.top - 12, textPaint);
            }

            if (showSkeletons) {
                float headX = target.centerX();
                float headY = target.top + (target.height() * 0.15f);
                float neckY = target.top + (target.height() * 0.25f);
                float pelvisY = target.top + (target.height() * 0.65f);

                canvas.drawCircle(headX, headY, 12f, skeletonPaint);
                canvas.drawLine(headX, headY, headX, pelvisY, skeletonPaint);
                canvas.drawLine(headX - (target.width() * 0.3f), neckY, headX + (target.width() * 0.3f), neckY, skeletonPaint);
                canvas.drawLine(headX, pelvisY, target.left + (target.width() * 0.2f), target.bottom, skeletonPaint);
                canvas.drawLine(headX, pelvisY, target.right - (target.width() * 0.2f), target.bottom, skeletonPaint);
            }
        }
    }
}
