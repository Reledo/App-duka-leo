package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.example.model.CancellationRequest
import com.example.ui.DukaLeoViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancellationRequestsScreen(
    viewModel: DukaLeoViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allRequests by viewModel.allCancellations.collectAsState()
    var selectedFilter by remember { mutableStateOf("PENDING") } // "PENDING", "APPROVED", "REJECTED", "ALL"

    val filtered = remember(allRequests, selectedFilter) {
        if (selectedFilter == "ALL") allRequests else allRequests.filter { it.status == selectedFilter }
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
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                text = "Cancellation Requests",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val pendingCount = allRequests.count { it.status == "PENDING" }
                        if (pendingCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AmberSecondaryLight
                            ) {
                                Text(
                                    text = "$pendingCount Pending",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AmberSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("PENDING" to "Pending", "APPROVED" to "Approved", "REJECTED" to "Rejected", "ALL" to "All").forEach { (key, label) ->
                            val isSel = key == selectedFilter
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedFilter = key },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (key == "PENDING") AmberSecondary else MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
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
                .testTag("cancellation_requests_screen")
        ) {
            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.CheckCircleOutline,
                    title = "No ${selectedFilter.lowercase()} requests",
                    message = "When cashiers request a sale cancellation with a valid reason, they will appear here for your review.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered) { request ->
                        CancellationRequestCard(
                            request = request,
                            onApprove = {
                                viewModel.approveCancellation(request) {
                                    Toast.makeText(context, "${request.transactionNumber} cancelled & stock restored!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onReject = {
                                viewModel.rejectCancellation(request) {
                                    Toast.makeText(context, "${request.transactionNumber} cancellation rejected", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CancellationRequestCard(
    request: CancellationRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isPending = request.status == "PENDING"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (request.status) {
                                    "APPROVED" -> Color(0xFFD1FAE5)
                                    "REJECTED" -> Color(0xFFFFE4E6)
                                    else -> AmberSecondaryLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (request.status) {
                                "APPROVED" -> Icons.Default.Check
                                "REJECTED" -> Icons.Default.Close
                                else -> Icons.Default.HourglassTop
                            },
                            contentDescription = null,
                            tint = when (request.status) {
                                "APPROVED" -> StatusSuccess
                                "REJECTED" -> StatusError
                                else -> AmberSecondary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = request.transactionNumber,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Requested by: ${request.cashierName} • ${FormatUtils.formatDateTime(request.requestedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = FormatUtils.formatCurrency(request.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reason Quote Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Cashier's Reason:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${request.reason}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!isPending) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: ${request.status} by ${request.reviewerName ?: "Owner"} • ${if (request.reviewedAt != null) FormatUtils.formatDateTime(request.reviewedAt) else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (request.status == "APPROVED") StatusSuccess else StatusError
                )
            }

            // Action Buttons for Owner
            if (isPending) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                    ) {
                        Text("Reject")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve & Restore Stock")
                    }
                }
            }
        }
    }
}
