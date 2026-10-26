package com.standoffvision.lab.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

public class NeverloseMenu {
    private Paint bgPaint;
    private Paint sidebarPaint;
    private Paint accentPaint;
    private Paint textPaint;
    private Paint subTextPaint;
    private Paint borderPaint;
    private Paint sliderBgPaint;
    private Paint sliderFillPaint;

    private RectF menuBounds = new RectF(80, 80, 920, 720);
    private RectF sidebarBounds = new RectF(80, 80, 280, 720);

    public int activeTab = 0; // 0: ESP, 1: AIMBOT, 2: SETTINGS

    // Toggles - ESP
    public boolean enableBox = true;
    public boolean enableHealth = true;
    public boolean enableName = true;
    public boolean enableSkeletons = true;
    public boolean enableSnaplines = true;

    // Toggles - AIMBOT
    public boolean enableAim = true;
    public boolean enableFovCircle = true;
    public float aimFov = 250f;
    public float aimSmooth = 5f;

    // UI Interactive Rects
    private RectF tab1Rect = new RectF(90, 160, 270, 210);
    private RectF tab2Rect = new RectF(90, 230, 270, 280);
    private RectF tab3Rect = new RectF(90, 300, 270, 350);

    // ESP Controls
    private RectF toggleBoxRect = new RectF(320, 160, 380, 200);
    private RectF toggleHealthRect = new RectF(320, 230, 380, 270);
    private RectF toggleNameRect = new RectF(320, 300, 380, 340);
    private RectF toggleSkeletonRect = new RectF(320, 370, 380, 410);
    private RectF toggleSnaplinesRect = new RectF(320, 440, 380, 480);

    // AIM Controls
    private RectF toggleAimRect = new RectF(320, 160, 380, 200);
    private RectF toggleFovRect = new RectF(320, 230, 380, 270);
    private RectF fovSliderBg = new RectF(320, 330, 720, 350);

    public NeverloseMenu() {
        initPaints();
    }

    private void initPaints() {
        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#0B0C10"));
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);

        sidebarPaint = new Paint();
        sidebarPaint.setColor(Color.parseColor("#1F2833"));
        sidebarPaint.setStyle(Paint.Style.FILL);
        sidebarPaint.setAntiAlias(true);

        accentPaint = new Paint();
        accentPaint.setColor(Color.parseColor("#00A8FF"));
        accentPaint.setStyle(Paint.Style.FILL);
        accentPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#00A8FF"));
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);

        subTextPaint = new Paint();
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setTextSize(26f);
        subTextPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#00A8FF"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setAntiAlias(true);

        sliderBgPaint = new Paint();
        sliderBgPaint.setColor(Color.parseColor("#2C3531"));
        sliderBgPaint.setStyle(Paint.Style.FILL);

        sliderFillPaint = new Paint();
        sliderFillPaint.setColor(Color.parseColor("#00A8FF"));
        sliderFillPaint.setStyle(Paint.Style.FILL);
    }

    public void draw(Canvas canvas) {
        canvas.drawRoundRect(menuBounds, 20f, 20f, bgPaint);
        canvas.drawRoundRect(menuBounds, 20f, 20f, borderPaint);
        canvas.drawRoundRect(sidebarBounds, 20f, 20f, sidebarPaint);

        canvas.drawText("NEVERLOSE", 100, 130, textPaint);

        canvas.drawText("VISUALS", 110, 195, activeTab == 0 ? textPaint : subTextPaint);
        canvas.drawText("AIMBOT", 110, 265, activeTab == 1 ? textPaint : subTextPaint);
        canvas.drawText("CONFIGS", 110, 335, activeTab == 2 ? textPaint : subTextPaint);

        if (activeTab == 0) {
            drawToggle(canvas, toggleBoxRect, enableBox, "Box ESP (2D)", 225);
            drawToggle(canvas, toggleHealthRect, enableHealth, "Health Bar", 295);
            drawToggle(canvas, toggleNameRect, enableName, "Distance & Info", 365);
            drawToggle(canvas, toggleSkeletonRect, enableSkeletons, "Skeleton ESP", 435);
            drawToggle(canvas, toggleSnaplinesRect, enableSnaplines, "Snaplines (Tracers)", 505);
        } else if (activeTab == 1) {
            drawToggle(canvas, toggleAimRect, enableAim, "Enable Aimbot Assist", 225);
            drawToggle(canvas, toggleFovRect, enableFovCircle, "Draw FOV Circle", 295);

            canvas.drawText("FOV Radius: " + (int) aimFov, 320, 320, subTextPaint);
            canvas.drawRoundRect(fovSliderBg, 10f, 10f, sliderBgPaint);
            float fillWidth = (aimFov / 500f) * (fovSliderBg.width());
            RectF sliderFill = new RectF(fovSliderBg.left, fovSliderBg.top, fovSliderBg.left + fillWidth, fovSliderBg.bottom);
            canvas.drawRoundRect(sliderFill, 10f, 10f, sliderFillPaint);
        } else if (activeTab == 2) {
            canvas.drawText("Config System Active [DEFAULT.CFG]", 320, 225, subTextPaint);
            canvas.drawText("Status: Undetected / Safe Overlay", 320, 280, textPaint);
        }
    }

    private void drawToggle(Canvas canvas, RectF rect, boolean active, String label, float textY) {
        canvas.drawRoundRect(rect, 8f, 8f, active ? accentPaint : sidebarPaint);
        canvas.drawText(label, rect.right + 20, textY, subTextPaint);
    }

    public boolean handleTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (tab1Rect.contains(x, y)) { activeTab = 0; return true; }
                if (tab2Rect.contains(x, y)) { activeTab = 1; return true; }
                if (tab3Rect.contains(x, y)) { activeTab = 2; return true; }

                if (activeTab == 0) {
                    if (toggleBoxRect.contains(x, y)) { enableBox = !enableBox; return true; }
                    if (toggleHealthRect.contains(x, y)) { enableHealth = !enableHealth; return true; }
                    if (toggleNameRect.contains(x, y)) { enableName = !enableName; return true; }
                    if (toggleSkeletonRect.contains(x, y)) { enableSkeletons = !enableSkeletons; return true; }
                    if (toggleSnaplinesRect.contains(x, y)) { enableSnaplines = !enableSnaplines; return true; }
                } else if (activeTab == 1) {
                    if (toggleAimRect.contains(x, y)) { enableAim = !enableAim; return true; }
                    if (toggleFovRect.contains(x, y)) { enableFovCircle = !enableFovCircle; return true; }
                }
            }

            if (activeTab == 1 && fovSliderBg.contains(x, y)) {
                float relativeX = x - fovSliderBg.left;
                float progress = relativeX / fovSliderBg.width();
                aimFov = Math.max(50f, Math.min(500f, progress * 500f));
                return true;
            }
        }
        return false;
    }
}
