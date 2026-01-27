package com.example.splitcheck.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.splitcheck.sync.Endpoint
import com.example.splitcheck.sync.NearbyManager
import kotlinx.coroutines.launch

@Composable
fun ConnectScreen(navController: NavController) {

    val scope = rememberCoroutineScope()
    var endpoints by remember { mutableStateOf<List<Endpoint>>(emptyList()) }
    var status by remember { mutableStateOf("Not connected") }

    val permissions = remember {
        buildList {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }.toTypedArray()
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // ok
    }

    LaunchedEffect(Unit) {
        NearbyManager.init(navController.context)
        scope.launch {
            NearbyManager.foundEndpoints.collect { ep ->
                endpoints = (endpoints + ep).distinctBy { it.id }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Connect", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))

        Text("Status: ${if (NearbyManager.isConnected()) "Connected" else status}")
        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { permLauncher.launch(permissions) }
        ) { Text("Grant permissions") }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    status = "Advertising…"
                    endpoints = emptyList()
                    NearbyManager.startAdvertising(navController.context, "SplitCheck Host")
                }
            ) { Text("Host") }

            Spacer(Modifier.width(10.dp))

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    status = "Discovering…"
                    endpoints = emptyList()
                    NearbyManager.startDiscovery(navController.context)
                }
            ) { Text("Join") }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                NearbyManager.stopAll()
                status = "Stopped"
            }
        ) { Text("Stop search") }

        Spacer(Modifier.height(12.dp))

        Text("Found devices:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(endpoints) { ep ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            status = "Connecting to ${ep.name}…"
                            NearbyManager.requestConnection(navController.context, "SplitCheck", ep.id)
                        }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(ep.name)
                        Text(ep.id, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                onClick = {
                    NearbyManager.disconnect()
                    status = "Disconnected"
                },
                modifier = Modifier.weight(1f)
            ) { Text("Disconnect") }
        }
    }
}
