package com.dicoding.asclepius.helper

import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.net.Uri
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.IOException

class ImageClassifierHelper(private val context: Context) {
    private var imageClassifier: ImageClassifier? = null
    private lateinit var imageProcessor: ImageProcessor
    private val imageWidth = 224
    private val imageHeight = 224

    init {
        try {
            setupImageClassifier()
        } catch (e: IOException) {
            throw IllegalStateException("Error loading model: ${e.message}")
        }
    }

    private fun setupImageClassifier() {
        try {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(1)
                .build()
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                "cancer_classification.tflite",
                options
            )

            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(imageHeight, imageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .build()
        } catch (e: Exception) {
            imageClassifier = null
            throw IOException("Error setting up image classifier: ${e.message}")
        }
    }

    fun classifyStaticImage(imageUri: Uri): Pair<String, Float> {
        if (imageClassifier == null) {
            throw IllegalStateException("TensorFlow Lite classifier not initialized")
        }
        val bitmap = try {
            getBitmapFromUri(imageUri)
        } catch (e: Exception) {
            throw IOException("Error loading image: ${e.message}")
        }

        try {
            var tensorImage = TensorImage.fromBitmap(bitmap)

            tensorImage = imageProcessor.process(tensorImage)

            val results = imageClassifier?.classify(tensorImage)

            val categories = results?.firstOrNull()?.categories
                ?: throw IOException("No classification results")

            categories.forEach { category ->
                println("Category: ${category.label}, Score: ${category.score}")
            }

            val label = categories[0].label
            val confidence = categories[0].score

            val result = when {
                label == "Cancer" && confidence > 0.5 -> "Cancer Detected"
                label == "Non-Cancer" && confidence > 0.5 -> "No Cancer Detected"
                else -> label
            }


            return Pair(result, confidence)
        } catch (e: Exception) {
            throw IOException("Error running classification: ${e.message}")
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            return BitmapFactory.decodeStream(inputStream, null, options)
                ?: throw IOException("Failed to decode image")
        } ?: throw IOException("Failed to open image file")
    }

    fun close() {
        imageClassifier?.close()
        imageClassifier = null
    }
}