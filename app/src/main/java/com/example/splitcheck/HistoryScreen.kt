package com.example.splitcheck.ui

import androidx.compose.foundation.clickable
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
fun HistoryScreen(navController: NavController) {

    val context = LocalContext.current

    var history by remember { mutableStateOf<List<ReceiptSessionRecord>>(emptyList()) }

    fun reload() {
        history = ReceiptHistoryStore.load(context)
    }

    LaunchedEffect(Unit) {
        reload()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ReceiptSessionRecord?>(null) }

    val df = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (history.isEmpty()) {
            Text("History is empty.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(history, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("history_detail?id=${rec.id}")
                            }
                            .padding(12.dp)
                    ) {
                        Text("Total: ${"%.2f".format(rec.total)}")
                        Text("People: ${rec.peopleNames.size}")
                        Text("Items: ${rec.items.size}")
                        Text("Date: ${df.format(Date(rec.createdAt))}")

                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth()) {
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    pendingDelete = rec
                                    showDeleteDialog = true
                                }
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }

    if (showDeleteDialog) {
        val rec = pendingDelete
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                pendingDelete = null
            },
            title = { Text("Delete from history?") },
            text = {
                Text(
                    "Are you sure you want to delete this record?\n" +
                            "This cannot be undone."
                )
            },
            confirmButton = {
                Button(onClick = {
                    rec?.let { ReceiptHistoryStore.delete(context, it.id) }
                    reload()
                    showDeleteDialog = false
                    pendingDelete = null
                }) {
                    Text("Yes, delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteDialog = false
                    pendingDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
