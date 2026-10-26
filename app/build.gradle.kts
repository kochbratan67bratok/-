import java.net.URL

plugins {
    id("com.android.application")
}

android {
    namespace = "com.standoffvision.lab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.standoffvision.lab"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val modelFile = layout.projectDirectory.file("src/main/assets/efficientdet-lite0.tflite").asFile
val downloadModel by tasks.registering {
    outputs.file(modelFile)
    doLast {
        if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
            modelFile.parentFile.mkdirs()
            val tempFile = File(modelFile.parentFile, "efficientdet-lite0.tflite.part")
            val modelUrl = URL(
                "https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/object_detection/android/lite-model_efficientdet_lite0_detection_metadata_1.tflite"
            )
            val connection = modelUrl.openConnection().apply {
                connectTimeout = 20_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "StandoffVisionLab/0.1")
            }
            connection.getInputStream().use { input ->
                tempFile.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            if (tempFile.length() < 1_000_000L) {
                tempFile.delete()
                throw GradleException("Downloaded model is unexpectedly small; check the model URL.")
            }
            if (modelFile.exists()) modelFile.delete()
            if (!tempFile.renameTo(modelFile)) {
                tempFile.copyTo(modelFile, overwrite = true)
                tempFile.delete()
            }
            logger.lifecycle("Downloaded EfficientDet-Lite0 model: ${modelFile.length()} bytes")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(downloadModel)
}

dependencies {
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
}
