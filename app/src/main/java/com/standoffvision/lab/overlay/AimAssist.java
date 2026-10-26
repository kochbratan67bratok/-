package com.standoffvision.lab.overlay;

import android.graphics.RectF;

public class AimAssist {

    public static float[] calculateSmoothAim(RectF[] boxes, float currentFov, float smoothSpeed, int screenWidth, int screenHeight) {
        if (boxes == null || boxes.length == 0 || screenWidth <= 0 || screenHeight <= 0) return null;

        float centerX = screenWidth / 2.0f;
        float centerY = screenHeight / 2.0f;

        RectF bestTarget = null;
        float minDistance = currentFov;

        for (RectF box : boxes) {
            if (box == null) continue;

            float boxCenterX = box.centerX();
            float boxCenterY = box.top + (box.height() * 0.2f); // Область головы/шеи

            float distX = boxCenterX - centerX;
            float distY = boxCenterY - centerY;
            float distance = (float) Math.sqrt(distX * distX + distY * distY);

            if (distance < minDistance) {
                minDistance = distance;
                bestTarget = box;
            }
        }

        if (bestTarget != null && smoothSpeed > 0f) {
            float targetX = bestTarget.centerX();
            float targetY = bestTarget.top + (bestTarget.height() * 0.2f);

            float deltaX = (targetX - centerX) / smoothSpeed;
            float deltaY = (targetY - centerY) / smoothSpeed;

            return new float[] { centerX + deltaX, centerY + deltaY };
        }

        return null;
    }
}
