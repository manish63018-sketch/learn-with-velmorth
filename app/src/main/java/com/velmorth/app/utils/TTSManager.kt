package com.velmorth.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Handles Android native Text-To-Speech engine for speaking language elements.
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingText: String? = null
    private var pendingLocale: Locale? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d("TTSManager", "TextToSpeech engine successfully initialized.")
            
            // Speak any pending text if it was queued before initialization finished
            pendingText?.let { text ->
                val locale = pendingLocale ?: Locale.JAPANESE
                speak(text, locale)
                pendingText = null
                pendingLocale = null
            }
        } else {
            Log.e("TTSManager", "Failed to initialize TextToSpeech engine. Status code: $status")
        }
    }

    /**
     * Pronounces the given text using a specific locale.
     * Defaults to [Locale.JAPANESE] for our seed content.
     */
    fun speak(text: String, locale: Locale = Locale.JAPANESE) {
        if (!isInitialized) {
            pendingText = text
            pendingLocale = locale
            return
        }

        try {
            tts?.let { engine ->
                val result = engine.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSManager", "The language locale $locale is not supported or missing data.")
                } else {
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "velmorth_tts_id")
                }
            }
        } catch (e: Exception) {
            Log.e("TTSManager", "Exception occurred during speech output", e)
        }
    }

    /**
     * Safely releases the TextToSpeech engine resources.
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            Log.d("TTSManager", "TextToSpeech engine resources released.")
        } catch (e: Exception) {
            Log.e("TTSManager", "Exception occurred during engine shutdown", e)
        }
    }
}
