package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Product
import com.example.model.User
import com.example.ui.components.DukaLeoTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class OwnerTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    POS("POS", Icons.Default.PointOfSale),
    PRODUCTS("Products", Icons.Default.Inventory2),
    SALES("Sales", Icons.Default.ReceiptLong),
    MORE("More", Icons.Default.Menu)
}

enum class CashierTab(val title: String, val icon: ImageVector) {
    POS("New Sale", Icons.Default.PointOfSale),
    SALES("My Sales", Icons.Default.ReceiptLong),
    PRODUCTS("Products", Icons.Default.Search)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DukaLeoApp(viewModel: DukaLeoViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val shop by viewModel.shop.collectAsState()
    val unreadNotifications by viewModel.unreadNotifications.collectAsState()
    val pendingCancellations by viewModel.pendingCancellations.collectAsState()

    var selectedOwnerTab by remember { mutableStateOf(OwnerTab.HOME) }
    var selectedCashierTab by remember { mutableStateOf(CashierTab.POS) }

    // Sub-screens under "More" or triggered by dashboard
    var activeSubScreen by remember { mutableStateOf<String?>(null) } // "REPORTS", "CASHIERS", "CANCELLATIONS", "NOTIFICATIONS", "SETTINGS", "EXPENSES"

    // Dialogs
    var showSwitchRoleDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 650.dp

        if (isExpanded) {
            // ==========================================
            // TABLET & DESKTOP RESPONSIVE LAYOUT
            // Left Navigation Sidebar + Full Content Area
            // ==========================================
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar Navigation
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .width(250.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        // Brand & Shop Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "DukaLeo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shop?.name ?: "DukaLeo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Smart Retail POS",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Role Status Card & Switcher
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentRole is ActiveRole.Owner) EmeraldPrimaryLight else AmberSecondaryLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSwitchRoleDialog = true }
                                .testTag("sidebar_role_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (currentRole is ActiveRole.Owner) Icons.Default.AdminPanelSettings else Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = if (currentRole is ActiveRole.Owner) EmeraldPrimary else AmberSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (currentRole is ActiveRole.Owner) "Owner Mode" else "Cashier Mode",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (currentRole is ActiveRole.Owner) EmeraldPrimary else AmberSecondary
                                        )
                                        Text(
                                            text = if (currentRole is ActiveRole.Owner) (shop?.ownerName ?: "Baraka Mrema") else (currentRole as ActiveRole.Cashier).user.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = "Switch Role",
                                    tint = if (currentRole is ActiveRole.Owner) EmeraldPrimary else AmberSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Navigation Items
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (currentRole is ActiveRole.Owner) {
                                SidebarNavItem(
                                    icon = Icons.Default.Dashboard,
                                    label = "Dashboard",
                                    isSelected = activeSubScreen == null && selectedOwnerTab == OwnerTab.HOME,
                                    onClick = {
                                        activeSubScreen = null
                                        selectedOwnerTab = OwnerTab.HOME
                                    }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.PointOfSale,
                                    label = "POS Terminal",
                                    isSelected = activeSubScreen == null && selectedOwnerTab == OwnerTab.POS,
                                    onClick = {
                                        activeSubScreen = null
                                        selectedOwnerTab = OwnerTab.POS
                                    }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.Inventory2,
                                    label = "Products & Stock",
                                    isSelected = activeSubScreen == null && selectedOwnerTab == OwnerTab.PRODUCTS,
                                    onClick = {
                                        activeSubScreen = null
                                        selectedOwnerTab = OwnerTab.PRODUCTS
                                    }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.ReceiptLong,
                                    label = "Sales History",
                                    isSelected = activeSubScreen == null && selectedOwnerTab == OwnerTab.SALES,
                                    onClick = {
                                        activeSubScreen = null
                                        selectedOwnerTab = OwnerTab.SALES
                                    }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.Assessment,
                                    label = "Reports & PDF",
                                    isSelected = activeSubScreen == "REPORTS",
                                    onClick = { activeSubScreen = "REPORTS" }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.MoneyOff,
                                    label = "Expenses",
                                    isSelected = activeSubScreen == "EXPENSES",
                                    onClick = { activeSubScreen = "EXPENSES" }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.Group,
                                    label = "Cashiers",
                                    isSelected = activeSubScreen == "CASHIERS",
                                    onClick = { activeSubScreen = "CASHIERS" }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.SwapCalls,
                                    label = "Cancellations",
                                    badgeCount = pendingCancellations.size,
                                    isSelected = activeSubScreen == "CANCELLATIONS",
                                    onClick = { activeSubScreen = "CANCELLATIONS" }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.Notifications,
                                    label = "Notifications",
                                    badgeCount = unreadNotifications.size,
                                    isSelected = activeSubScreen == "NOTIFICATIONS",
                                    onClick = { activeSubScreen = "NOTIFICATIONS" }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.Settings,
                                    label = "Shop Settings",
                                    isSelected = activeSubScreen == "SETTINGS",
                                    onClick = { activeSubScreen = "SETTINGS" }
                                )
                            } else {
                                // Cashier items
                                SidebarNavItem(
                                    icon = Icons.Default.PointOfSale,
                                    label = "POS / New Sale",
                                    isSelected = selectedCashierTab == CashierTab.POS,
                                    onClick = { selectedCashierTab = CashierTab.POS }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.ReceiptLong,
                                    label = "My Sales",
                                    isSelected = selectedCashierTab == CashierTab.SALES,
                                    onClick = { selectedCashierTab = CashierTab.SALES }
                                )
                                SidebarNavItem(
                                    icon = Icons.Default.Search,
                                    label = "Check Products",
                                    isSelected = selectedCashierTab == CashierTab.PRODUCTS,
                                    onClick = { selectedCashierTab = CashierTab.PRODUCTS }
                                )
                            }
                        }

                        // Bottom shop tag
                        Text(
                            text = "${shop?.location ?: "Dar es Salaam"} • TSh (TZS)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Main Content View (takes remaining width)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    RenderActiveScreen(
                        currentRole = currentRole,
                        selectedOwnerTab = selectedOwnerTab,
                        selectedCashierTab = selectedCashierTab,
                        activeSubScreen = activeSubScreen,
                        viewModel = viewModel,
                        onNavigateBack = { activeSubScreen = null },
                        onNavigateToPos = { selectedOwnerTab = OwnerTab.POS },
                        onNavigateToProducts = { selectedOwnerTab = OwnerTab.PRODUCTS },
                        onNavigateToExpenses = { activeSubScreen = "EXPENSES" },
                        onNavigateToReports = { activeSubScreen = "REPORTS" },
                        onNavigateToCashiers = { activeSubScreen = "CASHIERS" },
                        onNavigateToCancellations = { activeSubScreen = "CANCELLATIONS" },
                        onNavigateToNotifications = { activeSubScreen = "NOTIFICATIONS" },
                        onNavigateToSettings = { activeSubScreen = "SETTINGS" },
                        onOpenAddProduct = { showAddProductDialog = true },
                        onOpenAddExpense = { showAddExpenseDialog = true },
                        onOpenRestock = { productToRestock = it },
                        onSwitchRole = { showSwitchRoleDialog = true }
                    )
                }
            }
        } else {
            // ==========================================
            // MOBILE RESPONSIVE LAYOUT
            // 100% viewport width and height
            // Single Top Bar, Standard Bottom Navigation
            // ==========================================
            val showTopBar = activeSubScreen == null && currentRole is ActiveRole.Owner &&
                    (selectedOwnerTab == OwnerTab.HOME || selectedOwnerTab == OwnerTab.MORE)

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (showTopBar) {
                        DukaLeoTopBar(
                            title = shop?.name ?: "DukaLeo",
                            subtitle = if (currentRole is ActiveRole.Owner) "Tanzania Smart POS & Shop Manager" else "Cashier POS Terminal",
                            roleTitle = if (currentRole is ActiveRole.Owner) "Owner" else "Cashier",
                            unreadNotificationsCount = unreadNotifications.size,
                            onNotificationsClick = { activeSubScreen = "NOTIFICATIONS" },
                            onSwitchRoleClick = { showSwitchRoleDialog = true }
                        )
                    }
                },
                bottomBar = {
                    if (activeSubScreen == null) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            if (currentRole is ActiveRole.Owner) {
                                OwnerTab.values().forEach { tab ->
                                    val isSelected = selectedOwnerTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { selectedOwnerTab = tab },
                                        icon = {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        },
                                        label = { Text(tab.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            } else {
                                CashierTab.values().forEach { tab ->
                                    val isSelected = selectedCashierTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { selectedCashierTab = tab },
                                        icon = {
                                            Icon(tab.icon, contentDescription = tab.title)
                                        },
                                        label = { Text(tab.title) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("cashier_nav_tab_${tab.name.lowercase()}")
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
                ) {
                    RenderActiveScreen(
                        currentRole = currentRole,
                        selectedOwnerTab = selectedOwnerTab,
                        selectedCashierTab = selectedCashierTab,
                        activeSubScreen = activeSubScreen,
                        viewModel = viewModel,
                        onNavigateBack = { activeSubScreen = null },
                        onNavigateToPos = { selectedOwnerTab = OwnerTab.POS },
                        onNavigateToProducts = { selectedOwnerTab = OwnerTab.PRODUCTS },
                        onNavigateToExpenses = { activeSubScreen = "EXPENSES" },
                        onNavigateToReports = { activeSubScreen = "REPORTS" },
                        onNavigateToCashiers = { activeSubScreen = "CASHIERS" },
                        onNavigateToCancellations = { activeSubScreen = "CANCELLATIONS" },
                        onNavigateToNotifications = { activeSubScreen = "NOTIFICATIONS" },
                        onNavigateToSettings = { activeSubScreen = "SETTINGS" },
                        onOpenAddProduct = { showAddProductDialog = true },
                        onOpenAddExpense = { showAddExpenseDialog = true },
                        onOpenRestock = { productToRestock = it },
                        onSwitchRole = { showSwitchRoleDialog = true }
                    )
                }
            }
        }
    }

    // Role Switch Dialog
    if (showSwitchRoleDialog) {
        val users by viewModel.allUsers.collectAsState()
        SwitchRoleDialog(
            currentRole = currentRole,
            users = users,
            onDismiss = { showSwitchRoleDialog = false },
            onSelectRole = { role ->
                when (role) {
                    is ActiveRole.Owner -> viewModel.switchToOwner()
                    is ActiveRole.Cashier -> viewModel.switchToCashier(role.user)
                }
                showSwitchRoleDialog = false
                activeSubScreen = null
                Toast.makeText(context, "Switched role successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Product Modal
    if (showAddProductDialog) {
        AddEditProductDialog(
            product = null,
            onDismiss = { showAddProductDialog = false },
            onSave = { name, cat, buy, sell, stock, thresh, sku, barcode ->
                viewModel.addProduct(name, cat, buy, sell, stock, thresh, sku, barcode) { res ->
                    if (res.isSuccess) {
                        showAddProductDialog = false
                        Toast.makeText(context, "Product saved", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Add Expense Modal
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { desc, cat, amount, note ->
                viewModel.addExpense(desc, cat, amount, note) {
                    showAddExpenseDialog = false
                    Toast.makeText(context, "Expense recorded", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Restock Modal
    if (productToRestock != null) {
        AddStockDialog(
            product = productToRestock!!,
            onDismiss = { productToRestock = null },
            onAddStock = { qty, reason ->
                viewModel.addStock(productToRestock!!.id, qty, reason) { res ->
                    if (res.isSuccess) {
                        productToRestock = null
                        Toast.makeText(context, "Stock updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            if (badgeCount > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("$badgeCount", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun RenderActiveScreen(
    currentRole: ActiveRole,
    selectedOwnerTab: OwnerTab,
    selectedCashierTab: CashierTab,
    activeSubScreen: String?,
    viewModel: DukaLeoViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPos: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCashiers: () -> Unit,
    onNavigateToCancellations: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenAddProduct: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenRestock: (Product) -> Unit,
    onSwitchRole: () -> Unit
) {
    when (activeSubScreen) {
        "REPORTS" -> ReportsScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        "CASHIERS" -> CashierManagementScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        "CANCELLATIONS" -> CancellationRequestsScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        "NOTIFICATIONS" -> NotificationsScreen(viewModel = viewModel, onNavigateBack = onNavigateBack, onNavigateToCancellations = onNavigateToCancellations)
        "SETTINGS" -> ShopSettingsScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        "EXPENSES" -> ExpensesScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        else -> {
            if (currentRole is ActiveRole.Owner) {
                when (selectedOwnerTab) {
                    OwnerTab.HOME -> OwnerDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPos = onNavigateToPos,
                        onNavigateToProducts = onNavigateToProducts,
                        onNavigateToExpenses = onNavigateToExpenses,
                        onNavigateToReports = onNavigateToReports,
                        onNavigateToCancellations = onNavigateToCancellations,
                        onOpenAddProduct = onOpenAddProduct,
                        onOpenAddExpense = onOpenAddExpense,
                        onOpenRestock = onOpenRestock
                    )
                    OwnerTab.POS -> CashierPosScreen(viewModel = viewModel)
                    OwnerTab.PRODUCTS -> ProductsScreen(viewModel = viewModel)
                    OwnerTab.SALES -> SalesHistoryScreen(viewModel = viewModel)
                    OwnerTab.MORE -> MoreMenuScreen(
                        onNavigateToReports = onNavigateToReports,
                        onNavigateToExpenses = onNavigateToExpenses,
                        onNavigateToCashiers = onNavigateToCashiers,
                        onNavigateToCancellations = onNavigateToCancellations,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToSettings = onNavigateToSettings,
                        onSwitchRole = onSwitchRole
                    )
                }
            } else {
                // Cashier Interface
                when (selectedCashierTab) {
                    CashierTab.POS -> CashierPosScreen(viewModel = viewModel)
                    CashierTab.SALES -> SalesHistoryScreen(viewModel = viewModel)
                    CashierTab.PRODUCTS -> ProductsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MoreMenuScreen(
    onNavigateToReports: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToCashiers: () -> Unit,
    onNavigateToCancellations: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSwitchRole: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_menu_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Shop Management & Tools",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.Assessment,
                iconColor = EmeraldPrimary,
                title = "Financial Reports & PDF Export",
                subtitle = "Revenue, profits, margins, and downloadable PDF reports",
                onClick = onNavigateToReports
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.MoneyOff,
                iconColor = StatusError,
                title = "Expense Tracking",
                subtitle = "Manage overhead costs (rent, electricity, transport)",
                onClick = onNavigateToExpenses
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.Group,
                iconColor = StatusInfo,
                title = "Cashier Accounts & Permissions",
                subtitle = "Manage POS cashiers, PINs, and view sales performance",
                onClick = onNavigateToCashiers
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.SwapCalls,
                iconColor = AmberSecondary,
                title = "Cancellation Requests",
                subtitle = "Review and approve/reject transaction cancellations",
                onClick = onNavigateToCancellations
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.Notifications,
                iconColor = EmeraldPrimary,
                title = "Notification Center",
                subtitle = "Low stock alerts, daily summaries, and notifications",
                onClick = onNavigateToNotifications
            )
        }

        item {
            MoreMenuItem(
                icon = Icons.Default.Settings,
                iconColor = Color.DarkGray,
                title = "Shop Settings & Profile",
                subtitle = "Shop name, currency (TSh), location, and multi-shop branch info",
                onClick = onNavigateToSettings
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchRole() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Switch Active Role / Account", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Toggle between Owner dashboard and Cashier POS terminals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SwitchRoleDialog(
    currentRole: ActiveRole,
    users: List<User>,
    onDismiss: () -> Unit,
    onSelectRole: (ActiveRole) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Switch Role / Operator", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select who is currently operating DukaLeo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Owner Option
                val isOwnerSelected = currentRole is ActiveRole.Owner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOwnerSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isOwnerSelected) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldPrimary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRole(ActiveRole.Owner) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Baraka Mrema (Owner)", fontWeight = FontWeight.Bold)
                            Text("Full access: dashboard, profits, reports, inventory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isOwnerSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary)
                        }
                    }
                }

                // Cashiers
                users.filter { it.role == "CASHIER" && it.isActive }.forEach { cashier ->
                    val isCashierSelected = (currentRole as? ActiveRole.Cashier)?.user?.id == cashier.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCashierSelected) AmberSecondaryLight else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isCashierSelected) androidx.compose.foundation.BorderStroke(1.5.dp, AmberSecondary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRole(ActiveRole.Cashier(cashier)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, tint = AmberSecondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${cashier.name} (Cashier)", fontWeight = FontWeight.Bold)
                                Text("Fast POS interface, limited to sales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isCashierSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AmberSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
