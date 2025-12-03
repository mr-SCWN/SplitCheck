package com.example.splitcheck.ml

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object ReceiptTextRecognizer {

    suspend fun recognizeTextFromUri(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()
        return result.text
    }

    fun extractReceiptItems(text: String): List<ReceiptItem> {

        val rawLines = text.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val items = mutableListOf<ReceiptItem>()

        val priceRegex = Regex("""(\d+[\.,]\d{2})""")
        val quantityRegex = Regex("""(\d+)\s*[xX]""")

        var buffer: String? = null

        for (line in rawLines) {

            val priceMatch = priceRegex.find(line)

            if (priceMatch == null) {
                buffer = line 
                continue
            }

            val price = priceMatch.value.replace(",", ".").toDouble()
            val quantity = quantityRegex.find(line)?.groupValues?.get(1)?.toInt() ?: 1

            val name = buffer ?: line.replace(priceMatch.value, "").trim()

            // Убираем мусор
            if (name.contains("Order", true)) continue
            if (name.contains("Server", true)) continue
            if (name.contains("Table", true)) continue
            if (name.contains("Subtotal", true)) continue
            if (name.contains("Tax", true)) continue
            if (name.contains("Total", true)) continue
            if (name.length < 2) continue

            items.add(
                ReceiptItem(
                    name = name,
                    quantity = quantity,
                    price = price
                )
            )

            buffer = null
        }

        return items
    }
}

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Double
)
