package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Transaction
import com.example.model.TransactionWithItems
import com.example.ui.ActiveRole
import com.example.ui.DukaLeoViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: DukaLeoViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val allTxnsWithItems by viewModel.allTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("All") } // "All", "COMPLETED", "CANCELLED"
    var selectedPayment by remember { mutableStateOf("All") } // "All", "CASH", "MOBILE_MONEY"
    var selectedTransaction by remember { mutableStateOf<TransactionWithItems?>(null) }
    var showCancellationDialog by remember { mutableStateOf<Transaction?>(null) }

    val filteredTransactions = remember(allTxnsWithItems, searchQuery, selectedStatus, selectedPayment, currentRole) {
        allTxnsWithItems.filter { item ->
            val txn = item.transaction
            val matchesRole = when (val role = currentRole) {
                is ActiveRole.Cashier -> txn.cashierId == role.user.id // Cashiers only see their own sales
                else -> true // Owner sees all
            }

            val matchesQuery = searchQuery.isBlank() ||
                    txn.transactionNumber.contains(searchQuery, ignoreCase = true) ||
                    txn.cashierName.contains(searchQuery, ignoreCase = true) ||
                    item.items.any { it.productName.contains(searchQuery, ignoreCase = true) }

            val matchesStatus = selectedStatus == "All" || txn.status == selectedStatus
            val matchesPayment = selectedPayment == "All" || txn.paymentMethod == selectedPayment

            matchesRole && matchesQuery && matchesStatus && matchesPayment
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                            Text(
                                text = if (currentRole is ActiveRole.Cashier) "My Sales Today" else "Sales History",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${filteredTransactions.size} sales",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by TXN #, product, or cashier...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filters
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All", "COMPLETED", "CANCELLED")) { status ->
                            val isSel = status == selectedStatus
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedStatus = status },
                                label = { Text(if (status == "COMPLETED") "Completed" else if (status == "CANCELLED") "Cancelled" else "All Status") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (status == "CANCELLED") StatusError else MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        items(listOf("All", "CASH", "MOBILE_MONEY")) { pay ->
                            if (pay != "All") {
                                val isSel = pay == selectedPayment
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedPayment = if (isSel) "All" else pay },
                                    label = { Text(if (pay == "CASH") "Cash" else "Mobile Money") }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("sales_history_screen")
        ) {
            if (filteredTransactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "No sales found",
                    message = "Completed sales transactions will appear here.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions) { item ->
                        TransactionRowCard(
                            item = item,
                            onClick = { selectedTransaction = item }
                        )
                    }
                }
            }
        }
    }

    // Transaction Details Sheet
    if (selectedTransaction != null) {
        val txnWithItems = selectedTransaction!!
        val txn = txnWithItems.transaction

        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = txn.transactionNumber,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = FormatUtils.formatDateTime(txn.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (txn.status == "COMPLETED") Color(0xFFD1FAE5) else Color(0xFFFFE4E6)
                    ) {
                        Text(
                            text = txn.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (txn.status == "COMPLETED") StatusSuccess else StatusError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Meta Info
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Cashier", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(txn.cashierName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Payment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(txn.paymentMethod.replace("_", " "), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Items Sold (${txnWithItems.items.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Locked items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(txnWithItems.items) { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${line.quantity} × ${FormatUtils.formatCurrency(line.sellingPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = FormatUtils.formatCurrency(line.subtotal),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Sale Amount", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = FormatUtils.formatCurrency(txn.totalAmount),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (txn.status == "CANCELLED" && txn.cancellationReason != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFE4E6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Cancellation Reason:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = StatusError)
                            Text(txn.cancellationReason, style = MaterialTheme.typography.bodySmall, color = Color(0xFF991B1B))
                            if (txn.approvedBy != null) {
                                Text("Approved by: ${txn.approvedBy}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = Color(0xFF991B1B))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cashier Action: Request Cancellation
                if (txn.status == "COMPLETED") {
                    OutlinedButton(
                        onClick = {
                            showCancellationDialog = txn
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Cancellation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Cancellation Request Reason Dialog
    if (showCancellationDialog != null) {
        val txn = showCancellationDialog!!
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCancellationDialog = null },
            title = {
                Text("Request Transaction Cancellation", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Transaction ${txn.transactionNumber} (${FormatUtils.formatCurrency(txn.totalAmount)})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Cashiers cannot delete transactions directly. Enter the reason so the owner can review and approve.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = { Text("e.g. Wrong quantity entered, customer returned goods") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reason.isNotBlank()) {
                            viewModel.requestCancellation(txn, reason) { res ->
                                if (res.isSuccess) {
                                    showCancellationDialog = null
                                    selectedTransaction = null
                                    Toast.makeText(context, "Cancellation request sent to owner", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to submit request", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = reason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancellationDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionRowCard(
    item: TransactionWithItems,
    onClick: () -> Unit
) {
    val txn = item.transaction
    val isCancelled = txn.status == "CANCELLED"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCancelled) Color(0xFFFFE4E6) else Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCancelled) Icons.Default.Close else Icons.Default.Receipt,
                        contentDescription = null,
                        tint = if (isCancelled) StatusError else StatusSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = txn.transactionNumber,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${FormatUtils.formatTime(txn.timestamp)} • Cashier: ${txn.cashierName} • ${item.items.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCurrency(txn.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = if (isCancelled) "CANCELLED" else txn.paymentMethod.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCancelled) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
