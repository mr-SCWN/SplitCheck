package com.example.splitcheck.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.splitcheck.ml.ReceiptItem
import com.example.splitcheck.ml.ReceiptTextRecognizer

@Composable
fun ProductListScreen(uri: String?, people: Int) {

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf(emptyList<ReceiptItem>()) }

    LaunchedEffect(uri) {
        if (uri != null) {
            val text = ReceiptTextRecognizer.recognizeTextFromUri(context, Uri.parse(uri))
            items = ReceiptTextRecognizer.extractReceiptItems(text)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Extracted Receipt Items", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(items) { item ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Name: ${item.name}")
                        Text("Price: ${item.price}")
                        Text("Quantity: ${item.quantity}")
                    }
                }
            }
        }
    }
}
