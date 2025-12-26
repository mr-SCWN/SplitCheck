package com.example.splitcheck.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.splitcheck.ml.ReceiptTextRecognizer
import kotlinx.coroutines.launch

@Composable
fun PhotoPreviewScreen(uri: String?, navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var peopleCount by remember { mutableStateOf(1) }

    var extractedText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    var showEdit by remember { mutableStateOf(false) }
    var editableText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Text("Receipt Preview", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
        }

        item {
            uri?.let {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(it)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Button(
                onClick = {
                    if (uri == null) return@Button
                    loading = true
                    scope.launch {
                        val result = ReceiptTextRecognizer.recognizeReceiptFromUri(
                            context = context,
                            uri = Uri.parse(uri)
                        )
                        // ВАЖНО: берём "строки по рядам" — там левый+правый столбец склеены
                        extractedText = result.rowLines.joinToString("\n")
                        editableText = extractedText
                        loading = false
                    }
                }
            ) { Text("Scan Receipt") }
        }

        if (loading) {
            item {
                Spacer(Modifier.height(10.dp))
                Text("Processing…")
            }
        }

        if (extractedText.isNotBlank()) {
            item {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { showEdit = true }) { Text("Edit text") }

                Spacer(Modifier.height(12.dp))
                Text("Extracted text:", style = MaterialTheme.typography.titleMedium)

                // Чтобы текст не "ломал" скролл — он внутри LazyColumn как item
                Text(extractedText)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }

        item {
            Text("How many people?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { if (peopleCount > 1) peopleCount-- }) { Text("-") }
                Text(
                    peopleCount.toString(),
                    modifier = Modifier.padding(horizontal = 18.dp),
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(onClick = { peopleCount++ }) { Text("+") }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            Button(
                enabled = editableText.isNotBlank() && uri != null,
                onClick = {
                    // save extracted text
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "ocr_text",
                        editableText
                    )
                    navController.navigate("products?uri=${Uri.encode(uri!!)}&people=$peopleCount")
                }
            ) { Text("Continue") }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit extracted text") },
            text = {
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    extractedText = editableText
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { showEdit = false }) { Text("Cancel") }
            }
        )
    }
}
