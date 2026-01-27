package com.example.splitcheck.storage

import com.example.splitcheck.ml.ReceiptItem

data class ReceiptSessionRecord(
    val id: String,
    val createdAt: Long,
    val receiptUri: String,
    val peopleNames: List<String>,
    val items: List<ReceiptItem>,
    val selections: List<List<Boolean>>,
    val owed: List<Double>,
    val total: Double,
    val payerIndex: Int
)
