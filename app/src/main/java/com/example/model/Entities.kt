package com.example.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ownerName: String,
    val phone: String,
    val shopType: String = "Retail / General Store",
    val location: String = "Dar es Salaam, Tanzania",
    val currency: String = "TSh",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["shopId"])]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long = 1,
    val name: String,
    val role: String, // "OWNER" or "CASHIER"
    val pin: String = "1234",
    val phone: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["shopId"]), Index(value = ["name"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long = 1,
    val name: String,
    val category: String = "General",
    val buyingPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 10,
    val sku: String = "",
    val barcode: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val profitPerUnit: Double get() = sellingPrice - buyingPrice
    val marginPercentage: Double get() = if (sellingPrice > 0) ((sellingPrice - buyingPrice) / sellingPrice) * 100 else 0.0
}

@Entity(
    tableName = "inventory_movements",
    indices = [Index(value = ["shopId"]), Index(value = ["productId"])]
)
data class InventoryMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long = 1,
    val productId: Long,
    val productName: String,
    val changeQuantity: Int, // positive for addition/cancellation return, negative for sale
    val previousStock: Int,
    val newStock: Int,
    val reason: String, // "INITIAL_STOCK", "RESTOCK", "SALE", "CANCELLATION_RETURN", "ADJUSTMENT"
    val performedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["shopId"]), Index(value = ["cashierId"]), Index(value = ["timestamp"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionNumber: String,
    val shopId: Long = 1,
    val cashierId: Long,
    val cashierName: String,
    val totalAmount: Double,
    val totalCost: Double,
    val grossProfit: Double,
    val paymentMethod: String, // "CASH", "MOBILE_MONEY", "OTHER"
    val status: String = "COMPLETED", // "COMPLETED", "CANCELLED"
    val cancellationReason: String? = null,
    val requestedBy: String? = null,
    val approvedBy: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"]), Index(value = ["productId"])]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val sellingPrice: Double, // Locked at time of sale
    val buyingPrice: Double,  // Locked at time of sale
    val subtotal: Double
)

data class TransactionWithItems(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItem>
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["shopId"]), Index(value = ["date"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long = 1,
    val description: String,
    val category: String, // "Rent", "Electricity", "Transport", "Supplies", "Employee", "Packaging", "Repairs", "Other"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val recordedBy: String = "Owner"
)

@Entity(
    tableName = "cancellation_requests",
    indices = [Index(value = ["shopId"]), Index(value = ["transactionId"])]
)
data class CancellationRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val transactionNumber: String,
    val shopId: Long = 1,
    val cashierId: Long,
    val cashierName: String,
    val totalAmount: Double,
    val reason: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewerName: String? = null
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["shopId"]), Index(value = ["isRead"])]
)
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long = 1,
    val title: String,
    val message: String,
    val type: String, // "LOW_STOCK", "CANCELLATION", "REPORT", "INFO"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class CartItem(
    val product: Product,
    var quantity: Int
) {
    val subtotal: Double get() = product.sellingPrice * quantity
}
