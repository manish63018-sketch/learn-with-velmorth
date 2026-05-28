package com.example.learnwithvelmorth.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VelmorthSpeaker — wraps Android TextToSpeech.
 *
 * Usage:
 *   speaker.speak("Hello!", onDone = { /* animation ends */ })
 *   speaker.stop()
 */
@Singleton
class VelmorthSpeaker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.15f) // slightly friendly / cheerful pitch
                isReady = true

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
                })
            }
        }
    }

    /**
     * Speak [text] via TTS. [voiceSpeed] overrides default rate.
     * [onDone] fires when utterance finishes.
     */
    fun speak(
        text: String,
        voiceSpeed: Float = 1.0f,
        onDone: (() -> Unit)? = null,
    ) {
        if (!isReady) return
        tts?.setSpeechRate(voiceSpeed.coerceIn(0.5f, 2.0f))

        // Override onDone per call
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }
            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onDone?.invoke()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onDone?.invoke()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /** Immediately stops any active speech. */
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
    }

    companion object {
        private const val UTTERANCE_ID = "velmorth_tts"
    }
}
