# Standoff Vision Lab

Android 10–15 prototype for on-device screen object detection and a transparent visual overlay.

## Build

Open this repository in Android Studio with Android SDK 35 and JDK 17, or open **Actions → Android debug APK** and download the APK artifact from the latest successful run. The first Gradle build downloads the official TensorFlow Lite EfficientDet-Lite0 model automatically.

## Prototype status

- Android MediaProjection capture with system consent.
- TensorFlow Lite object detection running locally.
- Transparent overlay with bounding boxes, labels, confidence scores, FPS, and a visual marker for the nearest detected COCO class "person".
- No root required.
- The model is trained on COCO, not on Standoff 2 screenshots, so game characters may not be detected reliably. The next accuracy step is a custom model trained on labelled gameplay frames.
- This prototype does not read hidden game memory, modify the game's process, or automate game touch input.

See the source files and the GitHub Actions workflow in this repository.
