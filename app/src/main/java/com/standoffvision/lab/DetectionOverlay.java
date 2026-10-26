package com.standoffvision.lab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import com.standoffvision.lab.ui.NeverloseMenu;
import com.standoffvision.lab.overlay.EspRenderer;
import com.standoffvision.lab.overlay.AimAssist;

public class DetectionOverlay extends View {
    private NeverloseMenu menu;
    private EspRenderer espRenderer;

    private RectF[] dummyTargets;

    public DetectionOverlay(Context context) {
        super(context);
        menu = new NeverloseMenu();
        espRenderer = new EspRenderer();

        dummyTargets = new RectF[] {
            new RectF(800, 250, 980, 750),
            new RectF(1200, 300, 1360, 700)
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int screenW = getWidth();
        int screenH = getHeight();

        // Защита от деления на ноль при старте оверлея
        if (screenW <= 0 || screenH <= 0) return;

        espRenderer.render(
            canvas,
            dummyTargets,
            menu.enableBox,
            menu.enableHealth,
            menu.enableName,
            menu.enableSkeletons,
            menu.enableSnaplines,
            menu.enableFovCircle,
            menu.aimFov,
            screenW,
            screenH
        );

        if (menu.enableAim) {
            float[] aimCoords = AimAssist.calculateSmoothAim(dummyTargets, menu.aimFov, menu.aimSmooth, screenW, screenH);
            if (aimCoords != null) {
                // Плавное ведение цели
            }
        }

        menu.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (menu.handleTouch(event)) {
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
