package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AssistantRole
import com.example.data.CartItemEntity
import com.example.data.ChatMessage
import com.example.data.GeminiChatService
import com.example.data.GeminiModelOption
import com.example.data.InitialCatalogData
import com.example.data.OrderEntity
import com.example.data.ProductEntity
import com.example.data.StoreRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StoreScreen {
    HOME,
    CART,
    ORDERS,
    CHAT
}

enum class SortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    TOP_RATED("Top Rated")
}

data class StoreUiState(
    val currentScreen: StoreScreen = StoreScreen.HOME,
    val ownerName: String = "HUSSAIN",
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val showOnlyFavorites: Boolean = false,
    val showOnlySale: Boolean = false,
    val sortOption: SortOption = SortOption.FEATURED,
    val allProducts: List<ProductEntity> = InitialCatalogData.defaultProducts,
    val filteredProducts: List<ProductEntity> = InitialCatalogData.defaultProducts,
    val cartItems: List<CartItemEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val selectedProductForDetail: ProductEntity? = null,
    val isPromoApplied: Boolean = false
) {
    val totalCartUnits: Int
        get() = cartItems.sumOf { it.quantity }

    val cartSubtotal: Double
        get() = cartItems.sumOf { it.lineTotal }

    val promoDiscount: Double
        get() = if (isPromoApplied) (cartSubtotal * 0.10).toLong().toDouble() else 0.0

    val cartTotal: Double
        get() = (cartSubtotal - promoDiscount).coerceAtLeast(0.0)
}

private data class FilterParams(
    val query: String,
    val category: String?,
    val onlyFavorites: Boolean,
    val onlySale: Boolean,
    val sort: SortOption
)

class StoreViewModel(private val repository: StoreRepository) : ViewModel() {

    private val _currentScreen = MutableStateFlow(StoreScreen.HOME)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _showOnlyFavorites = MutableStateFlow(false)
    private val _showOnlySale = MutableStateFlow(false)
    private val _sortOption = MutableStateFlow(SortOption.FEATURED)
    private val _selectedProductIdForDetail = MutableStateFlow<Int?>(null)
    private val _isPromoApplied = MutableStateFlow(false)

    // Multi-turn Gemini Chat State
    private val initialWelcomeMessage = ChatMessage(
        text = "Assalam-o-Alaikum & welcome to My Store, proudly owned by HUSSAIN! I'm your AI Shopping Assistant. Ask me for outfit recommendations, product comparisons, or today's Big Sale deals.",
        isUser = false,
        modelUsed = GeminiModelOption.FLASH_GENERAL.displayName
    )
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(initialWelcomeMessage))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedAssistantRole = MutableStateFlow(AssistantRole.PERSONAL_STYLIST)
    val selectedAssistantRole: StateFlow<AssistantRole> = _selectedAssistantRole.asStateFlow()

    private val _selectedGeminiModel = MutableStateFlow(GeminiModelOption.FLASH_GENERAL)
    val selectedGeminiModel: StateFlow<GeminiModelOption> = _selectedGeminiModel.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureCatalogSeeded()
        }
    }

    private val filterParamsFlow = combine(
        _searchQuery,
        _selectedCategory,
        _showOnlyFavorites,
        _showOnlySale,
        _sortOption
    ) { query, category, onlyFavs, onlySale, sort ->
        FilterParams(query, category, onlyFavs, onlySale, sort)
    }

    private val dataFlow = combine(
        repository.productsFlow,
        repository.cartItemsFlow,
        repository.ordersFlow
    ) { products, cart, orders ->
        val effectiveProducts = products.ifEmpty { InitialCatalogData.defaultProducts }
        Triple(effectiveProducts, cart, orders)
    }

    val uiState: StateFlow<StoreUiState> = combine(
        _currentScreen,
        filterParamsFlow,
        dataFlow,
        _selectedProductIdForDetail,
        _isPromoApplied
    ) { screen, filters, (products, cart, orders), detailId, promoApplied ->
        val filtered = products.filter { product ->
            val matchesQuery = filters.query.isBlank() ||
                product.name.contains(filters.query, ignoreCase = true) ||
                product.category.contains(filters.query, ignoreCase = true) ||
                product.description.contains(filters.query, ignoreCase = true)
            val matchesCategory = filters.category == null ||
                product.category.equals(filters.category, ignoreCase = true)
            val matchesFav = !filters.onlyFavorites || product.isFavorite
            val matchesSale = !filters.onlySale || product.isOnSale
            matchesQuery && matchesCategory && matchesFav && matchesSale
        }.let { list ->
            when (filters.sort) {
                SortOption.FEATURED -> list
                SortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
                SortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
                SortOption.TOP_RATED -> list.sortedByDescending { it.rating }
            }
        }

        val detailProduct = detailId?.let { id -> products.find { it.id == id } }

        StoreUiState(
            currentScreen = screen,
            ownerName = "HUSSAIN",
            searchQuery = filters.query,
            selectedCategory = filters.category,
            showOnlyFavorites = filters.onlyFavorites,
            showOnlySale = filters.onlySale,
            sortOption = filters.sort,
            allProducts = products,
            filteredProducts = filtered,
            cartItems = cart,
            orders = orders,
            selectedProductForDetail = detailProduct,
            isPromoApplied = promoApplied
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StoreUiState()
    )

    fun navigateTo(screen: StoreScreen) {
        _currentScreen.value = screen
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleFavoritesFilter() {
        _showOnlyFavorites.value = !_showOnlyFavorites.value
    }

    fun toggleSaleFilter() {
        _showOnlySale.value = !_showOnlySale.value
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _showOnlyFavorites.value = false
        _showOnlySale.value = false
        _sortOption.value = SortOption.FEATURED
    }

    fun openProductDetail(product: ProductEntity) {
        _selectedProductIdForDetail.value = product.id
    }

    fun closeProductDetail() {
        _selectedProductIdForDetail.value = null
    }

    fun toggleProductFavorite(product: ProductEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(product)
        }
    }

    fun addToCart(product: ProductEntity, selectedSize: String = "M", quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, selectedSize, quantity)
            _snackbarEvents.tryEmit("${product.name} added to cart")
        }
    }

    fun updateCartItemQuantity(item: CartItemEntity, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(item, newQuantity)
        }
    }

    fun removeCartItem(item: CartItemEntity) {
        viewModelScope.launch {
            repository.removeCartItem(item.id)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            _isPromoApplied.value = false
        }
    }

    fun togglePromoCode() {
        _isPromoApplied.value = !_isPromoApplied.value
    }

    fun notifyCheckoutClicked() {
        _snackbarEvents.tryEmit("Checkout will be connected next.")
    }

    fun placeOrder(shippingAddress: String, paymentMethod: String) {
        val currentState = uiState.value
        if (currentState.cartItems.isEmpty()) return
        viewModelScope.launch {
            val order = repository.placeOrder(
                cartItems = currentState.cartItems,
                discountAmount = currentState.promoDiscount,
                shippingAddress = shippingAddress,
                paymentMethod = paymentMethod
            )
            _isPromoApplied.value = false
            _snackbarEvents.tryEmit("Order #${order.orderNumber} placed! Thank you for shopping with HUSSAIN.")
            _currentScreen.value = StoreScreen.ORDERS
        }
    }

    // Gemini Multi-Turn Chat Actions
    fun selectAssistantRole(role: AssistantRole) {
        _selectedAssistantRole.value = role
    }

    fun selectGeminiModel(model: GeminiModelOption) {
        _selectedGeminiModel.value = model
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Conversation reset! Welcome back to HUSSAIN's My Store. How can I help you as your ${_selectedAssistantRole.value.title}?",
                isUser = false,
                modelUsed = _selectedGeminiModel.value.displayName
            )
        )
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isChatGenerating.value) return

        val userMessage = ChatMessage(text = trimmed, isUser = true)
        val updatedHistory = _chatMessages.value + userMessage
        _chatMessages.value = updatedHistory
        _isChatGenerating.value = true

        val currentRole = _selectedAssistantRole.value
        val currentModel = _selectedGeminiModel.value
        val currentProducts = uiState.value.allProducts
        val currentCart = uiState.value.cartItems

        viewModelScope.launch {
            val result = GeminiChatService.sendMultiTurnMessage(
                history = updatedHistory,
                role = currentRole,
                modelOption = currentModel,
                products = currentProducts,
                cartItems = currentCart
            )
            _isChatGenerating.value = false

            result.fold(
                onSuccess = { replyText ->
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        text = replyText,
                        isUser = false,
                        modelUsed = "${currentRole.title} • ${currentModel.displayName}"
                    )
                },
                onFailure = { error ->
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        text = error.message ?: "Unable to reach Gemini right now. Please verify your GEMINI_API_KEY in the Secrets panel.",
                        isUser = false,
                        modelUsed = currentModel.displayName,
                        isError = true
                    )
                }
            )
        }
    }
}

class StoreViewModelFactory(private val repository: StoreRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
            return StoreViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
