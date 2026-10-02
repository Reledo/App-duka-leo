package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DukaLeoDatabase
import com.example.data.DukaLeoRepository
import com.example.model.CancellationRequest
import com.example.model.CartItem
import com.example.model.Expense
import com.example.model.InventoryMovement
import com.example.model.NotificationItem
import com.example.model.Product
import com.example.model.Shop
import com.example.model.Transaction
import com.example.model.TransactionWithItems
import com.example.model.User
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

sealed class ActiveRole {
    object Owner : ActiveRole()
    data class Cashier(val user: User) : ActiveRole()
}

data class DashboardMetrics(
    val todaySales: Double = 0.0,
    val todayGrossProfit: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val todayTransactionsCount: Int = 0,
    val totalStockValue: Double = 0.0,
    val salesGrowthPercent: Double = 0.0,
    val topSellingProducts: List<Pair<String, Int>> = emptyList(),
    val lowStockCount: Int = 0
)

data class TrendPoint(
    val label: String,
    val amount: Double
)

class DukaLeoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DukaLeoRepository
    init {
        val database = DukaLeoDatabase.getDatabase(application, viewModelScope)
        repository = DukaLeoRepository(database.dukaLeoDao())
    }

    // Role state
    private val _currentRole = MutableStateFlow<ActiveRole>(ActiveRole.Owner)
    val currentRole: StateFlow<ActiveRole> = _currentRole.asStateFlow()

    fun switchToOwner() {
        _currentRole.value = ActiveRole.Owner
    }

    fun switchToCashier(user: User) {
        _currentRole.value = ActiveRole.Cashier(user)
    }

    // Active Shop
    val shop: StateFlow<Shop?> = repository.shop.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateShop(shop: Shop) {
        viewModelScope.launch { repository.updateShop(shop) }
    }

    // Users / Cashiers
    val allUsers: StateFlow<List<User>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeCashiers: StateFlow<List<User>> = repository.activeCashiers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createCashier(name: String, pin: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.createCashier(name, pin, phone)
            onDone()
        }
    }

    fun toggleCashierStatus(user: User) {
        viewModelScope.launch {
            repository.updateUser(user.copy(isActive = !user.isActive))
        }
    }

    // Products
    val allProducts: StateFlow<List<Product>> = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Product Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedStockFilter = MutableStateFlow("All") // "All", "Healthy", "Low Stock", "Out of Stock"

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        searchQuery,
        selectedCategory,
        selectedStockFilter
    ) { products, query, category, stockFilter ->
        products.filter { p ->
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || p.category.equals(category, ignoreCase = true)

            val matchesStock = when (stockFilter) {
                "Low Stock" -> p.stockQuantity in 1..p.lowStockThreshold
                "Out of Stock" -> p.stockQuantity <= 0
                "Healthy" -> p.stockQuantity > p.lowStockThreshold
                else -> true
            }

            matchesQuery && matchesCategory && matchesStock
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addProduct(
        name: String,
        category: String,
        buyingPrice: Double,
        sellingPrice: Double,
        stock: Int,
        threshold: Int,
        sku: String,
        barcode: String,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.addProduct(
                name = name,
                category = category,
                buyingPrice = buyingPrice,
                sellingPrice = sellingPrice,
                initialStock = stock,
                lowStockThreshold = threshold,
                sku = sku,
                barcode = barcode,
                performedBy = when (val role = _currentRole.value) {
                    is ActiveRole.Cashier -> role.user.name
                    else -> "Baraka Mrema (Owner)"
                }
            )
            onResult(res)
        }
    }

    fun updateProduct(product: Product, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateProduct(
                product = product,
                performedBy = when (val role = _currentRole.value) {
                    is ActiveRole.Cashier -> role.user.name
                    else -> "Baraka Mrema (Owner)"
                }
            )
            onResult(res)
        }
    }

    fun addStock(productId: Long, quantity: Int, reason: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.addStock(
                productId = productId,
                quantityToAdd = quantity,
                reason = reason,
                performedBy = when (val role = _currentRole.value) {
                    is ActiveRole.Cashier -> role.user.name
                    else -> "Baraka Mrema (Owner)"
                }
            )
            onResult(res)
        }
    }

    // Inventory Movements
    val inventoryMovements: StateFlow<List<InventoryMovement>> = repository.inventoryMovements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Transactions
    val allTransactions: StateFlow<List<TransactionWithItems>> = repository.allTransactionsWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Expenses
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addExpense(description: String, category: String, amount: Double, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addExpense(
                description = description,
                category = category,
                amount = amount,
                note = note,
                recordedBy = when (val role = _currentRole.value) {
                    is ActiveRole.Cashier -> role.user.name
                    else -> "Baraka Mrema (Owner)"
                }
            )
            onDone()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    // Cancellation Requests
    val pendingCancellations: StateFlow<List<CancellationRequest>> = repository.pendingCancellationRequests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCancellations: StateFlow<List<CancellationRequest>> = repository.allCancellationRequests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun requestCancellation(
        transaction: Transaction,
        reason: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val (cashierId, cashierName) = when (val role = _currentRole.value) {
                is ActiveRole.Cashier -> Pair(role.user.id, role.user.name)
                else -> Pair(1L, "Baraka Mrema (Owner)")
            }
            val res = repository.requestCancellation(
                transactionId = transaction.id,
                transactionNumber = transaction.transactionNumber,
                cashierId = cashierId,
                cashierName = cashierName,
                totalAmount = transaction.totalAmount,
                reason = reason
            )
            onResult(res)
        }
    }

    fun approveCancellation(request: CancellationRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.approveCancellation(request)
            onDone()
        }
    }

    fun rejectCancellation(request: CancellationRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.rejectCancellation(request)
            onDone()
        }
    }

    // Notifications
    val notifications: StateFlow<List<NotificationItem>> = repository.notifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadNotifications: StateFlow<List<NotificationItem>> = repository.unreadNotifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun markNotificationsAsRead() {
        viewModelScope.launch { repository.markAllNotificationsAsRead() }
    }

    // Cart (POS)
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    fun addToCart(product: Product, quantityToAdd: Int = 1) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + quantityToAdd
            if (newQty <= product.stockQuantity) {
                current[index] = item.copy(quantity = newQty)
                _cart.value = current
            }
        } else {
            if (quantityToAdd <= product.stockQuantity) {
                current.add(CartItem(product = product, quantity = quantityToAdd))
                _cart.value = current
            }
        }
    }

    fun updateCartQuantity(productId: Long, newQuantity: Int) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                val maxStock = current[index].product.stockQuantity
                val validQty = minOf(newQuantity, maxStock)
                current[index] = current[index].copy(quantity = validQty)
            }
            _cart.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    // Single-turn Sale completion debounce
    private var isSubmittingSale = false

    fun completeSale(paymentMethod: String, onResult: (Result<Transaction>) -> Unit) {
        if (isSubmittingSale) return
        isSubmittingSale = true
        viewModelScope.launch {
            try {
                val (cashierId, cashierName) = when (val role = _currentRole.value) {
                    is ActiveRole.Cashier -> Pair(role.user.id, role.user.name)
                    else -> Pair(1L, "Baraka Mrema (Owner)")
                }
                val res = repository.completeSale(
                    cashierId = cashierId,
                    cashierName = cashierName,
                    paymentMethod = paymentMethod,
                    cartItems = _cart.value
                )
                if (res.isSuccess) {
                    _cart.value = emptyList()
                }
                onResult(res)
            } finally {
                isSubmittingSale = false
            }
        }
    }

    // Dashboard Calculations
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allTransactions,
        allExpenses,
        allProducts
    ) { txns, expenses, products ->
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYesterday = startOfToday - (24L * 3600 * 1000)

        val completedTxns = txns.map { it.transaction }.filter { it.status == "COMPLETED" }

        val todayTxns = completedTxns.filter { it.timestamp >= startOfToday }
        val yesterdayTxns = completedTxns.filter { it.timestamp in startOfYesterday until startOfToday }

        val todaySales = todayTxns.sumOf { it.totalAmount }
        val yesterdaySales = yesterdayTxns.sumOf { it.totalAmount }
        val todayGrossProfit = todayTxns.sumOf { it.grossProfit }

        val todayExpensesTotal = expenses.filter { it.date >= startOfToday }.sumOf { it.amount }
        val todayNetProfit = todayGrossProfit - todayExpensesTotal

        val stockValue = products.sumOf { it.stockQuantity * it.sellingPrice }

        val growth = if (yesterdaySales > 0) {
            ((todaySales - yesterdaySales) / yesterdaySales) * 100
        } else if (todaySales > 0) 100.0 else 0.0

        // Product ranking (from all today's items)
        val productSalesMap = mutableMapOf<String, Int>()
        txns.filter { it.transaction.status == "COMPLETED" }.forEach { t ->
            t.items.forEach { item ->
                productSalesMap[item.productName] = (productSalesMap[item.productName] ?: 0) + item.quantity
            }
        }
        val topProducts = productSalesMap.toList().sortedByDescending { it.second }.take(5)
        val lowStockCount = products.count { it.stockQuantity <= it.lowStockThreshold }

        DashboardMetrics(
            todaySales = todaySales,
            todayGrossProfit = todayGrossProfit,
            todayExpenses = todayExpensesTotal,
            todayNetProfit = todayNetProfit,
            todayTransactionsCount = todayTxns.size,
            totalStockValue = stockValue,
            salesGrowthPercent = growth,
            topSellingProducts = topProducts,
            lowStockCount = lowStockCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMetrics()
    )

    // Sales Trend Chart Points
    val trendPeriod = MutableStateFlow("7 Days") // "Today", "7 Days", "30 Days"

    fun getSalesTrendPoints(period: String, txns: List<TransactionWithItems>): List<TrendPoint> {
        val completed = txns.map { it.transaction }.filter { it.status == "COMPLETED" }
        val now = Calendar.getInstance()

        return when (period) {
            "Today" -> {
                // Breakdown into 4 time blocks: Morning (06-11), Noon (12-14), Afternoon (15-18), Evening (19-23)
                val blocks = listOf(
                    "06-11" to (6..11),
                    "12-14" to (12..14),
                    "15-18" to (15..18),
                    "19-23" to (19..23)
                )
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val todaySales = completed.filter { it.timestamp >= startOfToday }
                blocks.map { (label, range) ->
                    val cal = Calendar.getInstance()
                    val total = todaySales.filter {
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.HOUR_OF_DAY) in range
                    }.sumOf { it.totalAmount }
                    TrendPoint(label, total)
                }
            }
            "30 Days" -> {
                // 4 weeks
                (3 downTo 0).map { weekOffset ->
                    val label = "Wk ${4 - weekOffset}"
                    val weekStart = now.timeInMillis - ((weekOffset + 1) * 7L * 24 * 3600 * 1000)
                    val weekEnd = now.timeInMillis - (weekOffset * 7L * 24 * 3600 * 1000)
                    val sum = completed.filter { it.timestamp in weekStart..weekEnd }.sumOf { it.totalAmount }
                    TrendPoint(label, sum)
                }
            }
            else -> {
                // Last 7 days
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                (6 downTo 0).map { dayOffset ->
                    val dayCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -dayOffset)
                    }
                    val dayStart = dayCal.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = dayStart + (24L * 3600 * 1000)
                    val dayName = when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> "Mon"
                        Calendar.TUESDAY -> "Tue"
                        Calendar.WEDNESDAY -> "Wed"
                        Calendar.THURSDAY -> "Thu"
                        Calendar.FRIDAY -> "Fri"
                        Calendar.SATURDAY -> "Sat"
                        else -> "Sun"
                    }
                    val sum = completed.filter { it.timestamp in dayStart until dayEnd }.sumOf { it.totalAmount }
                    TrendPoint(dayName, sum)
                }
            }
        }
    }

    // PDF Report Generator
    fun generatePdfReport(context: Context, period: String): File {
        val currentShop = shop.value ?: Shop(
            name = "Kariakoo Smart Duka",
            ownerName = "Baraka Mrema",
            phone = "+255 754 123 456"
        )
        val txns = allTransactions.value
        val expenses = allExpenses.value

        val (startTime, periodTitle) = when (period) {
            "Today" -> {
                val start = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                Pair(start, "Today")
            }
            "Last 7 Days" -> {
                val start = System.currentTimeMillis() - (7L * 24 * 3600 * 1000)
                Pair(start, "Last 7 Days")
            }
            else -> {
                val start = System.currentTimeMillis() - (30L * 24 * 3600 * 1000)
                Pair(start, "Last 30 Days")
            }
        }

        val completed = txns.filter { it.transaction.status == "COMPLETED" && it.transaction.timestamp >= startTime }
        val filteredExpenses = expenses.filter { it.date >= startTime }

        val totalSales = completed.sumOf { it.transaction.totalAmount }
        val totalCost = completed.sumOf { it.transaction.totalCost }
        val grossProfit = completed.sumOf { it.transaction.grossProfit }
        val totalExpenses = filteredExpenses.sumOf { it.amount }
        val estimatedNet = grossProfit - totalExpenses

        val productMap = mutableMapOf<String, Int>()
        completed.forEach { t ->
            t.items.forEach { item ->
                productMap[item.productName] = (productMap[item.productName] ?: 0) + item.quantity
            }
        }
        val topProducts = productMap.toList().sortedByDescending { it.second }.take(5)

        val cashierMap = mutableMapOf<String, Double>()
        completed.forEach { t ->
            cashierMap[t.transaction.cashierName] = (cashierMap[t.transaction.cashierName] ?: 0.0) + t.transaction.totalAmount
        }

        val reportData = PdfReportGenerator.ReportData(
            shop = currentShop,
            periodTitle = periodTitle,
            totalSales = totalSales,
            totalCost = totalCost,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            estimatedNetProfit = estimatedNet,
            transactionCount = completed.size,
            topProducts = topProducts,
            expenses = filteredExpenses,
            cashierBreakdown = cashierMap.toList()
        )

        return PdfReportGenerator.generateReportPdf(context, reportData)
    }
}
