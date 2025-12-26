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
import java.util.ArrayList

@Composable
fun ProductListScreen(
    uri: String?,
    people: Int,
    navController: NavController
) {
    // take extracted text
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

    // people names
    val names = remember(people) {
        mutableStateListOf<String>().apply {
            repeat(people) { add("Person ${it + 1}") }
        }
    }

    // selections[itemIndex][personIndex]
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

    // dialog to delete object
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Products & who bought", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                "It's not possible to select products and prices (or everything was deleted).\n" +
                        "Go back and correct the text manually (Edit text) so that the lines are like:\n" +
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
                onValueChange = { names[i] = it },
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
                            ) {
                                Text("Delete")
                            }
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

                navController.currentBackStackEntry?.savedStateHandle?.set("summary_names", ArrayList(cleanNames))
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_owed", ArrayList(owed.toList()))
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_total", total)

                navController.navigate("summary")
            }
        ) {
            Text("Show summary")
        }
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

@Composable
fun SummaryScreen(navController: NavController) {
    val prev = navController.previousBackStackEntry?.savedStateHandle
    val names = (prev?.get<ArrayList<String>>("summary_names") ?: arrayListOf())
    val owed = (prev?.get<ArrayList<Double>>("summary_owed") ?: arrayListOf())
    val total = (prev?.get<Double>("summary_total") ?: 0.0)

    var payerIndex by remember { mutableStateOf(0) }
    var payerMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Summary", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Text("Total receipt: ${"%.2f".format(total)}")
        Spacer(Modifier.height(12.dp))

        Text("Who paid the bill? (optional)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Box {
            Button(onClick = { payerMenu = true }) {
                Text(if (names.isNotEmpty()) names[payerIndex] else "Select payer")
            }
            DropdownMenu(expanded = payerMenu, onDismissRequest = { payerMenu = false }) {
                names.forEachIndexed { i, n ->
                    DropdownMenuItem(
                        text = { Text(n) },
                        onClick = {
                            payerIndex = i
                            payerMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Each person owes:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        val payerPaid = total
        val balances = names.indices.map { i ->
            val owe = owed.getOrNull(i) ?: 0.0
            val paid = if (i == payerIndex) payerPaid else 0.0
            owe - paid
        }

        for (i in names.indices) {
            val owe = owed.getOrNull(i) ?: 0.0
            val bal = balances[i]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(names[i], style = MaterialTheme.typography.titleMedium)
                    Text("Owes (by items): ${"%.2f".format(owe)}")
                    if (i == payerIndex) {
                        Text("Paid: ${"%.2f".format(payerPaid)}")
                    }
                    Text(
                        text = if (bal > 0) "Should pay: ${"%.2f".format(bal)}"
                        else "Should receive: ${"%.2f".format(-bal)}"
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}
