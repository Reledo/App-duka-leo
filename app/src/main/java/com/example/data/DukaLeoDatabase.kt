package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.CancellationRequest
import com.example.model.Expense
import com.example.model.InventoryMovement
import com.example.model.NotificationItem
import com.example.model.Product
import com.example.model.Shop
import com.example.model.Transaction
import com.example.model.TransactionItem
import com.example.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Shop::class,
        User::class,
        Product::class,
        InventoryMovement::class,
        Transaction::class,
        TransactionItem::class,
        Expense::class,
        CancellationRequest::class,
        NotificationItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DukaLeoDatabase : RoomDatabase() {

    abstract fun dukaLeoDao(): DukaLeoDao

    companion object {
        @Volatile
        private var INSTANCE: DukaLeoDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DukaLeoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DukaLeoDatabase::class.java,
                    "dukaleo_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DukaLeoDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DukaLeoDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.dukaLeoDao())
                }
            }
        }
    }
}

suspend fun populateInitialData(dao: DukaLeoDao) {
    // 1. Initial Shop
    val shopId = dao.insertShop(
        Shop(
            id = 1,
            name = "Kariakoo Smart Duka",
            ownerName = "Baraka Mrema",
            phone = "+255 754 123 456",
            shopType = "Retail Grocery & Provisions",
            location = "Kariakoo, Dar es Salaam",
            currency = "TSh",
            createdAt = System.currentTimeMillis() - (30L * 24 * 3600 * 1000)
        )
    )

    // 2. Users (Owner & Cashiers)
    val ownerId = dao.insertUser(
        User(
            id = 1,
            shopId = shopId,
            name = "Baraka Mrema (Owner)",
            role = "OWNER",
            pin = "0000",
            phone = "+255 754 123 456",
            isActive = true
        )
    )

    val johnId = dao.insertUser(
        User(
            id = 2,
            shopId = shopId,
            name = "John Mwangi",
            role = "CASHIER",
            pin = "1234",
            phone = "+255 712 345 678",
            isActive = true
        )
    )

    val ashaId = dao.insertUser(
        User(
            id = 3,
            shopId = shopId,
            name = "Asha Juma",
            role = "CASHIER",
            pin = "5678",
            phone = "+255 784 987 654",
            isActive = true
        )
    )

    // 3. Products
    val products = listOf(
        Product(1, shopId, "Mayai (Tray/Pieces)", "Food", 300.0, 500.0, 96, 15, "EGG-01", "6164000101"),
        Product(2, shopId, "Mkate Bakhresa", "Food", 750.0, 1000.0, 42, 10, "BRD-01", "6164000102"),
        Product(3, shopId, "Soda (Coca-Cola 350ml)", "Drinks", 700.0, 1000.0, 68, 12, "SDA-01", "6164000103"),
        Product(4, shopId, "Maji Kilimanjaro 500ml", "Drinks", 300.0, 500.0, 120, 20, "WTR-01", "6164000104"),
        Product(5, shopId, "Sukari 1kg (Kilombero)", "Food", 2500.0, 3000.0, 28, 10, "SGR-01", "6164000105"),
        Product(6, shopId, "Mafuta Korie 1L", "Food", 3200.0, 4000.0, 18, 8, "OIL-01", "6164000106"),
        Product(7, shopId, "Sabuni Sunlight 1kg", "Household", 1800.0, 2500.0, 35, 10, "SP-01", "6164000107"),
        Product(8, shopId, "Mchele Kyela 1kg", "Food", 2400.0, 3000.0, 55, 15, "RC-01", "6164000108"),
        Product(9, shopId, "Chai Bora (Tea Bags)", "Food", 1500.0, 2200.0, 8, 10, "TEA-01", "6164000109"),
        Product(10, shopId, "Colgate Herbal 100ml", "Personal care", 2000.0, 2800.0, 6, 8, "TP-01", "6164000110")
    )

    for (p in products) {
        dao.insertProduct(p)
        dao.insertInventoryMovement(
            InventoryMovement(
                shopId = shopId,
                productId = p.id,
                productName = p.name,
                changeQuantity = p.stockQuantity,
                previousStock = 0,
                newStock = p.stockQuantity,
                reason = "INITIAL_STOCK",
                performedBy = "Baraka Mrema (Owner)",
                timestamp = System.currentTimeMillis() - (7L * 24 * 3600 * 1000)
            )
        )
    }

    val now = System.currentTimeMillis()

    // 4. Sample Transactions
    // Today's transaction 1: Cashier John
    val t1Id = dao.insertTransaction(
        Transaction(
            transactionNumber = "TXN-10480",
            shopId = shopId,
            cashierId = johnId,
            cashierName = "John Mwangi",
            totalAmount = 7500.0,
            totalCost = 5050.0,
            grossProfit = 2450.0,
            paymentMethod = "CASH",
            status = "COMPLETED",
            timestamp = now - (3 * 3600 * 1000)
        )
    )
    dao.insertTransactionItems(
        listOf(
            TransactionItem(0, t1Id, 1, "Mayai (Tray/Pieces)", 5, 500.0, 300.0, 2500.0),
            TransactionItem(0, t1Id, 2, "Mkate Bakhresa", 2, 1000.0, 750.0, 2000.0),
            TransactionItem(0, t1Id, 3, "Soda (Coca-Cola 350ml)", 3, 1000.0, 700.0, 3000.0)
        )
    )

    // Today's transaction 2: Cashier Asha
    val t2Id = dao.insertTransaction(
        Transaction(
            transactionNumber = "TXN-10481",
            shopId = shopId,
            cashierId = ashaId,
            cashierName = "Asha Juma",
            totalAmount = 14500.0,
            totalCost = 11400.0,
            grossProfit = 3100.0,
            paymentMethod = "MOBILE_MONEY",
            status = "COMPLETED",
            timestamp = now - (2 * 3600 * 1000)
        )
    )
    dao.insertTransactionItems(
        listOf(
            TransactionItem(0, t2Id, 5, "Sukari 1kg (Kilombero)", 2, 3000.0, 2500.0, 6000.0),
            TransactionItem(0, t2Id, 6, "Mafuta Korie 1L", 1, 4000.0, 3200.0, 4000.0),
            TransactionItem(0, t2Id, 7, "Sabuni Sunlight 1kg", 1, 2500.0, 1800.0, 2500.0),
            TransactionItem(0, t2Id, 4, "Maji Kilimanjaro 500ml", 4, 500.0, 300.0, 2000.0)
        )
    )

    // Today's transaction 3: Cashier John (with a pending cancellation request demo)
    val t3Id = dao.insertTransaction(
        Transaction(
            transactionNumber = "TXN-10482",
            shopId = shopId,
            cashierId = johnId,
            cashierName = "John Mwangi",
            totalAmount = 2500.0,
            totalCost = 1500.0,
            grossProfit = 1000.0,
            paymentMethod = "CASH",
            status = "COMPLETED",
            timestamp = now - (45 * 60 * 1000)
        )
    )
    dao.insertTransactionItems(
        listOf(
            TransactionItem(0, t3Id, 1, "Mayai (Tray/Pieces)", 5, 500.0, 300.0, 2500.0)
        )
    )

    // Yesterday's transaction 4: Asha
    val t4Id = dao.insertTransaction(
        Transaction(
            transactionNumber = "TXN-10475",
            shopId = shopId,
            cashierId = ashaId,
            cashierName = "Asha Juma",
            totalAmount = 35000.0,
            totalCost = 28200.0,
            grossProfit = 6800.0,
            paymentMethod = "MOBILE_MONEY",
            status = "COMPLETED",
            timestamp = now - (26 * 3600 * 1000)
        )
    )
    dao.insertTransactionItems(
        listOf(
            TransactionItem(0, t4Id, 8, "Mchele Kyela 1kg", 5, 3000.0, 2400.0, 15000.0),
            TransactionItem(0, t4Id, 6, "Mafuta Korie 1L", 3, 4000.0, 3200.0, 12000.0),
            TransactionItem(0, t4Id, 3, "Soda (Coca-Cola 350ml)", 8, 1000.0, 700.0, 8000.0)
        )
    )

    // 5. Sample Pending Cancellation Request (For demonstration)
    dao.insertCancellationRequest(
        CancellationRequest(
            transactionId = t3Id,
            transactionNumber = "TXN-10482",
            shopId = shopId,
            cashierId = johnId,
            cashierName = "John Mwangi",
            totalAmount = 2500.0,
            reason = "Customer wanted 3 eggs instead of 5, wrong quantity entered.",
            status = "PENDING",
            requestedAt = now - (30 * 60 * 1000)
        )
    )

    // 6. Sample Expenses
    dao.insertExpense(
        Expense(
            shopId = shopId,
            description = "Electricity token (LUKU)",
            category = "Electricity",
            amount = 15000.0,
            date = now - (4 * 3600 * 1000),
            note = "Tokens for shop refrigerator & lighting",
            recordedBy = "Baraka Mrema"
        )
    )
    dao.insertExpense(
        Expense(
            shopId = shopId,
            description = "Bodaboda transport for bread supply",
            category = "Transport",
            amount = 8000.0,
            date = now - (5 * 3600 * 1000),
            note = "Delivery from bakery to Kariakoo",
            recordedBy = "Baraka Mrema"
        )
    )
    dao.insertExpense(
        Expense(
            shopId = shopId,
            description = "Plastic shopping bags & packaging",
            category = "Packaging",
            amount = 12000.0,
            date = now - (2 * 24 * 3600 * 1000),
            note = "Eco-friendly carrier bags bundle",
            recordedBy = "Baraka Mrema"
        )
    )

    // 7. Notifications
    dao.insertNotification(
        NotificationItem(
            shopId = shopId,
            title = "⚠️ Low Stock Alert",
            message = "Chai Bora (Tea Bags) has only 8 units left (threshold is 10).",
            type = "LOW_STOCK",
            timestamp = now - (2 * 3600 * 1000),
            isRead = false
        )
    )
    dao.insertNotification(
        NotificationItem(
            shopId = shopId,
            title = "⚠️ Low Stock Alert",
            message = "Colgate Herbal 100ml has only 6 units left (threshold is 8).",
            type = "LOW_STOCK",
            timestamp = now - (1 * 3600 * 1000),
            isRead = false
        )
    )
    dao.insertNotification(
        NotificationItem(
            shopId = shopId,
            title = "🔄 Cancellation Request",
            message = "John Mwangi requested cancellation for TXN-10482 (TSh 2,500).",
            type = "CANCELLATION",
            timestamp = now - (30 * 60 * 1000),
            isRead = false
        )
    )
}
