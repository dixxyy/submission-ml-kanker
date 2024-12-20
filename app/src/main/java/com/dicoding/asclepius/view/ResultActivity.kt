package com.dicoding.asclepius.view

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private var imageClassifierHelper: ImageClassifierHelper? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            imageClassifierHelper = ImageClassifierHelper(this)
        } catch (e: Exception) {
            handleError("Failed to initialize classifier: ${e.message}")
            return
        }

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)

            try {
                binding.resultImage.setImageURI(imageUri)
            } catch (e: Exception) {
                handleError("Failed to load image: ${e.message}")
                return
            }

            try {
                val (result, confidence) = imageClassifierHelper?.classifyStaticImage(imageUri)
                    ?: throw IllegalStateException("Classifier not initialized")

                val confidencePercentage = (confidence * 100).toInt()

                binding.resultText.text = """
                    Classification Result : $result
                    Confidence Score : $confidencePercentage%
                """.trimIndent()
            } catch (e: Exception) {
                handleError("Error analyzing image: ${e.message}")
            }
        } else {
            handleError("No image provided")
        }
    }

    private fun handleError(message: String) {
        binding.resultText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        imageClassifierHelper?.close()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}