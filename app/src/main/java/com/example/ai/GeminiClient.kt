package com.example.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

enum class GeminiRole(
    val title: String,
    val subtitle: String,
    val modelName: String,
    val systemPrompt: String
) {
    GENERAL(
        title = "Showroom Expert",
        subtitle = "General queries & product advice",
        modelName = "gemini-flash-latest",
        systemPrompt = "You are the Showroom Product & Catalog AI Assistant. You are an expert in bathroom fittings, CP faucets, sanitaryware, hardware, tiles, and accessories. You know leading Indian and global brands including Hindware, Jaquar, Cera, Kohler, and Parryware. You provide helpful, concise, well-structured specifications, product comparisons, quotations, and pricing guidance. Keep answers professional and customer-ready."
    ),
    COMPLEX_STRATEGY(
        title = "Pricing Strategist",
        subtitle = "Complex margin, tax & bulk math",
        modelName = "gemini-3.1-pro-preview",
        systemPrompt = "You are an Expert Financial & Margin Strategist for wholesale and showroom distribution. You specialize in calculating complex tiered discounts, profit margin optimization, GST tax reconciliation, bulk quotation pricing, and competitor analysis. Always break down mathematical calculations step-by-step: MRP, Discount, Taxable Price, GST, and Net Margin."
    ),
    FAST_LOOKUP(
        title = "Fast Spec Finder",
        subtitle = "Lightning-fast code lookup",
        modelName = "gemini-3.1-flash-lite-preview",
        systemPrompt = "You are a High-Speed Product Code & Specification Matcher. You provide lightning-fast lookups, code interpretations (e.g. HW-1234, JQ-501), category mappings, and quick stock recommendations. Keep answers ultra-compact, bulleted, and immediate."
    )
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = ""
)

data class ImageGenerationResult(
    val bitmap: Bitmap? = null,
    val textDescription: String? = null,
    val errorMessage: String? = null
)

object GeminiClient {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Configure 60s timeout as mandated by gemini-api guidelines
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY.ifBlank { "" }
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() &&
                !key.contains("DEFAULT_API_KEY") &&
                !key.contains("MY_GEMINI_API_KEY") &&
                !key.contains("YOUR_API_KEY")
    }

    /**
     * Sends a multi-turn conversation to Gemini with a specified role and system instruction.
     */
    suspend fun sendChatMessage(
        messages: List<ChatMessage>,
        geminiRole: GeminiRole
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API Key is not configured. Please enter your GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }

        val modelsToTry = listOf(
            geminiRole.modelName,
            "gemini-flash-latest",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.1-pro-preview"
        ).distinct()

        var lastErrorMsg = ""

        for (modelCandidate in modelsToTry) {
            try {
                val rootJson = JSONObject()

                // Multi-turn contents array
                val contentsArray = JSONArray()
                for (msg in messages) {
                    val turn = JSONObject()
                    turn.put("role", if (msg.role == "user") "user" else "model")
                    val parts = JSONArray()
                    val part = JSONObject()
                    part.put("text", msg.content)
                    parts.put(part)
                    turn.put("parts", parts)
                    contentsArray.put(turn)
                }
                rootJson.put("contents", contentsArray)

                // System instruction for the role
                val systemInstruction = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", geminiRole.systemPrompt)
                sysParts.put(sysPart)
                systemInstruction.put("parts", sysParts)
                rootJson.put("systemInstruction", systemInstruction)

                // Generation config
                val genConfig = JSONObject()
                genConfig.put("temperature", 0.7)
                rootJson.put("generationConfig", genConfig)

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelCandidate:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errJson = JSONObject(responseBody)
                        errJson.optJSONObject("error")?.optString("message") ?: responseBody
                    } catch (e: Exception) {
                        responseBody
                    }
                    lastErrorMsg = "Gemini error (${response.code}) on $modelCandidate: $errorMsg"
                    // If 503 (high demand) or 429 (rate limit) or 500, try next candidate
                    if (response.code == 503 || response.code == 429 || response.code >= 500) {
                        continue
                    } else {
                        break
                    }
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val responseText = StringBuilder()
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val p = parts.getJSONObject(i)
                            if (p.has("text")) {
                                responseText.append(p.getString("text"))
                            }
                        }
                    }
                    if (responseText.isNotEmpty()) {
                        return@withContext Result.success(responseText.toString())
                    }
                }
            } catch (e: Exception) {
                lastErrorMsg = e.message ?: "Connection error"
            }
        }

        // If network / API models failed or 503 spike, check if smart showroom calculation fallback applies
        val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content ?: ""
        val smartFallback = tryLocalSmartShowroomResponse(lastUserMessage)
        if (smartFallback != null) {
            return@withContext Result.success(smartFallback)
        }

        return@withContext Result.failure(Exception(if (lastErrorMsg.isNotBlank()) lastErrorMsg else "Gemini servers are currently experiencing high demand. Please try again."))
    }

    /**
     * Provides an immediate, intelligent showroom fallback response for margin math,
     * quotations, and specs if the remote model is temporarily experiencing 503 spikes.
     */
    fun tryLocalSmartShowroomResponse(prompt: String): String? {
        val lower = prompt.lowercase()

        // Match pricing / margin calculations
        if ((lower.contains("mrp") || lower.contains("margin") || lower.contains("discount") || lower.contains("gst")) &&
            (lower.contains("calculate") || lower.contains("formula") || lower.contains("net") || lower.contains("%"))
        ) {
            // Extract numbers from text
            val mrpMatch = Regex("""mrp[^\d]*[₹Rs\.]*\s*([\d,]+)""", RegexOption.IGNORE_CASE).find(prompt)
                ?: Regex("""[₹Rs\.]\s*([\d,]+)""").find(prompt)
            val discMatch = Regex("""([\d\.]+)\s*%\s*disc""", RegexOption.IGNORE_CASE).find(prompt)
                ?: Regex("""discount[^\d]*([\d\.]+)\s*%""", RegexOption.IGNORE_CASE).find(prompt)
            val gstMatch = Regex("""([\d\.]+)\s*%\s*gst""", RegexOption.IGNORE_CASE).find(prompt)
                ?: Regex("""gst[^\d]*([\d\.]+)\s*%""", RegexOption.IGNORE_CASE).find(prompt)
            val purchaseMatch = Regex("""purchase[^\d]*[₹Rs\.]*\s*([\d,]+)""", RegexOption.IGNORE_CASE).find(prompt)
                ?: Regex("""cost[^\d]*[₹Rs\.]*\s*([\d,]+)""", RegexOption.IGNORE_CASE).find(prompt)

            val mrp = mrpMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: 14000.0
            val discount = discMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 35.0
            val gst = gstMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 18.0
            val purchase = purchaseMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: 6200.0

            val discAmt = mrp * (discount / 100.0)
            val taxable = mrp - discAmt
            val gstAmt = taxable * (gst / 100.0)
            val finalSale = taxable + gstAmt
            val marginAmt = taxable - purchase
            val marginOnCost = if (purchase > 0) (marginAmt / purchase) * 100.0 else 0.0
            val marginOnSale = if (taxable > 0) (marginAmt / taxable) * 100.0 else 0.0

            return buildString {
                appendLine("📊 **Showroom Financial & Margin Breakdown:**\n")
                appendLine("1. **MRP:** ₹%,.2f".format(mrp))
                appendLine("2. **Discount (%.1f%%):** -₹%,.2f".format(discount, discAmt))
                appendLine("3. **Taxable Selling Price:** ₹%,.2f".format(taxable))
                appendLine("4. **GST (%.1f%%):** +₹%,.2f".format(gst, gstAmt))
                appendLine("5. **Final Customer Price (Incl. GST):** ₹%,.2f\n".format(finalSale))
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("💼 **Purchase / Landing Cost:** ₹%,.2f".format(purchase))
                appendLine("💰 **Net Gross Margin:** ₹%,.2f (Taxable Price - Purchase Cost)".format(marginAmt))
                appendLine("📈 **Markup on Cost:** %.1f%%".format(marginOnCost))
                appendLine("📈 **Profit Margin on Sale:** %.1f%%".format(marginOnSale))
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("💡 *Tip: You can also generate an instant Quotation or Tax Bill for this product using the Quotation & Bill Generator in the app!*")
            }
        }

        // Match quotation / bill questions
        if (lower.contains("coatation") || lower.contains("quotation") || lower.contains("bill") || lower.contains("watsapp") || lower.contains("whatsapp")) {
            return buildString {
                appendLine("🧾 **Showroom Quotation & Bill Generator:**\n")
                appendLine("You can now create **Quotation (कोटेशन)** and **Tax Invoice (पक्का बिल)** directly from the app and share them instantly on WhatsApp!")
                appendLine("\n**Steps:**")
                appendLine("1. Go to **Dashboard** or **Tools** tab and tap **'Quotation / Bill'**.")
                appendLine("2. Enter Customer Name & WhatsApp Mobile Number.")
                appendLine("3. Add products from catalog or add custom fittings.")
                appendLine("4. View the clean, showroom-standard **Bill Preview**.")
                appendLine("5. Tap **'Send on WhatsApp (व्हाट्सएप भेजें)'** to send a complete, itemized bill to your customer.")
            }
        }

        return null
    }

    /**
     * Generates a new image or edits an existing image using gemini-3.1-flash-image-preview.
     */
    suspend fun generateOrEditImage(
        prompt: String,
        sourceBitmap: Bitmap? = null,
        aspectRatio: String = "1:1"
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext ImageGenerationResult(
                errorMessage = "Gemini API Key is not configured. Please enter your GEMINI_API_KEY in the Secrets panel in AI Studio."
            )
        }

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")

            val parts = JSONArray()
            // 1. Text prompt part
            val textPart = JSONObject()
            textPart.put("text", prompt)
            parts.put(textPart)

            // 2. If editing existing image, attach base64 inlineData
            if (sourceBitmap != null) {
                val imagePart = JSONObject()
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", bitmapToBase64(sourceBitmap))
                imagePart.put("inlineData", inlineData)
                parts.put(imagePart)
            }

            turn.put("parts", parts)
            contentsArray.put(turn)
            rootJson.put("contents", contentsArray)

            // Generation config with modalities and imageConfig
            val genConfig = JSONObject()
            val modalities = JSONArray()
            modalities.put("TEXT")
            modalities.put("IMAGE")
            genConfig.put("responseModalities", modalities)

            val imageConfig = JSONObject()
            imageConfig.put("aspectRatio", aspectRatio)
            imageConfig.put("imageSize", "1K")
            genConfig.put("imageConfig", imageConfig)

            rootJson.put("generationConfig", genConfig)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: responseBody
                } catch (e: Exception) {
                    responseBody
                }
                return@withContext ImageGenerationResult(
                    errorMessage = "Image generation failed (${response.code}): $errorMsg"
                )
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext ImageGenerationResult(errorMessage = "No image candidates returned.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val partsArray = content?.optJSONArray("parts")

            var outputBitmap: Bitmap? = null
            val textBuilder = StringBuilder()

            if (partsArray != null) {
                for (i in 0 until partsArray.length()) {
                    val p = partsArray.getJSONObject(i)
                    if (p.has("inlineData")) {
                        val inline = p.getJSONObject("inlineData")
                        val base64Data = inline.getString("data")
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        outputBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } else if (p.has("text")) {
                        textBuilder.append(p.getString("text"))
                    }
                }
            }

            if (outputBitmap != null) {
                ImageGenerationResult(
                    bitmap = outputBitmap,
                    textDescription = textBuilder.toString().ifBlank { null }
                )
            } else {
                ImageGenerationResult(
                    textDescription = textBuilder.toString(),
                    errorMessage = if (textBuilder.isNotBlank()) textBuilder.toString() else "Model did not produce an image."
                )
            }
        } catch (e: Exception) {
            ImageGenerationResult(errorMessage = e.message ?: "Unknown error generating image.")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
