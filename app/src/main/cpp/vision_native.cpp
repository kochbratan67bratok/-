#include <jni.h>
#include <cmath>
#include <limits>

// Visual-only target selection. Coordinates are in overlay pixels.
// Returns the index of the closest point inside the configured FOV radius,
// or -1 when no point is inside the circle.
extern "C"
JNIEXPORT jint JNICALL
Java_com_standoffvision_lab_DetectionOverlay_nativeNearestPoint(
        JNIEnv *env, jclass, jfloatArray pointsArray,
        jfloat centerX, jfloat centerY, jfloat radius) {
    if (pointsArray == nullptr || radius <= 0.0f) return -1;

    const jsize length = env->GetArrayLength(pointsArray);
    if (length < 2) return -1;

    jfloat *points = env->GetFloatArrayElements(pointsArray, nullptr);
    if (points == nullptr) return -1;

    const float radiusSquared = radius * radius;
    float bestDistance = std::numeric_limits<float>::max();
    jint bestIndex = -1;

    for (jsize i = 0; i + 1 < length; i += 2) {
        const float dx = points[i] - centerX;
        const float dy = points[i + 1] - centerY;
        const float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared <= radiusSquared && distanceSquared < bestDistance) {
            bestDistance = distanceSquared;
            bestIndex = static_cast<jint>(i / 2);
        }
    }

    env->ReleaseFloatArrayElements(pointsArray, points, JNI_ABORT);
    return bestIndex;
}
