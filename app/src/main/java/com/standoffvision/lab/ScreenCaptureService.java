package com.standoffvision.lab;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.graphics.drawable.Icon;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.content.pm.ServiceInfo;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ScreenCaptureService extends Service {
    public static final String EXTRA_RESULT_CODE = "capture_result_code";
    public static final String EXTRA_RESULT_DATA = "capture_result_data";
    private static final String ACTION_STOP = "com.standoffvision.lab.STOP";
    private static final String CHANNEL_ID = "vision_capture";
    private static final int NOTIFICATION_ID = 4102;
    private static final int MAX_CAPTURE_DIMENSION = 960;
    private static final long MIN_INFERENCE_INTERVAL_MS = 180L;

    private HandlerThread workerThread;
    private Handler worker;
    private MediaProjection projection;
    private MediaProjection.Callback projectionCallback;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private ObjectDetector detector;
    private WindowManager windowManager;
    private DetectionOverlay overlay;
    private WindowManager.LayoutParams overlayParams;

    private int screenWidth;
    private int screenHeight;
    private int captureWidth;
    private int captureHeight;
    private int densityDpi;
    private long lastInferenceAt;
    private long fpsWindowStart;
    private int framesInWindow;
    private float reportedFps;
    private volatile boolean stopping;

    @Override
    public void onCreate() {
        super.onCreate();
        workerThread = new HandlerThread("VisionCaptureWorker");
        workerThread.start();
        worker = new Handler(workerThread.getLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopCapture();
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (projection != null) return START_NOT_STICKY;
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        Intent resultData;
        if (Build.VERSION.SDK_INT >= 33) {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        } else {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        }
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        try {
            initialiseDetector();
            initialiseOverlay();
            initialiseProjection(resultCode, resultData);
        } catch (Exception e) {
            if (overlay != null) overlay.setStatus("Ошибка запуска: " + e.getClass().getSimpleName());
            stopCapture();
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    private void initialiseDetector() {
        ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(0.35f)
                .setMaxResults(12)
                .setBaseOptions(BaseOptions.builder().setNumThreads(2).build())
                .build();
        detector = ObjectDetector.createFromFileAndOptions(this, "efficientdet-lite0.tflite", options);
    }

    private void initialiseOverlay() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        densityDpi = metrics.densityDpi;

        float scale = Math.min(1f, MAX_CAPTURE_DIMENSION / (float) Math.max(screenWidth, screenHeight));
        captureWidth = Math.max(1, Math.round(screenWidth * scale));
        captureHeight = Math.max(1, Math.round(screenHeight * scale));

        if (!android.provider.Settings.canDrawOverlays(this)) {
            throw new IllegalStateException("Разрешение на отображение поверх других приложений не выдано");
        }
        overlay = new DetectionOverlay(this);
        overlayParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(overlay, overlayParams);
        overlay.setStatus("AI: запуск модели EfficientDet-Lite0…");
    }

    private void initialiseProjection(int resultCode, Intent resultData) {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection = manager.getMediaProjection(resultCode, resultData);
        if (projection == null) throw new IllegalStateException("MediaProjection не создан");

        projectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopSelf();
            }
        };
        projection.registerCallback(projectionCallback, worker);

        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(this::processFrame, worker);
        virtualDisplay = projection.createVirtualDisplay(
                "StandoffVisionLab",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                worker);
        if (virtualDisplay == null) throw new IllegalStateException("Не удалось создать VirtualDisplay");
        lastInferenceAt = 0L;
        fpsWindowStart = SystemClock.elapsedRealtime();
        framesInWindow = 0;
    }

    private void processFrame(ImageReader reader) {
        Image image = null;
        Bitmap bitmap = null;
        try {
            image = reader.acquireLatestImage();
            if (image == null || stopping) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastInferenceAt < MIN_INFERENCE_INTERVAL_MS) return;
            lastInferenceAt = now;

            bitmap = imageToBitmap(image);
            if (bitmap == null || detector == null) return;
            long inferenceStart = SystemClock.elapsedRealtime();
            List<Detection> detections = detector.detect(TensorImage.fromBitmap(bitmap));
            long inferenceMs = SystemClock.elapsedRealtime() - inferenceStart;

            List<DetectionOverlay.Item> items = new ArrayList<>();
            for (Detection detection : detections) {
                if (detection.getCategories() == null || detection.getCategories().isEmpty()) continue;
                Category category = detection.getCategories().get(0);
                RectF box = new RectF(detection.getBoundingBox());
                items.add(new DetectionOverlay.Item(box, category.getLabel(), category.getScore()));
            }

            framesInWindow++;
            long elapsed = now - fpsWindowStart;
            if (elapsed >= 1000L) {
                reportedFps = framesInWindow * 1000f / Math.max(1L, elapsed);
                framesInWindow = 0;
                fpsWindowStart = now;
            }
            if (overlay != null) overlay.update(items, captureWidth, captureHeight, reportedFps, inferenceMs, "COCO");
        } catch (Exception e) {
            if (overlay != null) overlay.setStatus("AI error: " + e.getClass().getSimpleName());
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            if (image != null) image.close();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer().duplicate();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int rowOffset = y * rowStride;
            int outOffset = y * width;
            for (int x = 0; x < width; x++) {
                int offset = rowOffset + x * pixelStride;
                int r = buffer.get(offset) & 0xFF;
                int g = buffer.get(offset + 1) & 0xFF;
                int b = buffer.get(offset + 2) & 0xFF;
                int a = buffer.get(offset + 3) & 0xFF;
                pixels[outOffset + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, ScreenCaptureService.class).setAction(ACTION_STOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 4103, stopIntent, flags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Standoff Vision Lab")
                .setContentText("Захват экрана и локальное распознавание работают")
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                        "Остановить",
                        stopPendingIntent).build());
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Vision screen capture", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Уведомление сервиса распознавания экрана");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void stopCapture() {
        if (stopping) return;
        stopping = true;
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (projection != null) {
            try {
                if (projectionCallback != null) projection.unregisterCallback(projectionCallback);
                projection.stop();
            } catch (Exception ignored) { }
            projection = null;
        }
        if (detector != null) {
            try { detector.close(); } catch (Exception ignored) { }
            detector = null;
        }
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) { }
            overlay = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        if (workerThread != null) workerThread.quitSafely();
    }

    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
