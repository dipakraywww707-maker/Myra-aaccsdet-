package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.system.BatteryInfo
import com.example.data.system.DeviceTelemetry
import com.example.data.system.NetworkStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MyraAiRepository(private val apiService: GeminiApiService = GeminiClient.apiService) {

    suspend fun askMyra(
        userPrompt: String,
        conversationHistory: List<Pair<String, Boolean>>, // (text, isFromUser)
        battery: BatteryInfo,
        telemetry: DeviceTelemetry,
        network: NetworkStatus,
        isTorchOn: Boolean
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineIntelligentReply(userPrompt, battery, telemetry, isTorchOn)
        }

        try {
            val systemContextPrompt = """
                You are Myra, a brilliant, charming, and highly intelligent AI phone companion and system assistant.
                You are running directly on the user's Android phone.
                You speak fluent Hinglish (Hindi + English mix), Hindi, and English. Match the language of the user's input.
                Keep your answers concise, natural, warm, and helpful.

                Current Live Phone Telemetry:
                - Device: ${telemetry.manufacturer} ${telemetry.deviceModel} (Android ${telemetry.androidVersion})
                - Battery: ${battery.level}% (Charging: ${battery.isCharging}, Temp: ${"%.1f".format(battery.temperatureCelsius)}°C)
                - Flashlight/Torch: ${if (isTorchOn) "ON" else "OFF"}
                - Network: ${network.connectionType} (${if (network.isConnected) "Connected" else "Offline"})

                If the user asks to control or check phone settings (torch, battery, apps, volume, camera, wifi, etc.), advise them or acknowledge that you can assist directly. Always be supportive, witty, and loyal like a true personal AI assistant.
            """.trimIndent()

            val contents = mutableListOf<GeminiContent>()

            // Add previous recent messages for conversation flow (up to last 6)
            val recentHistory = conversationHistory.takeLast(6)
            for ((text, isUser) in recentHistory) {
                contents.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = text)),
                        role = if (isUser) "user" else "model"
                    )
                )
            }

            // Add current message
            contents.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = userPrompt)),
                    role = "user"
                )
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemContextPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 1000
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!responseText.isNullOrBlank()) {
                responseText.trim()
            } else {
                getOfflineIntelligentReply(userPrompt, battery, telemetry, isTorchOn)
            }
        } catch (e: Exception) {
            Log.e("MyraAiRepository", "Gemini API error: ${e.message}", e)
            getOfflineIntelligentReply(userPrompt, battery, telemetry, isTorchOn, errorNote = e.localizedMessage)
        }
    }

    private fun getOfflineIntelligentReply(
        prompt: String,
        battery: BatteryInfo,
        telemetry: DeviceTelemetry,
        isTorchOn: Boolean,
        errorNote: String? = null
    ): String {
        val q = prompt.lowercase()
        return when {
            q.contains("kaise ho") || q.contains("how are you") || q.contains("kya haal") ->
                "Main bilkul badhiya hoon! Aapka phone super smooth chal raha hai. Aap bataiye aaj main aapki kya madad kar sakti hoon? ✨"

            q.contains("naam kya hai") || q.contains("who are you") || q.contains("tum kaun ho") ->
                "Main Myra hoon—aapki personal AI assistant aur phone system companion! Main aapke phone ke sabhi tools control kar sakti hoon aur sawalon ke jawab de sakti hoon. 💫"

            q.contains("battery") || q.contains("charge") ->
                "Aapke phone me abhi ${battery.level}% battery bachi hai. ${if (battery.isCharging) "Charging active hai!" else "Abhi battery normal backup de rahi hai."}"

            q.contains("torch") || q.contains("flashlight") ->
                "Flashlight abhi ${if (isTorchOn) "ON" else "OFF"} hai. Aap mujhse 'Torch on' ya 'Torch off' bolkar control kar sakte hain! 🔦"

            q.contains("phone") || q.contains("device") || q.contains("model") ->
                "Aapka device ${telemetry.manufacturer} ${telemetry.deviceModel} hai, Android ${telemetry.androidVersion} par running. 📱"

            q.contains("kya kar sakti ho") || q.contains("features") || q.contains("help") ->
                "Main aapke phone ki flashlight on/off kar sakti hoon, battery check, volume change, YouTube/WhatsApp/Camera khol sakti hoon, notes save kar sakti hoon, aur har sawal ka smart jawab de sakti hoon! 🚀"

            else ->
                "Main Myra hoon! Maine aapka message samjha: '$prompt'. Main aapke phone ke system ko control karne aur madad ke liye hamesha tayyar hoon. ${if (errorNote != null) "Tip: API key Secrets panel se set karke live cloud Gemini intelligence activate kar sakte hain!" else ""}"
        }
    }
}
