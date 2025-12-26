package com.example.splitcheck.ml

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

data class ParsedLine(
    val text: String,
    val box: Rect
)

data class ReceiptScanResult(
    val parsedLines: List<ParsedLine>,
    val rowLines: List<String>
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Double
)

object ReceiptTextRecognizer {

    /**
     * main function:
     * - recognize text
     * - collect rows into "rows" (left+right) so that there is no "left column first, then right"
     */
    suspend fun recognizeReceiptFromUri(context: Context, uri: Uri): ReceiptScanResult {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(image).await()

        val lines = mutableListOf<ParsedLine>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: Rect(0, 0, 0, 0)
                lines.add(ParsedLine(text = line.text, box = box))
            }
        }

        val rowLines = buildRowLines(lines)

        return ReceiptScanResult(
            parsedLines = lines,
            rowLines = rowLines
        )
    }

    /**
     * We glue the recognized lines into "receipt lines":
     * - group by Y (approximately one line)
     * - sort inside the line by X (left -> right)
     * - glue through the spaces
     */
    private fun buildRowLines(lines: List<ParsedLine>): List<String> {
        if (lines.isEmpty()) return emptyList()

        val heights = lines.map { (it.box.bottom - it.box.top).coerceAtLeast(1) }
        val avgH = heights.sorted()[heights.size / 2]
        val threshold = (avgH * 0.7f).coerceAtLeast(12f)

        data class Row(var centerY: Float, val parts: MutableList<ParsedLine>)

        val sortedByY = lines.sortedBy { (it.box.top + it.box.bottom) / 2f }
        val rows = mutableListOf<Row>()

        for (l in sortedByY) {
            val cy = (l.box.top + l.box.bottom) / 2f
            val existing = rows.minByOrNull { abs(it.centerY - cy) }

            if (existing != null && abs(existing.centerY - cy) <= threshold) {
                existing.parts.add(l)
                existing.centerY = (existing.centerY + cy) / 2f
            } else {
                rows.add(Row(centerY = cy, parts = mutableListOf(l)))
            }
        }

        //  collect the text in each line: left -> right
        return rows
            .sortedBy { it.centerY }
            .map { row ->
                row.parts
                    .sortedBy { it.box.left }
                    .joinToString("   ") { it.text.trim() }
                    .trim()
            }
            .filter { it.isNotBlank() }
    }


    fun extractReceiptItemsFromTextLines(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()

        val priceAtEnd = Regex("""(?:[$€₴₽]|PLN|USD|EUR)?\s*(\d+[\.,]\d{2})\s*$""", RegexOption.IGNORE_CASE)

        val qtyPrefix = Regex("""^\s*(\d+)\s*[xX]\s*""")

        val ignore = listOf(
            "total", "subtotal", "tax", "change", "cash", "card", "thank", "approval",
            "terminal", "receipt"
        )

        for (raw in lines) {
            val line = raw.trim()
            if (line.isBlank()) continue

            val lower = line.lowercase()
            if (ignore.any { lower.contains(it) }) continue

            val pm = priceAtEnd.find(line) ?: continue
            val price = pm.groupValues[1].replace(",", ".").toDoubleOrNull() ?: continue

            val qm = qtyPrefix.find(line)
            val quantity = qm?.groupValues?.get(1)?.toIntOrNull() ?: 1

            var name = line
                .replace(pm.value, "")
                .replace(qtyPrefix, "")
                .trim()

            name = name.replace("$", "").replace("€", "").replace("₴", "").replace("₽", "").trim()

            if (name.length < 2) continue

            items.add(
                ReceiptItem(
                    name = name,
                    quantity = quantity,
                    price = price
                )
            )
        }

        return items
    }
}
