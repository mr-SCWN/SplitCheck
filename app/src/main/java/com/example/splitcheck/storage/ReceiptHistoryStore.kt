package com.example.splitcheck.storage

import android.content.Context
import com.example.splitcheck.util.JsonUtil
import com.google.gson.reflect.TypeToken

object ReceiptHistoryStore {

    private const val PREFS = "splitcheck_history"
    private const val KEY = "history_json"

    fun load(context: Context): List<ReceiptSessionRecord> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = sp.getString(KEY, "[]") ?: "[]"

        val type = object : TypeToken<List<ReceiptSessionRecord>>() {}.type
        return JsonUtil.gson.fromJson<List<ReceiptSessionRecord>>(json, type) ?: emptyList()
    }

    fun save(context: Context, record: ReceiptSessionRecord) {
        val current = load(context).toMutableList()

        current.add(0, record)

        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putString(KEY, JsonUtil.gson.toJson(current)).apply()
    }

    fun delete(context: Context, id: String) {
        val current = load(context).toMutableList()
        val newList = current.filter { it.id != id }

        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putString(KEY, JsonUtil.gson.toJson(newList)).apply()
    }

    fun clear(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putString(KEY, "[]").apply()
    }
}
