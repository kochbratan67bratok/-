package com.standoffvision.lab;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 1001;
    private static final int REQUEST_CAPTURE = 1002;

    private MediaProjectionManager projectionManager;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(11, 15, 23));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(26));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = new TextView(this);
        eyebrow.setText("VISION / LAB  •  ANDROID 10–15");
        eyebrow.setTextColor(Color.rgb(99, 220, 190));
        eyebrow.setTextSize(12);
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText("Standoff Vision Lab");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, params(0, 0, 0, 8));

        TextView description = new TextView(this);
        description.setText("Первый прототип экранного компьютерного зрения: захват изображения, локальная модель EfficientDet-Lite0 и прозрачные рамки поверх экрана.");
        description.setTextColor(Color.rgb(190, 199, 213));
        description.setTextSize(15);
        description.setLineSpacing(dp(3), 1.0f);
        root.addView(description, params(0, 0, 0, 20));

        statusView = new TextView(this);
        statusView.setText("Статус: готов к запуску");
        statusView.setTextColor(Color.rgb(99, 220, 190));
        statusView.setTextSize(14);
        statusView.setPadding(dp(14), dp(14), dp(14), dp(14));
        statusView.setBackground(panelBackground());
        root.addView(statusView, params(0, 0, 0, 18));

        Button startButton = makeButton("ЗАПУСТИТЬ ЗАХВАТ", true);
        root.addView(startButton, params(0, 0, 0, 10));
        startButton.setOnClickListener(v -> beginSetup());

        Button stopButton = makeButton("ОСТАНОВИТЬ", false);
        root.addView(stopButton, params(0, 0, 0, 22));
        stopButton.setOnClickListener(v -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            statusView.setText("Статус: команда остановки отправлена");
            Toast.makeText(this, "Захват остановлен", Toast.LENGTH_SHORT).show();
        });

        TextView heading = new TextView(this);
        heading.setText("ЧТО ТЕСТИРУЕМ");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(13);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(heading, params(0, 0, 0, 8));

        TextView details = new TextView(this);
        details.setText(
                "• MediaProjection: системное разрешение на захват экрана\n" +
                "• TensorFlow Lite: распознавание объектов локально на устройстве\n" +
                "• Overlay: рамки, центральный ориентир и подсветка ближайшего объекта класса person\n\n" +
                "Модель обучена на COCO, а не на Standoff 2. На игровых персонажах она может не находить цели — позже потребуется отдельная модель на размеченных игровых кадрах. Прототип не внедряется в процесс игры и не отправляет игровые касания автоматически.");
        details.setTextColor(Color.rgb(165, 176, 193));
        details.setTextSize(13);
        details.setLineSpacing(dp(4), 1.0f);
        details.setPadding(dp(14), dp(14), dp(14), dp(14));
        details.setBackground(panelBackground());
        root.addView(details, params(0, 0, 0, 0));

        setContentView(scroll);
    }

    private void beginSetup() {
        if (!Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(overlayIntent, REQUEST_OVERLAY);
            return;
        }
        requestScreenCapture();
    }

    private void requestScreenCapture() {
        if (projectionManager == null) {
            statusView.setText("Ошибка: MediaProjection недоступен на этом устройстве");
            return;
        }
        statusView.setText("Ожидается разрешение Android на захват экрана…");
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                requestScreenCapture();
            } else {
                statusView.setText("Разрешение на overlay не выдано");
            }
            return;
        }
        if (requestCode == REQUEST_CAPTURE) {
            if (resultCode != RESULT_OK || data == null) {
                statusView.setText("Захват отменён: разрешение не выдано");
                return;
            }
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            serviceIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode);
            serviceIntent.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            statusView.setText("Статус: сервис запускается — смотри overlay на экране");
        }
    }

    private Button makeButton(String text, boolean primary) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.rgb(10, 21, 25) : Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(primary ? Color.rgb(99, 220, 190) : Color.rgb(35, 45, 61));
        button.setBackground(bg);
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        return button;
    }

    private GradientDrawable panelBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(23, 30, 43));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), Color.rgb(45, 57, 75));
        return bg;
    }

    private LinearLayout.LayoutParams params(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return lp;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
