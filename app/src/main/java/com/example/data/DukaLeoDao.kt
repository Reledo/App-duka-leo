package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.example.model.CancellationRequest
import com.example.model.Expense
import com.example.model.InventoryMovement
import com.example.model.NotificationItem
import com.example.model.Product
import com.example.model.Shop
import com.example.model.Transaction
import com.example.model.TransactionItem
import com.example.model.TransactionWithItems
import com.example.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface DukaLeoDao {

    // Shop
    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    fun getShop(shopId: Long = 1): Flow<Shop?>

    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    suspend fun getShopSync(shopId: Long = 1): Shop?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: Shop): Long

    @Update
    suspend fun updateShop(shop: Shop)

    // Users / Cashiers
    @Query("SELECT * FROM users WHERE shopId = :shopId ORDER BY role ASC, name ASC")
    fun getAllUsers(shopId: Long = 1): Flow<List<User>>

    @Query("SELECT * FROM users WHERE shopId = :shopId AND role = 'CASHIER' AND isActive = 1 ORDER BY name ASC")
    fun getActiveCashiers(shopId: Long = 1): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    // Products
    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY name ASC")
    fun getAllProducts(shopId: Long = 1): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE shopId = :shopId AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String, shopId: Long = 1): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): Product?

    @Query("SELECT * FROM products WHERE shopId = :shopId AND stockQuantity <= lowStockThreshold ORDER BY stockQuantity ASC")
    fun getLowStockProducts(shopId: Long = 1): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // Inventory Movements
    @Query("SELECT * FROM inventory_movements WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getAllInventoryMovements(shopId: Long = 1): Flow<List<InventoryMovement>>

    @Query("SELECT * FROM inventory_movements WHERE shopId = :shopId AND productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long, shopId: Long = 1): Flow<List<InventoryMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryMovement(movement: InventoryMovement): Long

    // Transactions
    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getAllTransactionsWithItems(shopId: Long = 1): Flow<List<TransactionWithItems>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE shopId = :shopId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsByDateRange(startTime: Long, endTime: Long, shopId: Long = 1): Flow<List<TransactionWithItems>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE shopId = :shopId AND cashierId = :cashierId ORDER BY timestamp DESC")
    fun getTransactionsByCashier(cashierId: Long, shopId: Long = 1): Flow<List<TransactionWithItems>>

    @RoomTransaction
    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionWithItemsById(transactionId: Long): TransactionWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>

    // Expenses
    @Query("SELECT * FROM expenses WHERE shopId = :shopId ORDER BY date DESC")
    fun getAllExpenses(shopId: Long = 1): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE shopId = :shopId AND date >= :startTime AND date <= :endTime ORDER BY date DESC")
    fun getExpensesByDateRange(startTime: Long, endTime: Long, shopId: Long = 1): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Cancellation Requests
    @Query("SELECT * FROM cancellation_requests WHERE shopId = :shopId ORDER BY requestedAt DESC")
    fun getAllCancellationRequests(shopId: Long = 1): Flow<List<CancellationRequest>>

    @Query("SELECT * FROM cancellation_requests WHERE shopId = :shopId AND status = 'PENDING' ORDER BY requestedAt DESC")
    fun getPendingCancellationRequests(shopId: Long = 1): Flow<List<CancellationRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCancellationRequest(request: CancellationRequest): Long

    @Update
    suspend fun updateCancellationRequest(request: CancellationRequest)

    // Notifications
    @Query("SELECT * FROM notifications WHERE shopId = :shopId ORDER BY timestamp DESC")
    fun getAllNotifications(shopId: Long = 1): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE shopId = :shopId AND isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(shopId: Long = 1): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE shopId = :shopId")
    suspend fun markAllNotificationsAsRead(shopId: Long = 1)
}
