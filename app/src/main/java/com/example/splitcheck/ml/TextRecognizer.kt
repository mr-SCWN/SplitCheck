package com.example.splitcheck.ml

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.max

object ReceiptTextRecognizer {


    suspend fun recognizeReceiptFromUri(context: Context, uri: Uri): ReceiptOcrResult {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()

        val raw = mutableListOf<OcrLine>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                raw.add(
                    OcrLine(
                        text = line.text.trim(),
                        box = line.boundingBox ?: Rect(0, 0, 0, 0)
                    )
                )
            }
        }

        val sorted = raw.sortedBy { it.box.centerY() }

        val rows = mutableListOf<MutableList<OcrLine>>()
        for (ln in sorted) {
            val h = max(ln.box.height(), 1)
            val threshold = max(12, (h * 0.65f).toInt()) // адаптивно
            val placed = rows.lastOrNull()?.let { lastRow ->
                val rowY = lastRow.map { it.box.centerY() }.average()
                if (abs(ln.box.centerY() - rowY) <= threshold) {
                    lastRow.add(ln); true
                } else false
            } ?: false

            if (!placed) rows.add(mutableListOf(ln))
        }

        val rowLines = rows
            .map { row ->
                row.sortedBy { it.box.left }
                    .joinToString("    ") { it.text }
                    .replace(Regex("\\s{2,}"), "  ")
                    .trim()
            }
            .filter { it.isNotBlank() }

        return ReceiptOcrResult(rawLines = raw, rowLines = rowLines)
    }


    fun extractReceiptItemsFromTextLines(lines: List<String>): List<ReceiptItem> {

        val ignoreWords = listOf(
            "total", "subtotal", "tax", "cash", "change",
            "thank", "card", "visa", "master", "amount", "receipt"
        )

        // qty optional, name, price at end
        val pattern = Regex("""^\s*(?:(\d+)\s*[xX]\s*)?(.+?)\s+[$€£]?\s*(\d+[.,]\d{2})\s*$""")

        val items = mutableListOf<ReceiptItem>()

        for (raw in lines) {
            val line = raw.trim()


            val lower = line.lowercase()
            if (ignoreWords.any { lower.contains(it) }) continue

            val m = pattern.find(line) ?: continue

            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val name = m.groupValues[2]
                .trim()
                .trimEnd('$', '€', '£')
                .trim()

            val price = m.groupValues[3].replace(",", ".").toDoubleOrNull() ?: continue

            if (name.length < 2) continue
            if (price <= 0.0) continue

            items.add(ReceiptItem(name = name, quantity = qty, price = price))
        }

        return items
    }
}

data class ReceiptOcrResult(
    val rawLines: List<OcrLine>,
    val rowLines: List<String>
)

data class OcrLine(
    val text: String,
    val box: Rect
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Double
)
