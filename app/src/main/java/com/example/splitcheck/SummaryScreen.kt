package com.example.splitcheck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.splitcheck.storage.ReceiptSessionRecord
import com.example.splitcheck.util.JsonUtil
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.UUID
import com.example.splitcheck.storage.ReceiptHistoryStore


@Composable
fun SummaryScreen(navController: NavController) {

    val prev = navController.previousBackStackEntry?.savedStateHandle

    val uri = (prev?.get<String>("session_uri") ?: "")
    val names = (prev?.get<ArrayList<String>>("summary_names") ?: arrayListOf())
    val owed = (prev?.get<ArrayList<Double>>("summary_owed") ?: arrayListOf())
    val total = (prev?.get<Double>("summary_total") ?: 0.0)

    val itemsJson = prev?.get<String>("session_items_json") ?: "[]"
    val selectionsJson = prev?.get<String>("session_selections_json") ?: "[]"

    var payerIndex by remember { mutableStateOf(0) }
    var payerMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var savedInfo by remember { mutableStateOf<String?>(null) }

    // parse items + selections
    val itemsType = object : TypeToken<List<com.example.splitcheck.ml.ReceiptItem>>() {}.type
    val selType = object : TypeToken<List<List<Boolean>>>() {}.type

    val items = remember(itemsJson) { JsonUtil.gson.fromJson<List<com.example.splitcheck.ml.ReceiptItem>>(itemsJson, itemsType) ?: emptyList() }
    val selections = remember(selectionsJson) { JsonUtil.gson.fromJson<List<List<Boolean>>>(selectionsJson, selType) ?: emptyList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Summary", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Text("Total receipt: ${"%.2f".format(total)}")
        Spacer(Modifier.height(12.dp))

        Text("Who paid the bill? (optional)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Box {
            Button(onClick = { payerMenu = true }) {
                Text(if (names.isNotEmpty()) names[payerIndex] else "Select payer")
            }
            DropdownMenu(expanded = payerMenu, onDismissRequest = { payerMenu = false }) {
                names.forEachIndexed { i, n ->
                    DropdownMenuItem(
                        text = { Text(n) },
                        onClick = {
                            payerIndex = i
                            payerMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Each person owes:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        val payerPaid = total
        val balances = names.indices.map { i ->
            val owe = owed.getOrNull(i) ?: 0.0
            val paid = if (i == payerIndex) payerPaid else 0.0
            owe - paid
        }

        for (i in names.indices) {
            val owe = owed.getOrNull(i) ?: 0.0
            val bal = balances[i]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(names[i], style = MaterialTheme.typography.titleMedium)
                    Text("Owes (by items): ${"%.2f".format(owe)}")
                    if (i == payerIndex) Text("Paid: ${"%.2f".format(payerPaid)}")
                    Text(
                        text = if (bal > 0) "Should pay: ${"%.2f".format(bal)}"
                        else "Should receive: ${"%.2f".format(-bal)}"
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    val record = ReceiptSessionRecord(
                        id = UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        receiptUri = uri,
                        peopleNames = names.toList(),
                        items = items,
                        selections = selections,
                        owed = owed.toList(),
                        total = total,
                        payerIndex = payerIndex
                    )
                    ReceiptHistoryStore.save(navController.context, record)
                    savedInfo = "Saved to history!"
                }
            }
        ) { Text("Save to history") }

        savedInfo?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(onClick = { navController.navigate("history") }, modifier = Modifier.weight(1f)) {
                Text("Open history")
            }
        }
    }
}
