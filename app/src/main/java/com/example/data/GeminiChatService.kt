package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val subtitle: String
) {
    FLASH_GENERAL(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        subtitle = "General shopping & styling tasks"
    ),
    PRO_COMPLEX(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        subtitle = "Complex wardrobe planning & comparisons"
    ),
    FLASH_LITE_FAST(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash-Lite",
        subtitle = "Fast instant answers"
    )
}

enum class AssistantRole(
    val title: String,
    val badge: String,
    val roleInstruction: String
) {
    PERSONAL_STYLIST(
        title = "Personal Stylist",
        badge = "Outfit & Style Expert",
        roleInstruction = "You are the Personal Stylist for 'My Store', owned and founded by HUSSAIN. Help customers pair clothes, shoes, watches, and leather bags into cohesive outfits, suggest sizes, and highlight quality craftsmanship."
    ),
    DEAL_FINDER(
        title = "Deal Advisor",
        badge = "Savings & Bundles",
        roleInstruction = "You are the Deal & Value Advisor for 'My Store', owned by HUSSAIN. Help shoppers maximize their budget, explain the Big Sale (up to 50% OFF), remind them about promo code SAVE10 for an extra 10% off at checkout, and suggest high-value bundles."
    ),
    STORE_CONCIERGE(
        title = "Store Concierge",
        badge = "Catalog & Support",
        roleInstruction = "You are the Official Store Concierge for 'My Store', owned by HUSSAIN. Answer questions about product specifications, shipping, returns, order tracking, and HUSSAIN's commitment to customer satisfaction."
    )
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val isError: Boolean = false
)

interface GeminiRestApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body requestBody: RequestBody
    ): ResponseBody
}

object GeminiChatService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val api: GeminiRestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
            .create(GeminiRestApi::class.java)
    }

    fun buildSystemPrompt(
        role: AssistantRole,
        products: List<ProductEntity>,
        cartItems: List<CartItemEntity>
    ): String {
        val catalogText = products.joinToString("\n") { product ->
            "- ${product.name} (${product.category}): Rs. ${product.price.toLong()} " +
                "(Original: Rs. ${product.originalPrice.toLong()}, Rating: ${product.rating}/5) — ${product.description}"
        }
        val cartText = if (cartItems.isEmpty()) {
            "Customer's cart is currently empty."
        } else {
            "Customer's current cart: " + cartItems.joinToString(", ") {
                "${it.quantity}x ${it.name} (Size ${it.selectedSize}, Rs. ${it.price.toLong()} each)"
            }
        }

        return """
            ${role.roleInstruction}
            
            Store Information:
            - Store Name: My Store
            - Store Owner / Proprietor: HUSSAIN
            - Currency: Pakistani Rupees (Rs.)
            - Active Promotions: Big Sale up to 50% OFF on featured items, plus promo code SAVE10 for 10% off in My Cart.
            
            Current Store Catalog:
            $catalogText
            
            $cartText
            
            Keep responses warm, helpful, concise, and tailored to HUSSAIN's store catalog.
        """.trimIndent()
    }

    suspend fun sendMultiTurnMessage(
        history: List<ChatMessage>,
        role: AssistantRole,
        modelOption: GeminiModelOption,
        products: List<ProductEntity>,
        cartItems: List<CartItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException(
                    "GEMINI_API_KEY is not configured. Please add your Gemini API key in the AI Studio Secrets panel to enable live AI responses."
                )
            )
        }

        try {
            val systemPrompt = buildSystemPrompt(role, products, cartItems)

            val rootJson = JSONObject()

            // System Instruction
            val systemInstructionJson = JSONObject().apply {
                put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", systemPrompt)
                    )
                )
            }
            rootJson.put("systemInstruction", systemInstructionJson)

            // Multi-turn contents array (take last 16 messages to respect context window)
            val contentsArray = JSONArray()
            history.filter { !it.isError }.takeLast(16).forEach { msg ->
                val turnJson = JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put("text", msg.text)
                        )
                    )
                }
                contentsArray.put(turnJson)
            }
            rootJson.put("contents", contentsArray)

            // GenerationConfig
            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            }
            rootJson.put("generationConfig", generationConfig)

            val requestBody = rootJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val responseBody = api.generateContent(
                model = modelOption.modelId,
                apiKey = apiKey,
                requestBody = requestBody
            )

            val responseString = responseBody.string()
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val partsArray = contentObj?.optJSONArray("parts")

            val replyBuilder = StringBuilder()
            if (partsArray != null) {
                for (i in 0 until partsArray.length()) {
                    val partText = partsArray.optJSONObject(i)?.optString("text").orEmpty()
                    if (partText.isNotEmpty()) {
                        replyBuilder.append(partText)
                    }
                }
            }

            val finalReply = replyBuilder.toString().trim()
            if (finalReply.isNotEmpty()) {
                Result.success(finalReply)
            } else {
                Result.failure(IllegalStateException("Received an empty response from ${modelOption.displayName}."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
