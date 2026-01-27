package com.example.splitcheck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.splitcheck.storage.ReceiptHistoryStore
import com.example.splitcheck.storage.ReceiptSessionRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDetailScreen(navController: NavController, id: String) {

    val context = LocalContext.current
    val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val record: ReceiptSessionRecord? = remember(id) {
        ReceiptHistoryStore.load(context).firstOrNull { it.id == id }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (record == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Record not found.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("History detail", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        Text("Date: ${df.format(Date(record.createdAt))}")
        Text("Total: ${"%.2f".format(record.total)}")

        Spacer(Modifier.height(12.dp))
        Text("People:", style = MaterialTheme.typography.titleMedium)
        record.peopleNames.forEach { Text("• $it") }

        Spacer(Modifier.height(12.dp))
        Text("Items:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(record.items) { it ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {

                        val buyers = record.selections
                            .getOrNull(record.items.indexOf(it))
                            ?.mapIndexedNotNull { idx, v -> if (v) record.peopleNames.getOrNull(idx) else null }
                            ?: emptyList()

                        Text(it.name, style = MaterialTheme.typography.titleMedium)
                        Text("Qty: ${it.quantity}   Price: ${"%.2f".format(it.price)}")
                        Text("Bought by: ${if (buyers.isEmpty()) "-" else buyers.joinToString(", ")}")
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete from history?") },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    ReceiptHistoryStore.delete(context, record.id)
                    showDeleteDialog = false
                    navController.popBackStack() // назад в список
                }) { Text("Yes, delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
