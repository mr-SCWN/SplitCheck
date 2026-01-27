package com.example.splitcheck.storage

import android.content.Context
import com.example.splitcheck.util.JsonUtil
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ReceiptHistoryStore {

    private const val FILE_NAME = "splitcheck_history.json"

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    suspend fun loadAll(context: Context): List<ReceiptSessionRecord> = withContext(Dispatchers.IO) {
        val f = file(context)
        if (!f.exists()) return@withContext emptyList()

        val txt = f.readText()
        if (txt.isBlank()) return@withContext emptyList()

        val type = object : TypeToken<List<ReceiptSessionRecord>>() {}.type
        JsonUtil.gson.fromJson<List<ReceiptSessionRecord>>(txt, type) ?: emptyList()
    }

    suspend fun save(context: Context, record: ReceiptSessionRecord) = withContext(Dispatchers.IO) {
        val all = loadAll(context).toMutableList()
        all.add(0, record) // newest first
        file(context).writeText(JsonUtil.gson.toJson(all))
    }

    suspend fun findById(context: Context, id: String): ReceiptSessionRecord? = withContext(Dispatchers.IO) {
        loadAll(context).firstOrNull { it.id == id }
    }
}
