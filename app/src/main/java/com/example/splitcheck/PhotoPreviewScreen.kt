package com.example.splitcheck.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun PhotoPreviewScreen(uri: String?) {

    var peopleCount by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Receipt Preview", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(30.dp))

        Text("How many people?", style = MaterialTheme.typography.titleMedium)

        Row(verticalAlignment = Alignment.CenterVertically) {

            Button(onClick = { if (peopleCount > 1) peopleCount-- }) {
                Text("-")
            }

            Text(
                text = peopleCount.toString(),
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Button(onClick = { peopleCount++ }) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { /* NEXT */ }) {
            Text("Continue")
        }
    }
}
