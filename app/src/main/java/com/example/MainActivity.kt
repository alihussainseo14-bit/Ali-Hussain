package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StoreDatabase
import com.example.data.StoreRepository
import com.example.ui.CartScreen
import com.example.ui.ChatScreen
import com.example.ui.HomeScreen
import com.example.ui.OrdersScreen
import com.example.ui.StoreScreen
import com.example.ui.StoreViewModel
import com.example.ui.StoreViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MyStoreApp()
            }
        }
    }
}

@Composable
fun MyStoreApp(
    viewModel: StoreViewModel = viewModel(
        factory = StoreViewModelFactory(
            StoreRepository(
                StoreDatabase.getDatabase(LocalContext.current).storeDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedAssistantRole.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedGeminiModel.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Crossfade(
        targetState = uiState.currentScreen,
        label = "store_screen_transition"
    ) { screen ->
        when (screen) {
            StoreScreen.HOME -> {
                HomeScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onCategoryClick = viewModel::selectCategory,
                    onSaleBannerClick = viewModel::toggleSaleFilter,
                    onFavoritesFilterToggle = viewModel::toggleFavoritesFilter,
                    onSortChange = viewModel::updateSortOption,
                    onClearFilters = viewModel::clearAllFilters,
                    onProductClick = viewModel::openProductDetail,
                    onDismissDetail = viewModel::closeProductDetail,
                    onToggleProductFavorite = viewModel::toggleProductFavorite,
                    onAddToCart = { product -> viewModel.addToCart(product) },
                    onAddToCartWithSize = { product, size, qty ->
                        viewModel.addToCart(product, size, qty)
                    },
                    onNavigateToCart = { viewModel.navigateTo(StoreScreen.CART) },
                    onNavigateToOrders = { viewModel.navigateTo(StoreScreen.ORDERS) },
                    onNavigateToChat = { viewModel.navigateTo(StoreScreen.CHAT) }
                )
            }

            StoreScreen.CART -> {
                CartScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onBack = { viewModel.navigateTo(StoreScreen.HOME) },
                    onUpdateQuantity = viewModel::updateCartItemQuantity,
                    onRemoveItem = viewModel::removeCartItem,
                    onClearCart = viewModel::clearCart,
                    onTogglePromo = viewModel::togglePromoCode,
                    onCheckoutClick = viewModel::notifyCheckoutClicked,
                    onConfirmOrder = viewModel::placeOrder
                )
            }

            StoreScreen.ORDERS -> {
                OrdersScreen(
                    orders = uiState.orders,
                    snackbarHostState = snackbarHostState,
                    onBack = { viewModel.navigateTo(StoreScreen.HOME) }
                )
            }

            StoreScreen.CHAT -> {
                ChatScreen(
                    messages = chatMessages,
                    selectedRole = selectedRole,
                    selectedModel = selectedModel,
                    isGenerating = isChatGenerating,
                    onSelectRole = viewModel::selectAssistantRole,
                    onSelectModel = viewModel::selectGeminiModel,
                    onSendMessage = viewModel::sendChatMessage,
                    onClearChat = viewModel::clearChatHistory,
                    onBack = { viewModel.navigateTo(StoreScreen.HOME) }
                )
            }
        }
    }
}
