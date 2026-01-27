package com.example.splitcheck.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.splitcheck.ml.ReceiptItem
import com.example.splitcheck.ml.ReceiptTextRecognizer
import com.example.splitcheck.sync.NearbyManager
import com.example.splitcheck.sync.SyncState
import com.example.splitcheck.util.JsonUtil
import java.util.ArrayList

@Composable
fun ProductListScreen(
    uri: String?,
    people: Int,
    navController: NavController
) {
    val textFromPreview: String =
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("ocr_text") ?: ""

    val textLines = remember(textFromPreview) {
        textFromPreview
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val initialItems: List<ReceiptItem> = remember(textLines) {
        ReceiptTextRecognizer.extractReceiptItemsFromTextLines(textLines)
    }

    val items = remember {
        mutableStateListOf<ReceiptItem>().apply { addAll(initialItems) }
    }

    val names = remember(people) {
        mutableStateListOf<String>().apply {
            repeat(people) { add("Person ${it + 1}") }
        }
    }

    val selections = remember {
        mutableStateListOf<SnapshotStateList<Boolean>>()
    }

    LaunchedEffect(Unit) {
        if (selections.isEmpty() && items.isNotEmpty()) {
            items.forEach {
                selections.add(
                    mutableStateListOf<Boolean>().apply { repeat(people) { add(false) } }
                )
            }
        }
    }

    // delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf(-1) }

    // ---- Nearby sync: apply incoming state ----
    LaunchedEffect(Unit) {
        NearbyManager.syncFlow.collect { st ->
            // применяем только если people совпадает
            if (st.people != people) return@collect

            // names
            for (i in 0 until people) {
                if (i < names.size && i < st.names.size) names[i] = st.names[i]
            }

            // items
            items.clear()
            items.addAll(st.items)

            // selections
            selections.clear()
            st.selections.forEach { row ->
                selections.add(mutableStateListOf<Boolean>().apply { addAll(row) })
            }
        }
    }

    fun sendSync() {
        if (!NearbyManager.isConnected()) return
        val state = SyncState(
            people = people,
            names = names.toList(),
            items = items.toList(),
            selections = selections.map { it.toList() }
        )
        NearbyManager.sendSync(state)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Products & who bought", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (NearbyManager.isConnected()) {
                Text("Connected", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                "Не удалось выделить товары/цены.\n" +
                        "Вернись назад и исправь текст вручную (Edit text), чтобы строки были типа:\n" +
                        "1x T-Shirt 25.50"
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
            return@Column
        }

        Text("People names:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        for (i in 0 until people) {
            OutlinedTextField(
                value = names[i],
                onValueChange = {
                    names[i] = it
                    sendSync()
                },
                label = { Text("Person ${i + 1} name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Select who bought each item (can be multiple):", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            itemsIndexed(items) { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(
                                onClick = {
                                    deleteIndex = index
                                    showDeleteDialog = true
                                }
                            ) { Text("Delete") }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text("Qty: ${item.quantity}    Price: ${"%.2f".format(item.price)}")

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (p in 0 until people) {
                                val checked = selections.getOrNull(index)?.getOrNull(p) ?: false

                                Column(
                                    modifier = Modifier.padding(end = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { v ->
                                            selections.getOrNull(index)?.set(p, v)
                                            sendSync()
                                        }
                                    )
                                    Text(
                                        text = names[p].ifBlank { "P${p + 1}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val cleanNames = names.mapIndexed { i, s ->
                    if (s.isBlank()) "Person ${i + 1}" else s.trim()
                }

                val owed = DoubleArray(people) { 0.0 }
                val total = items.sumOf { it.price }

                items.forEachIndexed { i, it ->
                    val selected = selections[i]
                        .mapIndexedNotNull { idx, v -> if (v) idx else null }

                    if (selected.isEmpty()) return@forEachIndexed

                    val share = it.price / selected.size.toDouble()
                    selected.forEach { p -> owed[p] += share }
                }

                // в Summary передаём всё, чтобы сохранить в историю
                navController.currentBackStackEntry?.savedStateHandle?.set("session_uri", uri ?: "")
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_names", ArrayList(cleanNames))
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_owed", ArrayList(owed.toList()))
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_total", total)

                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "session_items_json",
                    JsonUtil.gson.toJson(items.toList())
                )
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    "session_selections_json",
                    JsonUtil.gson.toJson(selections.map { it.toList() })
                )

                navController.navigate("summary")
            }
        ) { Text("Show summary") }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete item?") },
            text = {
                val name = items.getOrNull(deleteIndex)?.name ?: ""
                Text("Are you sure you want to delete \"$name\"?\nThis item will not be counted in the summary.")
            },
            confirmButton = {
                Button(onClick = {
                    if (deleteIndex in items.indices) {
                        items.removeAt(deleteIndex)
                        selections.removeAt(deleteIndex)
                        sendSync()
                    }
                    showDeleteDialog = false
                    deleteIndex = -1
                }) { Text("Yes, delete") }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    deleteIndex = -1
                }) { Text("Cancel") }
            }
        )
    }
}
