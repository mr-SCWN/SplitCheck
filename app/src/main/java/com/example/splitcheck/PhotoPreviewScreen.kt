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
    val coroutine = rememberCoroutineScope()

    var peopleCount by remember { mutableStateOf(1) }
    var ocrText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        item {
            Text(
                "Receipt Preview",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(20.dp))
        }

        item {
            uri?.let {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(it)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Button(onClick = {
                if (uri != null) {
                    loading = true
                    coroutine.launch {
                        ocrText = ReceiptTextRecognizer.recognizeTextFromUri(
                            context,
                            Uri.parse(uri)
                        )
                        loading = false
                    }
                }
            }) {
                Text("Scan Receipt")
            }
        }

        if (loading) {
            item { Text("Processing…") }
        }

        if (ocrText.isNotBlank()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("Extracted text:", style = MaterialTheme.typography.titleMedium)
                Text(ocrText)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // People selector
        item {
            Text("How many people?", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {

                Button(onClick = { if (peopleCount > 1) peopleCount-- }) {
                    Text("-")
                }

                Text(
                    peopleCount.toString(),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.headlineMedium
                )

                Button(onClick = { peopleCount++ }) {
                    Text("+")
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            Button(onClick = {
                if (uri != null) {
                    navController.navigate(
                        "products?uri=${Uri.encode(uri)}&people=$peopleCount"
                    )
                }
            }) {
                Text("Continue")
            }
        }
    }
}
