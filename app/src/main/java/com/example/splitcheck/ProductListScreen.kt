package com.example.splitcheck.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    // take text from previous screen
    val textFromPreview: String =
        navController.previousBackStackEntry?.savedStateHandle?.get<String>("ocr_text") ?: ""

    val textLines = remember(textFromPreview) {
        textFromPreview
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val items: List<ReceiptItem> = remember(textLines) {
        ReceiptTextRecognizer.extractReceiptItemsFromTextLines(textLines)
    }

    // people names
    val names = remember(people) {
        mutableStateListOf<String>().apply {
            repeat(people) { add("Person ${it + 1}") }
        }
    }

    // selections[itemIndex][personIndex] = true/false
    val selections = remember(items, people) {
        items.map {
            mutableStateListOf<Boolean>().apply { repeat(people) { add(false) } }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Products & who bought", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                "Не удалось выделить товары и цены из текста.\n" +
                        "Вернись назад и поправь текст вручную (Edit text), чтобы строки были типа:\n" +
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
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
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
                                val checked = selections[index][p]
                                Column(
                                    modifier = Modifier.padding(end = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { selections[index][p] = it }
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

                // save data for SummaryScreen using savedStateHandle
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_names", ArrayList(cleanNames))
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_owed", ArrayList(owed.toList()))
                navController.currentBackStackEntry?.savedStateHandle?.set("summary_total", total)

                navController.navigate("summary")
            }
        ) {
            Text("Show summary")
        }
    }
}

@Composable
fun SummaryScreen(navController: NavController) {
    // Достаём результаты из предыдущего экрана (products)
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

        // Кто платил (опционально)
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
            owe - paid // >0: должен, <0: должен получить
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
