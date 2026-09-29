package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.R

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val price: Double,
    val originalPrice: Double,
    val category: String,
    val description: String,
    val rating: Float,
    val reviewCount: Int,
    val drawableKey: String,
    val isFavorite: Boolean = false,
    val isOnSale: Boolean = true
) {
    val discountPercentage: Int
        get() = if (originalPrice > price && originalPrice > 0) {
            (((originalPrice - price) / originalPrice) * 100).toInt()
        } else {
            0
        }

    fun resolveDrawableRes(): Int = resolveProductDrawable(drawableKey)
}

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val name: String,
    val price: Double,
    val category: String,
    val drawableKey: String,
    val selectedSize: String = "M",
    val quantity: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
) {
    val lineTotal: Double
        get() = price * quantity

    fun resolveDrawableRes(): Int = resolveProductDrawable(drawableKey)
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderNumber: String,
    val itemsSummary: String,
    val totalItemsCount: Int,
    val subtotalAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val shippingAddress: String,
    val paymentMethod: String,
    val status: String = "Confirmed",
    val createdAt: Long = System.currentTimeMillis()
)

fun resolveProductDrawable(drawableKey: String): Int {
    return when (drawableKey) {
        "tshirt" -> R.drawable.img_product_tshirt
        "shoes" -> R.drawable.img_product_shoes
        "watch" -> R.drawable.img_product_watch
        "bag" -> R.drawable.img_product_bag
        else -> R.drawable.img_product_tshirt
    }
}

fun formatPriceRs(price: Double): String {
    return "Rs. ${price.toLong()}"
}

object InitialCatalogData {
    val defaultProducts = listOf(
        ProductEntity(
            id = 1,
            name = "Premium T-Shirt",
            price = 2499.0,
            originalPrice = 3499.0,
            category = "Clothes",
            description = "Crafted from 100% combed Pima cotton (240 GSM) with a tailored modern silhouette, breathable weave, and pre-shrunk durability for everyday luxury.",
            rating = 4.8f,
            reviewCount = 142,
            drawableKey = "tshirt",
            isFavorite = false,
            isOnSale = true
        ),
        ProductEntity(
            id = 2,
            name = "Running Shoes",
            price = 5999.0,
            originalPrice = 8999.0,
            category = "Sports",
            description = "Engineered mesh upper with dual-density nitrogen-infused foam midsole and high-traction carbon rubber outsole for responsive road and track running.",
            rating = 4.9f,
            reviewCount = 218,
            drawableKey = "shoes",
            isFavorite = false,
            isOnSale = true
        ),
        ProductEntity(
            id = 3,
            name = "Classic Watch",
            price = 7999.0,
            originalPrice = 12999.0,
            category = "Watches",
            description = "Sapphire crystal glass over a deep sunray dial, precision quartz chronograph movement, 5 ATM water resistance, and hand-stitched Italian calfskin strap.",
            rating = 4.9f,
            reviewCount = 96,
            drawableKey = "watch",
            isFavorite = false,
            isOnSale = true
        ),
        ProductEntity(
            id = 4,
            name = "Leather Bag",
            price = 4499.0,
            originalPrice = 6999.0,
            category = "Bags",
            description = "Full-grain vegetable-tanned cognac leather tote with padded 15-inch laptop sleeve, solid brushed brass hardware, and reinforced shoulder straps.",
            rating = 4.7f,
            reviewCount = 84,
            drawableKey = "bag",
            isFavorite = false,
            isOnSale = true
        ),
        ProductEntity(
            id = 5,
            name = "Merino Crewneck Tee",
            price = 2999.0,
            originalPrice = 3999.0,
            category = "Clothes",
            description = "Ultra-fine Australian merino wool blend tee offering natural temperature regulation, odor resistance, and a silky drape for travel or layering.",
            rating = 4.6f,
            reviewCount = 63,
            drawableKey = "tshirt",
            isFavorite = false,
            isOnSale = false
        ),
        ProductEntity(
            id = 6,
            name = "Velocity Trail Trainers",
            price = 6499.0,
            originalPrice = 8499.0,
            category = "Sports",
            description = "All-terrain athletic trainers with reinforced toe guard, adaptive arch support, and multidirectional grip lugs for high-intensity training.",
            rating = 4.8f,
            reviewCount = 115,
            drawableKey = "shoes",
            isFavorite = false,
            isOnSale = true
        ),
        ProductEntity(
            id = 7,
            name = "Minimalist Dress Watch",
            price = 6999.0,
            originalPrice = 8999.0,
            category = "Watches",
            description = "Ultra-slim 7mm brushed surgical steel case paired with luminous baton indices and a quick-release genuine leather strap.",
            rating = 4.7f,
            reviewCount = 71,
            drawableKey = "watch",
            isFavorite = false,
            isOnSale = false
        ),
        ProductEntity(
            id = 8,
            name = "Executive Messenger Bag",
            price = 5299.0,
            originalPrice = 7499.0,
            category = "Bags",
            description = "Structured heritage leather satchel with magnetic quick-access closures, organizer pockets, and detachable crossbody strap.",
            rating = 4.8f,
            reviewCount = 52,
            drawableKey = "bag",
            isFavorite = false,
            isOnSale = true
        )
    )
}
