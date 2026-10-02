package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.model.CartItem
import com.example.model.Product
import com.example.model.Transaction
import com.example.ui.ActiveRole
import com.example.ui.DukaLeoViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPosScreen(
    viewModel: DukaLeoViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentRole by viewModel.currentRole.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()

    var posSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCartSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastCompletedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }

    val categories = remember(allProducts) {
        listOf("All") + allProducts.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredPosProducts = remember(allProducts, posSearchQuery, selectedCategory) {
        allProducts.filter { p ->
            val matchesQuery = posSearchQuery.isBlank() ||
                    p.name.contains(posSearchQuery, ignoreCase = true) ||
                    p.sku.contains(posSearchQuery, ignoreCase = true) ||
                    p.barcode.contains(posSearchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }

    val cartTotal = remember(cart) { cart.sumOf { it.subtotal } }
    val cartItemCount = remember(cart) { cart.sumOf { it.quantity } }

    val cashierName = when (val role = currentRole) {
        is ActiveRole.Cashier -> role.user.name
        else -> "Baraka Mrema (Owner)"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 650.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                }
                            }
                            Column {
                                Text(
                                    text = "New Sale (POS)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cashier: $cashierName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Mobile Cart Badge
                        if (!isExpanded && cartItemCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showCartSheet = true }
                                    .testTag("open_cart_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$cartItemCount items • ${FormatUtils.formatCurrency(cartTotal)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Mobile Sticky Bottom Cart Bar
                if (!isExpanded && cartItemCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Cart Subtotal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(cartTotal),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { showCartSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("review_cart_button")
                            ) {
                                Text("Review Cart ($cartItemCount)", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            if (isExpanded) {
                // =====================================
                // TABLET / DESKTOP 2-PANE POS TERMINAL
                // Left (60%): Catalog | Right (40%): Live Checkout Panel
                // =====================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("cashier_pos_screen")
                ) {
                    // Left Pane: Catalog & Search
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        PosSearchAndFilters(
                            searchQuery = posSearchQuery,
                            onSearchQueryChange = { posSearchQuery = it },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = { selectedCategory = it }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredPosProducts.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.SearchOff,
                                title = "No products found",
                                message = "Try searching with a different name or category.",
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 180.dp),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredPosProducts) { product ->
                                    val cartItem = cart.find { it.product.id == product.id }
                                    val cartQty = cartItem?.quantity ?: 0
                                    PosProductCard(
                                        product = product,
                                        cartQty = cartQty,
                                        onAddToCart = {
                                            if (cartQty < product.stockQuantity) {
                                                viewModel.addToCart(product, 1)
                                            } else {
                                                Toast.makeText(context, "Only ${product.stockQuantity} in stock!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onUpdateQty = { newQty ->
                                            if (newQty <= product.stockQuantity) {
                                                viewModel.updateCartQuantity(product.id, newQty)
                                            } else {
                                                Toast.makeText(context, "Max stock reached", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Right Pane: Active Order / Cart & Checkout Sidebar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight()
                    ) {
                        PosCheckoutPanel(
                            cart = cart,
                            cartTotal = cartTotal,
                            cartItemCount = cartItemCount,
                            selectedPaymentMethod = selectedPaymentMethod,
                            onSelectPayment = { selectedPaymentMethod = it },
                            onClearCart = { viewModel.clearCart() },
                            onUpdateQty = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                            onCompleteSale = {
                                viewModel.completeSale(selectedPaymentMethod) { result ->
                                    if (result.isSuccess) {
                                        lastCompletedTransaction = result.getOrNull()
                                        showSuccessDialog = true
                                    } else {
                                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Sale failed", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // =====================================
                // MOBILE SINGLE-COLUMN POS
                // Search -> Filter Chips -> Products List
                // =====================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("cashier_pos_screen")
                ) {
                    PosSearchAndFilters(
                        searchQuery = posSearchQuery,
                        onSearchQueryChange = { posSearchQuery = it },
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { selectedCategory = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (filteredPosProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.SearchOff,
                            title = "No products found",
                            message = "Try searching with a different name or category.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 90.dp)
                        ) {
                            items(filteredPosProducts) { product ->
                                val cartItem = cart.find { it.product.id == product.id }
                                val cartQty = cartItem?.quantity ?: 0
                                PosProductRow(
                                    product = product,
                                    cartQty = cartQty,
                                    onAddToCart = {
                                        if (cartQty < product.stockQuantity) {
                                            viewModel.addToCart(product, 1)
                                        } else {
                                            Toast.makeText(context, "Only ${product.stockQuantity} available!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onUpdateQty = { newQty ->
                                        if (newQty <= product.stockQuantity) {
                                            viewModel.updateCartQuantity(product.id, newQty)
                                        } else {
                                            Toast.makeText(context, "Max stock reached", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Mobile Cart Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                PosCheckoutPanel(
                    cart = cart,
                    cartTotal = cartTotal,
                    cartItemCount = cartItemCount,
                    selectedPaymentMethod = selectedPaymentMethod,
                    onSelectPayment = { selectedPaymentMethod = it },
                    onClearCart = {
                        viewModel.clearCart()
                        showCartSheet = false
                    },
                    onUpdateQty = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                    onCompleteSale = {
                        viewModel.completeSale(selectedPaymentMethod) { result ->
                            if (result.isSuccess) {
                                lastCompletedTransaction = result.getOrNull()
                                showCartSheet = false
                                showSuccessDialog = true
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Sale failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // Sale Success Confirmation Dialog
    if (showSuccessDialog && lastCompletedTransaction != null) {
        val txn = lastCompletedTransaction!!
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Next Sale", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Sale Completed!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = FormatUtils.formatCurrency(txn.totalAmount),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${txn.transactionNumber} • ${txn.paymentMethod.replace("_", " ")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Stock was automatically updated and transaction is permanently recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        )
    }
}

@Composable
private fun PosSearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("🔍 Search product (e.g. Mayai, Soda, Mkate)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("pos_search_input")
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun PosProductRow(
    product: Product,
    cartQty: Int,
    onAddToCart: () -> Unit,
    onUpdateQty: (Int) -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cartQty > 0) EmeraldPrimaryLight else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) {
                if (cartQty == 0) onAddToCart() else onUpdateQty(cartQty + 1)
            }
            .testTag("pos_product_item_${product.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FormatUtils.formatCurrency(product.sellingPrice),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${product.stockQuantity} in stock",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stockQuantity <= product.lowStockThreshold) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cartQty > 0) {
                    IconButton(
                        onClick = { onUpdateQty(cartQty - 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = "$cartQty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { onUpdateQty(cartQty + 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Button(
                        onClick = onAddToCart,
                        enabled = !isOutOfStock,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isOutOfStock) {
                            Text("Out of Stock", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosProductCard(
    product: Product,
    cartQty: Int,
    onAddToCart: () -> Unit,
    onUpdateQty: (Int) -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cartQty > 0) EmeraldPrimaryLight else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) {
                if (cartQty == 0) onAddToCart() else onUpdateQty(cartQty + 1)
            }
            .testTag("pos_product_item_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (cartQty > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("$cartQty", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = FormatUtils.formatCurrency(product.sellingPrice),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${product.stockQuantity} in stock",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stockQuantity <= product.lowStockThreshold) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (cartQty > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onUpdateQty(cartQty - 1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onUpdateQty(cartQty + 1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    FilledTonalButton(
                        onClick = onAddToCart,
                        enabled = !isOutOfStock,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (isOutOfStock) "Out" else "+ Add", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCheckoutPanel(
    cart: List<CartItem>,
    cartTotal: Double,
    cartItemCount: Int,
    selectedPaymentMethod: String,
    onSelectPayment: (String) -> Unit,
    onClearCart: () -> Unit,
    onUpdateQty: (Long, Int) -> Unit,
    onCompleteSale: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order Ticket ($cartItemCount items)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (cart.isNotEmpty()) {
                TextButton(onClick = onClearCart) {
                    Text("Clear All", color = StatusError)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ticket is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap products on the left to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Cart Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.quantity} × ${FormatUtils.formatCurrency(item.product.sellingPrice)} = ${FormatUtils.formatCurrency(item.subtotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onUpdateQty(item.product.id, item.quantity - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = { onUpdateQty(item.product.id, item.quantity + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onUpdateQty(item.product.id, 0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))

        // Payment Method Selector
        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        val methods = listOf(
            "CASH" to "Cash (Pesa)",
            "M_PESA" to "M-Pesa",
            "AIRTEL_MONEY" to "Airtel",
            "TIGO_PESA" to "Tigo Pesa"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            methods.forEach { (key, label) ->
                val isSelected = selectedPaymentMethod == key
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPayment(key) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Total & Complete Sale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Payable",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = FormatUtils.formatCurrency(cartTotal),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCompleteSale,
            enabled = cart.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("complete_sale_submit_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete Sale", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
