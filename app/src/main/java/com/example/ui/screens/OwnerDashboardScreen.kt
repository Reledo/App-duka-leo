package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.ui.DukaLeoViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.FormatUtils

@Composable
fun OwnerDashboardScreen(
    viewModel: DukaLeoViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCancellations: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenRestock: (Product) -> Unit
) {
    val shop by viewModel.shop.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val trendPeriod by viewModel.trendPeriod.collectAsState()
    val allTxns by viewModel.allTransactions.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val pendingCancellations by viewModel.pendingCancellations.collectAsState()

    val trendPoints = remember(trendPeriod, allTxns) {
        viewModel.getSalesTrendPoints(trendPeriod, allTxns)
    }

    val greeting = remember { FormatUtils.getGreeting() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 700.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("owner_dashboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Welcome Greeting Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greeting.first,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${greeting.second} • ${shop?.name ?: "DukaLeo"}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }

                        // POS Launch Button
                        Button(
                            onClick = onNavigateToPos,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("dashboard_pos_button")
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open POS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Cancellation Request Alert Banner (If Any)
            if (pendingCancellations.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberSecondaryLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCancellations() }
                            .testTag("pending_cancellations_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AmberSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapCalls,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${pendingCancellations.size} Cancellation Request Pending",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = "Cashier ${pendingCancellations.first().cashierName} requested review for ${pendingCancellations.first().transactionNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }

            // 3. Quick Action Buttons Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.AddBox,
                        label = "+ Product",
                        onClick = onOpenAddProduct,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.ReceiptLong,
                        label = "+ Expense",
                        onClick = onOpenAddExpense,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        icon = Icons.Default.Assessment,
                        label = "Reports & PDF",
                        onClick = onNavigateToReports,
                        modifier = Modifier.weight(1f)
                    )
                    if (isTablet) {
                        QuickActionButton(
                            icon = Icons.Default.Inventory2,
                            label = "Manage Stock",
                            onClick = onNavigateToProducts,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Primary Metrics Grid (Adaptive: 3 columns on tablet, 2 on mobile)
            item {
                if (isTablet) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricStatCard(
                                title = "Today's Sales",
                                value = FormatUtils.formatCurrency(metrics.todaySales),
                                subtitle = if (metrics.salesGrowthPercent >= 0) "↑ ${String.format("%.0f", metrics.salesGrowthPercent)}% vs yesterday" else "↓ ${String.format("%.0f", -metrics.salesGrowthPercent)}% vs yesterday",
                                isPositive = metrics.salesGrowthPercent >= 0,
                                icon = Icons.Default.AttachMoney,
                                iconBgColor = EmeraldPrimaryLight,
                                iconTint = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Gross Profit",
                                value = FormatUtils.formatCurrency(metrics.todayGrossProfit),
                                subtitle = "${metrics.todayTransactionsCount} sales today",
                                icon = Icons.Default.TrendingUp,
                                iconBgColor = Color(0xFFD1FAE5),
                                iconTint = StatusSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Est. Net Profit",
                                value = FormatUtils.formatCurrency(metrics.todayNetProfit),
                                subtitle = "Gross − Expenses",
                                isPositive = metrics.todayNetProfit >= 0,
                                icon = Icons.Default.AccountBalanceWallet,
                                iconBgColor = if (metrics.todayNetProfit >= 0) EmeraldPrimaryLight else Color(0xFFFFE4E6),
                                iconTint = if (metrics.todayNetProfit >= 0) EmeraldPrimary else StatusError,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricStatCard(
                                title = "Today's Expenses",
                                value = FormatUtils.formatCurrency(metrics.todayExpenses),
                                subtitle = "Shop overhead costs",
                                icon = Icons.Default.MoneyOff,
                                iconBgColor = Color(0xFFFFE4E6),
                                iconTint = StatusError,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToExpenses
                            )
                            MetricStatCard(
                                title = "Total Stock Value",
                                value = FormatUtils.formatCurrency(metrics.totalStockValue),
                                subtitle = "Inventory retail value",
                                icon = Icons.Default.Inventory2,
                                iconBgColor = AmberSecondaryLight,
                                iconTint = AmberSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToProducts
                            )
                            MetricStatCard(
                                title = "Transactions",
                                value = "${metrics.todayTransactionsCount}",
                                subtitle = "Completed today",
                                icon = Icons.Default.ShoppingBag,
                                iconBgColor = Color(0xFFE0E7FF),
                                iconTint = StatusInfo,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Mobile 2-column rows
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricStatCard(
                                title = "Today's Sales",
                                value = FormatUtils.formatCurrency(metrics.todaySales),
                                subtitle = if (metrics.salesGrowthPercent >= 0) "↑ ${String.format("%.0f", metrics.salesGrowthPercent)}% vs yesterday" else "↓ ${String.format("%.0f", -metrics.salesGrowthPercent)}% vs yesterday",
                                isPositive = metrics.salesGrowthPercent >= 0,
                                icon = Icons.Default.AttachMoney,
                                iconBgColor = EmeraldPrimaryLight,
                                iconTint = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricStatCard(
                                title = "Gross Profit",
                                value = FormatUtils.formatCurrency(metrics.todayGrossProfit),
                                subtitle = "${metrics.todayTransactionsCount} sales today",
                                icon = Icons.Default.TrendingUp,
                                iconBgColor = Color(0xFFD1FAE5),
                                iconTint = StatusSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricStatCard(
                                title = "Today's Expenses",
                                value = FormatUtils.formatCurrency(metrics.todayExpenses),
                                subtitle = "Shop overhead",
                                icon = Icons.Default.MoneyOff,
                                iconBgColor = Color(0xFFFFE4E6),
                                iconTint = StatusError,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToExpenses
                            )
                            MetricStatCard(
                                title = "Est. Net Profit",
                                value = FormatUtils.formatCurrency(metrics.todayNetProfit),
                                subtitle = "Gross − Expenses",
                                isPositive = metrics.todayNetProfit >= 0,
                                icon = Icons.Default.AccountBalanceWallet,
                                iconBgColor = if (metrics.todayNetProfit >= 0) EmeraldPrimaryLight else Color(0xFFFFE4E6),
                                iconTint = if (metrics.todayNetProfit >= 0) EmeraldPrimary else StatusError,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MetricStatCard(
                                title = "Total Stock Value",
                                value = FormatUtils.formatCurrency(metrics.totalStockValue),
                                subtitle = "Inventory retail value",
                                icon = Icons.Default.Inventory2,
                                iconBgColor = AmberSecondaryLight,
                                iconTint = AmberSecondary,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToProducts
                            )
                            MetricStatCard(
                                title = "Transactions",
                                value = "${metrics.todayTransactionsCount}",
                                subtitle = "Completed today",
                                icon = Icons.Default.ShoppingBag,
                                iconBgColor = Color(0xFFE0E7FF),
                                iconTint = StatusInfo,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 5. Sales Trend Line Chart
            item {
                SalesTrendLineChart(
                    points = trendPoints,
                    selectedPeriod = trendPeriod,
                    onPeriodSelected = { viewModel.trendPeriod.value = it }
                )
            }

            // 6. Top Selling Products
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Best-Selling Products",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = onNavigateToProducts) {
                                Text("All Products")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (metrics.topSellingProducts.isEmpty()) {
                            Text(
                                text = "No sales recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val maxSold = metrics.topSellingProducts.firstOrNull()?.second ?: 1
                            metrics.topSellingProducts.forEachIndexed { index, pair ->
                                RankedProductItem(
                                    rank = index + 1,
                                    name = pair.first,
                                    unitsSold = pair.second,
                                    maxUnits = maxSold
                                )
                            }
                        }
                    }
                }
            }

            // 7. Low Stock Alerts Section
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = StatusWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Low Stock Alerts (${lowStockProducts.size})",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = onNavigateToProducts) {
                                    Text("View All")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            lowStockProducts.take(4).forEach { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${product.stockQuantity} remaining (alert at ${product.lowStockThreshold})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StatusWarning
                                        )
                                    }
                                    Button(
                                        onClick = { onOpenRestock(product) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldPrimaryLight,
                                            contentColor = EmeraldPrimary
                                        )
                                    ) {
                                        Text("+ Restock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
