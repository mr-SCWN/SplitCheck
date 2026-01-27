package com.example.splitcheck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.splitcheck.storage.ReceiptHistoryStore
import com.example.splitcheck.storage.ReceiptSessionRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDetailScreen(navController: NavController, id: String) {
    val scope = rememberCoroutineScope()
    var rec by remember { mutableStateOf<ReceiptSessionRecord?>(null) }

    LaunchedEffect(id) {
        scope.launch {
            rec = ReceiptHistoryStore.findById(navController.context, id)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History detail", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        val r = rec
        if (r == null) {
            Text("Loading…")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            return@Column
        }

        val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

        Text("Date: ${df.format(Date(r.createdAt))}")
        Text("Total: ${"%.2f".format(r.total)}")
        Spacer(Modifier.height(10.dp))

        Text("People:", style = MaterialTheme.typography.titleMedium)
        r.peopleNames.forEach { Text("• $it") }

        Spacer(Modifier.height(12.dp))
        Text("Items:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(r.items) { idx, it ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it.name, style = MaterialTheme.typography.titleMedium)
                        Text("Qty: ${it.quantity}   Price: ${"%.2f".format(it.price)}")
                        val buyers = r.selections.getOrNull(idx)
                            ?.mapIndexedNotNull { p, v -> if (v) r.peopleNames.getOrNull(p) else null }
                            ?: emptyList()
                        Text("Bought by: ${if (buyers.isEmpty()) "(none)" else buyers.joinToString(", ")}")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { navController.popBackStack() }) { Text("Back") }
    }
}
