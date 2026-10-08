package com.example.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MyraVoiceManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                // Try Hindi or fallback to English or default locale
                val hindiLocale = Locale.forLanguageTag("hi-IN")
                val result = tts?.setLanguage(hindiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.ENGLISH
                }
                tts?.setPitch(1.05f) // Pleasant female assistant voice pitch
                tts?.setSpeechRate(1.0f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.e("MyraVoiceManager", "TextToSpeech init failed with code $status")
            }
        }
    }

    fun speak(text: String) {
        if (_isMuted.value || !isInitialized) return
        // Clean markdown symbols, emojis from spoken text for smooth speech
        val cleaned = text
            .replace(Regex("[#*`_~>\\[\\]()]"), " ")
            .replace(Regex("[\\p{So}\\p{Cn}]"), "") // strip emojis from speech
            .trim()

        if (cleaned.isBlank()) return

        tts?.stop()
        _isSpeaking.value = true
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "myra_utterance_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun toggleMute(): Boolean {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        if (newMute) {
            stop()
        }
        return newMute
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("MyraVoiceManager", "TTS shutdown error", e)
        }
    }
}
