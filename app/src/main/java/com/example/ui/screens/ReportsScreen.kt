package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import com.example.ui.DukaLeoViewModel
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RankedProductItem
import com.example.ui.theme.*
import com.example.util.FormatUtils
import com.example.util.PdfReportGenerator
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: DukaLeoViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val allTxns by viewModel.allTransactions.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val shop by viewModel.shop.collectAsState()

    var selectedPeriod by remember { mutableStateOf("Last 7 Days") } // "Today", "Last 7 Days", "Last 30 Days"
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    // Period timestamp calculation
    val (periodStartTime, periodLabel) = remember(selectedPeriod) {
        val now = System.currentTimeMillis()
        when (selectedPeriod) {
            "Today" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, "Today")
            }
            "Last 7 Days" -> {
                Pair(now - (7L * 24 * 3600 * 1000), "Last 7 Days")
            }
            else -> {
                Pair(now - (30L * 24 * 3600 * 1000), "Last 30 Days")
            }
        }
    }

    // Filtered data for period
    val periodCompletedTxns = remember(allTxns, periodStartTime) {
        allTxns.filter { it.transaction.status == "COMPLETED" && it.transaction.timestamp >= periodStartTime }
    }
    val periodExpenses = remember(allExpenses, periodStartTime) {
        allExpenses.filter { it.date >= periodStartTime }
    }

    val totalRevenue = remember(periodCompletedTxns) { periodCompletedTxns.sumOf { it.transaction.totalAmount } }
    val totalCost = remember(periodCompletedTxns) { periodCompletedTxns.sumOf { it.transaction.totalCost } }
    val grossProfit = remember(periodCompletedTxns) { periodCompletedTxns.sumOf { it.transaction.grossProfit } }
    val totalExpAmount = remember(periodExpenses) { periodExpenses.sumOf { it.amount } }
    val estimatedNetProfit = grossProfit - totalExpAmount
    val transactionCount = periodCompletedTxns.size
    val averageBasket = if (transactionCount > 0) totalRevenue / transactionCount else 0.0

    // Top products
    val topProducts = remember(periodCompletedTxns) {
        val map = mutableMapOf<String, Int>()
        periodCompletedTxns.forEach { t ->
            t.items.forEach { line ->
                map[line.productName] = (map[line.productName] ?: 0) + line.quantity
            }
        }
        map.toList().sortedByDescending { it.second }.take(5)
    }

    // Cashier performance
    val cashierSales = remember(periodCompletedTxns) {
        val map = mutableMapOf<String, Pair<Int, Double>>() // Name -> Count, Amount
        periodCompletedTxns.forEach { t ->
            val prev = map[t.transaction.cashierName] ?: Pair(0, 0.0)
            map[t.transaction.cashierName] = Pair(prev.first + 1, prev.second + t.transaction.totalAmount)
        }
        map.toList().sortedByDescending { it.second.second }
    }

    // Payment methods
    val paymentBreakdown = remember(periodCompletedTxns) {
        val map = mutableMapOf<String, Double>()
        periodCompletedTxns.forEach { t ->
            val key = if (t.transaction.paymentMethod == "CASH") "Cash" else "Mobile Money"
            map[key] = (map[key] ?: 0.0) + t.transaction.totalAmount
        }
        map.toList()
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            text = "Financial Reports",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // PDF Export Button
                    Button(
                        onClick = {
                            isGeneratingPdf = true
                            try {
                                val file = viewModel.generatePdfReport(context, selectedPeriod)
                                generatedPdfFile = file
                                isGeneratingPdf = false
                                PdfReportGenerator.viewPdf(context, file)
                                Toast.makeText(context, "PDF Report generated!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                isGeneratingPdf = false
                                Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("reports_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    listOf("Today", "Last 7 Days", "Last 30 Days").forEach { period ->
                        val isSel = period == selectedPeriod
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedPeriod = period }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Executive Net Profit Banner
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimaryLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ESTIMATED NET PROFIT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = EmeraldPrimaryVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White
                            ) {
                                Text(
                                    text = periodLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = FormatUtils.formatCurrency(estimatedNetProfit),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gross Profit (${FormatUtils.formatCurrency(grossProfit)}) − Expenses (${FormatUtils.formatCurrency(totalExpAmount)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimaryVariant
                        )
                    }
                }
            }

            // 3. Key Financial Overview Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Total Sales Revenue",
                            value = FormatUtils.formatCurrency(totalRevenue),
                            subtitle = "$transactionCount sales recorded",
                            icon = Icons.Default.AttachMoney,
                            iconBgColor = EmeraldPrimaryLight,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Cost of Goods (COGS)",
                            value = FormatUtils.formatCurrency(totalCost),
                            subtitle = "Purchasing cost",
                            icon = Icons.Default.LocalShipping,
                            iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Gross Profit",
                            value = FormatUtils.formatCurrency(grossProfit),
                            subtitle = if (totalRevenue > 0) "${String.format("%.0f", (grossProfit / totalRevenue) * 100)}% gross margin" else "0% margin",
                            isPositive = grossProfit >= 0,
                            icon = Icons.Default.TrendingUp,
                            iconBgColor = Color(0xFFD1FAE5),
                            iconTint = StatusSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Avg. Basket Value",
                            value = FormatUtils.formatCurrency(averageBasket),
                            subtitle = "Per transaction",
                            icon = Icons.Default.ShoppingCart,
                            iconBgColor = Color(0xFFE0E7FF),
                            iconTint = StatusInfo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Top Selling Products in Period
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Top Products Sold ($periodLabel)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (topProducts.isEmpty()) {
                            Text("No product sales recorded in this period.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val max = topProducts.firstOrNull()?.second ?: 1
                            topProducts.forEachIndexed { i, p ->
                                RankedProductItem(rank = i + 1, name = p.first, unitsSold = p.second, maxUnits = max)
                            }
                        }
                    }
                }
            }

            // 5. Cashier Performance Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Cashier Performance ($periodLabel)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (cashierSales.isEmpty()) {
                            Text("No cashier sales recorded in this period.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            cashierSales.forEach { (name, stats) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name.take(1),
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("${stats.first} transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Text(
                                        text = FormatUtils.formatCurrency(stats.second),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Payment Methods Distribution
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payment Method Share",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (paymentBreakdown.isEmpty()) {
                            Text("No sales data available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            paymentBreakdown.forEach { (method, amount) ->
                                val pct = if (totalRevenue > 0) (amount / totalRevenue).toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(method, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text("${FormatUtils.formatCurrency(amount)} (${String.format("%.0f", pct * 100)}%)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (method == "Cash") StatusSuccess else StatusInfo,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
