package com.example.splitcheck.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun HistoryScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<ReceiptSessionRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            list = ReceiptHistoryStore.loadAll(navController.context)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (list.isEmpty()) {
            Text("No saved receipts yet.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            return@Column
        }

        val df = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

        LazyColumn(Modifier.fillMaxSize()) {
            items(list) { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { navController.navigate("history_detail/${rec.id}") }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Total: ${"%.2f".format(rec.total)}")
                        Text("People: ${rec.peopleNames.size}")
                        Text("Items: ${rec.items.size}")
                        Text("Date: ${df.format(Date(rec.createdAt))}")
                    }
                }
            }
        }
    }
}
