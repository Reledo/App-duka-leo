package com.example.data

import com.example.model.CancellationRequest
import com.example.model.CartItem
import com.example.model.Expense
import com.example.model.InventoryMovement
import com.example.model.NotificationItem
import com.example.model.Product
import com.example.model.Shop
import com.example.model.Transaction
import com.example.model.TransactionItem
import com.example.model.TransactionWithItems
import com.example.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DukaLeoRepository(private val dao: DukaLeoDao) {

    // Shop
    val shop: Flow<Shop?> = dao.getShop(1)
    suspend fun getShopSync(): Shop? = withContext(Dispatchers.IO) { dao.getShopSync(1) }
    suspend fun updateShop(shop: Shop) = withContext(Dispatchers.IO) { dao.updateShop(shop) }

    // Users
    val allUsers: Flow<List<User>> = dao.getAllUsers(1)
    val activeCashiers: Flow<List<User>> = dao.getActiveCashiers(1)

    suspend fun createCashier(name: String, pin: String, phone: String): Long = withContext(Dispatchers.IO) {
        val user = User(
            shopId = 1,
            name = name.trim(),
            role = "CASHIER",
            pin = if (pin.isNotBlank()) pin else "1234",
            phone = phone.trim(),
            isActive = true
        )
        dao.insertUser(user)
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        dao.updateUser(user)
    }

    // Products
    val allProducts: Flow<List<Product>> = dao.getAllProducts(1)
    val lowStockProducts: Flow<List<Product>> = dao.getLowStockProducts(1)

    fun searchProducts(query: String): Flow<List<Product>> = dao.searchProducts(query, 1)

    suspend fun addProduct(
        name: String,
        category: String,
        buyingPrice: Double,
        sellingPrice: Double,
        initialStock: Int,
        lowStockThreshold: Int,
        sku: String = "",
        barcode: String = "",
        performedBy: String = "Owner"
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val product = Product(
                shopId = 1,
                name = name.trim(),
                category = if (category.isNotBlank()) category.trim() else "General",
                buyingPrice = buyingPrice,
                sellingPrice = sellingPrice,
                stockQuantity = initialStock,
                lowStockThreshold = if (lowStockThreshold > 0) lowStockThreshold else 5,
                sku = sku.trim(),
                barcode = barcode.trim()
            )
            val productId = dao.insertProduct(product)
            if (initialStock > 0) {
                dao.insertInventoryMovement(
                    InventoryMovement(
                        shopId = 1,
                        productId = productId,
                        productName = product.name,
                        changeQuantity = initialStock,
                        previousStock = 0,
                        newStock = initialStock,
                        reason = "INITIAL_STOCK",
                        performedBy = performedBy
                    )
                )
            }
            Result.success(productId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product, performedBy: String = "Owner"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val oldProduct = dao.getProductById(product.id)
            dao.updateProduct(product)
            if (oldProduct != null && oldProduct.stockQuantity != product.stockQuantity) {
                val diff = product.stockQuantity - oldProduct.stockQuantity
                dao.insertInventoryMovement(
                    InventoryMovement(
                        shopId = product.shopId,
                        productId = product.id,
                        productName = product.name,
                        changeQuantity = diff,
                        previousStock = oldProduct.stockQuantity,
                        newStock = product.stockQuantity,
                        reason = "INVENTORY_ADJUSTMENT",
                        performedBy = performedBy
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addStock(
        productId: Long,
        quantityToAdd: Int,
        reason: String = "RESTOCK",
        performedBy: String = "Owner"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val product = dao.getProductById(productId)
                ?: return@withContext Result.failure(IllegalArgumentException("Product not found"))
            val prevStock = product.stockQuantity
            val newStock = prevStock + quantityToAdd
            dao.updateProduct(product.copy(stockQuantity = newStock, updatedAt = System.currentTimeMillis()))
            dao.insertInventoryMovement(
                InventoryMovement(
                    shopId = product.shopId,
                    productId = product.id,
                    productName = product.name,
                    changeQuantity = quantityToAdd,
                    previousStock = prevStock,
                    newStock = newStock,
                    reason = reason,
                    performedBy = performedBy
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Inventory Movements
    val inventoryMovements: Flow<List<InventoryMovement>> = dao.getAllInventoryMovements(1)

    // Transactions
    val allTransactionsWithItems: Flow<List<TransactionWithItems>> = dao.getAllTransactionsWithItems(1)

    suspend fun completeSale(
        cashierId: Long,
        cashierName: String,
        paymentMethod: String,
        cartItems: List<CartItem>
    ): Result<Transaction> = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Cart is empty"))
        }

        // 1. Stock Validation
        for (item in cartItems) {
            val freshProduct = dao.getProductById(item.product.id)
            if (freshProduct == null) {
                return@withContext Result.failure(IllegalStateException("Product ${item.product.name} not found"))
            }
            if (freshProduct.stockQuantity < item.quantity) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "Only ${freshProduct.stockQuantity} units available for '${freshProduct.name}' (requested ${item.quantity})."
                    )
                )
            }
        }

        // 2. Calculations
        var totalAmount = 0.0
        var totalCost = 0.0
        val itemsToInsert = mutableListOf<TransactionItem>()

        for (item in cartItems) {
            val fresh = dao.getProductById(item.product.id)!!
            val lineTotal = fresh.sellingPrice * item.quantity
            val lineCost = fresh.buyingPrice * item.quantity
            totalAmount += lineTotal
            totalCost += lineCost
        }
        val grossProfit = totalAmount - totalCost

        val txnNumber = "TXN-" + SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())

        val transaction = Transaction(
            transactionNumber = txnNumber,
            shopId = 1,
            cashierId = cashierId,
            cashierName = cashierName,
            totalAmount = totalAmount,
            totalCost = totalCost,
            grossProfit = grossProfit,
            paymentMethod = paymentMethod,
            status = "COMPLETED",
            timestamp = System.currentTimeMillis()
        )

        val txnId = dao.insertTransaction(transaction)

        // 3. Insert items and update stock
        for (item in cartItems) {
            val fresh = dao.getProductById(item.product.id)!!
            itemsToInsert.add(
                TransactionItem(
                    transactionId = txnId,
                    productId = fresh.id,
                    productName = fresh.name,
                    quantity = item.quantity,
                    sellingPrice = fresh.sellingPrice,
                    buyingPrice = fresh.buyingPrice,
                    subtotal = fresh.sellingPrice * item.quantity
                )
            )

            // Reduce stock
            val prevStock = fresh.stockQuantity
            val newStock = prevStock - item.quantity
            dao.updateProduct(fresh.copy(stockQuantity = newStock, updatedAt = System.currentTimeMillis()))

            // Log movement
            dao.insertInventoryMovement(
                InventoryMovement(
                    shopId = 1,
                    productId = fresh.id,
                    productName = fresh.name,
                    changeQuantity = -item.quantity,
                    previousStock = prevStock,
                    newStock = newStock,
                    reason = "SALE ($txnNumber)",
                    performedBy = cashierName
                )
            )

            // Check low stock
            if (newStock <= fresh.lowStockThreshold) {
                dao.insertNotification(
                    NotificationItem(
                        shopId = 1,
                        title = "⚠️ Low Stock Alert",
                        message = "${fresh.name} has only $newStock units remaining (threshold: ${fresh.lowStockThreshold}).",
                        type = "LOW_STOCK"
                    )
                )
            }
        }

        dao.insertTransactionItems(itemsToInsert)

        Result.success(transaction.copy(id = txnId))
    }

    // Cancellation Requests
    val pendingCancellationRequests: Flow<List<CancellationRequest>> = dao.getPendingCancellationRequests(1)
    val allCancellationRequests: Flow<List<CancellationRequest>> = dao.getAllCancellationRequests(1)

    suspend fun requestCancellation(
        transactionId: Long,
        transactionNumber: String,
        cashierId: Long,
        cashierName: String,
        totalAmount: Double,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = CancellationRequest(
                transactionId = transactionId,
                transactionNumber = transactionNumber,
                shopId = 1,
                cashierId = cashierId,
                cashierName = cashierName,
                totalAmount = totalAmount,
                reason = reason.trim(),
                status = "PENDING"
            )
            dao.insertCancellationRequest(request)

            // Notify owner
            dao.insertNotification(
                NotificationItem(
                    shopId = 1,
                    title = "🔄 Cancellation Request",
                    message = "$cashierName requested cancellation of $transactionNumber: \"$reason\"",
                    type = "CANCELLATION"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reviewCancellationRequest(
        requestId: Long,
        approve: Boolean,
        reviewerName: String = "Owner"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requests = dao.getAllCancellationRequests(1)
            // Query specific request
            val reqList = dao.getAllCancellationRequests(1)
            // We can retrieve the request or find in list
            val request = dao.getPendingCancellationRequests(1)
            // Let's do a direct look up from the transaction
            val allReqs = mutableListOf<CancellationRequest>()
            // We can update directly:
            val currentRequests = dao.getAllCancellationRequests(1)
            // To be precise:
            if (approve) {
                // Find request and transaction
                val txnWithItems = dao.getTransactionWithItemsById(requestId) // fallback or pass txnId
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveCancellation(
        request: CancellationRequest,
        reviewerName: String = "Baraka Mrema (Owner)"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Update request status
            dao.updateCancellationRequest(
                request.copy(
                    status = "APPROVED",
                    reviewedAt = System.currentTimeMillis(),
                    reviewerName = reviewerName
                )
            )

            // 2. Load transaction and items
            val txnWithItems = dao.getTransactionWithItemsById(request.transactionId)
            if (txnWithItems != null && txnWithItems.transaction.status != "CANCELLED") {
                // Update transaction
                dao.updateTransaction(
                    txnWithItems.transaction.copy(
                        status = "CANCELLED",
                        cancellationReason = request.reason,
                        requestedBy = request.cashierName,
                        approvedBy = reviewerName
                    )
                )

                // 3. Restore inventory for all items
                for (item in txnWithItems.items) {
                    val product = dao.getProductById(item.productId)
                    if (product != null) {
                        val prevStock = product.stockQuantity
                        val newStock = prevStock + item.quantity
                        dao.updateProduct(
                            product.copy(
                                stockQuantity = newStock,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        dao.insertInventoryMovement(
                            InventoryMovement(
                                shopId = 1,
                                productId = product.id,
                                productName = product.name,
                                changeQuantity = item.quantity,
                                previousStock = prevStock,
                                newStock = newStock,
                                reason = "CANCELLATION_RETURN (${request.transactionNumber})",
                                performedBy = reviewerName
                            )
                        )
                    }
                }

                // 4. Create confirmation notification
                dao.insertNotification(
                    NotificationItem(
                        shopId = 1,
                        title = "✅ Cancellation Approved",
                        message = "${request.transactionNumber} was cancelled and stock has been restored.",
                        type = "INFO"
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectCancellation(
        request: CancellationRequest,
        reviewerName: String = "Baraka Mrema (Owner)"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateCancellationRequest(
                request.copy(
                    status = "REJECTED",
                    reviewedAt = System.currentTimeMillis(),
                    reviewerName = reviewerName
                )
            )
            dao.insertNotification(
                NotificationItem(
                    shopId = 1,
                    title = "❌ Cancellation Rejected",
                    message = "Cancellation for ${request.transactionNumber} was rejected.",
                    type = "INFO"
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Expenses
    val allExpenses: Flow<List<Expense>> = dao.getAllExpenses(1)

    suspend fun addExpense(
        description: String,
        category: String,
        amount: Double,
        note: String = "",
        recordedBy: String = "Owner"
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val expense = Expense(
                shopId = 1,
                description = description.trim(),
                category = category.trim(),
                amount = amount,
                note = note.trim(),
                recordedBy = recordedBy
            )
            val id = dao.insertExpense(expense)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        dao.deleteExpense(expense)
    }

    // Notifications
    val notifications: Flow<List<NotificationItem>> = dao.getAllNotifications(1)
    val unreadNotifications: Flow<List<NotificationItem>> = dao.getUnreadNotifications(1)

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        dao.markAllNotificationsAsRead(1)
    }
}
