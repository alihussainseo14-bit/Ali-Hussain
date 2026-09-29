package com.example.data

import kotlinx.coroutines.flow.Flow

class StoreRepository(private val storeDao: StoreDao) {

    val productsFlow: Flow<List<ProductEntity>> = storeDao.observeAllProducts()
    val cartItemsFlow: Flow<List<CartItemEntity>> = storeDao.observeCartItems()
    val ordersFlow: Flow<List<OrderEntity>> = storeDao.observeOrders()

    suspend fun ensureCatalogSeeded() {
        val count = storeDao.getProductCount()
        if (count == 0) {
            storeDao.insertProducts(InitialCatalogData.defaultProducts)
        }
    }

    suspend fun toggleFavorite(product: ProductEntity) {
        storeDao.updateFavoriteStatus(product.id, !product.isFavorite)
    }

    suspend fun addToCart(product: ProductEntity, selectedSize: String = "M", quantityToAdd: Int = 1) {
        val existing = storeDao.getCartItemByProductAndSize(product.id, selectedSize)
        if (existing != null) {
            storeDao.updateCartItem(existing.copy(quantity = existing.quantity + quantityToAdd))
        } else {
            storeDao.insertCartItem(
                CartItemEntity(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    category = product.category,
                    drawableKey = product.drawableKey,
                    selectedSize = selectedSize,
                    quantity = quantityToAdd
                )
            )
        }
    }

    suspend fun updateCartItemQuantity(item: CartItemEntity, newQuantity: Int) {
        if (newQuantity <= 0) {
            storeDao.deleteCartItemById(item.id)
        } else {
            storeDao.updateCartItem(item.copy(quantity = newQuantity))
        }
    }

    suspend fun removeCartItem(cartItemId: Int) {
        storeDao.deleteCartItemById(cartItemId)
    }

    suspend fun clearCart() {
        storeDao.clearCart()
    }

    suspend fun placeOrder(
        cartItems: List<CartItemEntity>,
        discountAmount: Double,
        shippingAddress: String,
        paymentMethod: String
    ): OrderEntity {
        val subtotal = cartItems.sumOf { it.lineTotal }
        val total = (subtotal - discountAmount).coerceAtLeast(0.0)
        val totalUnits = cartItems.sumOf { it.quantity }
        val summary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" }
        val orderNum = (100000..999999).random().toString()

        val order = OrderEntity(
            orderNumber = orderNum,
            itemsSummary = summary,
            totalItemsCount = totalUnits,
            subtotalAmount = subtotal,
            discountAmount = discountAmount,
            totalAmount = total,
            shippingAddress = shippingAddress,
            paymentMethod = paymentMethod
        )
        storeDao.insertOrder(order)
        storeDao.clearCart()
        return order
    }
}
