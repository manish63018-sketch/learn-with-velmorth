package com.example.learnwithvelmorth.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnwithvelmorth.data.repository.DialogueRepository
import com.example.learnwithvelmorth.domain.VelmorthEmotion
import com.example.learnwithvelmorth.domain.VelmorthSpeaker
import com.example.learnwithvelmorth.domain.VelmorthTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class VelmorthCharacterState(
    val emotion: VelmorthEmotion        = VelmorthEmotion.IDLE,
    val dialogueText: String            = "",
    val targetLangLine: String          = "",
    val isSpeaking: Boolean             = false,
    val isVisible: Boolean              = true,
    val wrongAnswerCount: Int           = 0,
)

@HiltViewModel
class VelmorthCharacterViewModel @Inject constructor(
    private val dialogueRepository: DialogueRepository,
    private val speaker: VelmorthSpeaker,
) : ViewModel() {

    private val _state = MutableStateFlow(VelmorthCharacterState())
    val state: StateFlow<VelmorthCharacterState> = _state.asStateFlow()

    /** Observe TTS speaking state and keep character in TALKING emotion while active. */
    private var previousEmotion: VelmorthEmotion = VelmorthEmotion.IDLE

    init {
        viewModelScope.launch {
            // Seed dialogues if DB is empty
            dialogueRepository.seedIfEmpty()
        }

        // Mirror TTS speaking state into character emotion
        viewModelScope.launch {
            speaker.isSpeaking.collect { speaking ->
                if (speaking) {
                    previousEmotion = _state.value.emotion
                    _state.update { it.copy(isSpeaking = true, emotion = VelmorthEmotion.TALKING) }
                } else {
                    _state.update { it.copy(isSpeaking = false, emotion = previousEmotion) }
                }
            }
        }
    }

    /**
     * Main entry point — call this from any screen.
     * Example:  viewModel.fireEvent(VelmorthTrigger.LESSON_COMPLETE)
     */
    fun fireEvent(trigger: String, speakAloud: Boolean = true) {
        viewModelScope.launch {
            // Track consecutive wrong answers for encouragement escalation
            val wrongCount = if (trigger == VelmorthTrigger.WRONG_ANSWER) {
                _state.value.wrongAnswerCount + 1
            } else {
                0
            }

            // Escalate to multiple-wrong trigger after 3 mistakes
            val resolvedTrigger = if (wrongCount >= 3) VelmorthTrigger.MULTIPLE_WRONG else trigger

            val dialogue = dialogueRepository.pickDialogue(resolvedTrigger)
                ?: dialogueRepository.pickDialogue(VelmorthTrigger.APP_OPEN) // fallback
                ?: return@launch

            val emotion = VelmorthEmotion.fromString(dialogue.emotion)
            previousEmotion = emotion

            _state.update {
                it.copy(
                    emotion         = emotion,
                    dialogueText    = dialogue.text,
                    targetLangLine  = dialogue.targetLangLine,
                    wrongAnswerCount = wrongCount,
                )
            }

            if (speakAloud) {
                speaker.speak(dialogue.text, voiceSpeed = dialogue.voiceSpeed)
            }

            // Auto-clear dialogue bubble after 5 seconds (unless still speaking)
            delay(5_000)
            if (!_state.value.isSpeaking) {
                _state.update { it.copy(dialogueText = "", targetLangLine = "") }
                // Settle back to idle
                delay(500)
                _state.update { it.copy(emotion = VelmorthEmotion.IDLE) }
            }
        }
    }

    /**
     * Fire the appropriate time-of-day greeting on app open.
     */
    fun greetUser(speakAloud: Boolean = true) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        fireEvent(VelmorthTrigger.greetingForHour(hour), speakAloud)
    }

    /** Display a custom message without querying the DB. */
    fun showCustomMessage(
        text: String,
        emotion: VelmorthEmotion = VelmorthEmotion.HAPPY,
        targetLang: String = "",
        speakAloud: Boolean = true,
    ) {
        viewModelScope.launch {
            previousEmotion = emotion
            _state.update {
                it.copy(
                    emotion        = emotion,
                    dialogueText   = text,
                    targetLangLine = targetLang,
                )
            }
            if (speakAloud) speaker.speak(text)
            delay(5_000)
            if (!_state.value.isSpeaking) {
                _state.update { it.copy(dialogueText = "", targetLangLine = "") }
                delay(500)
                _state.update { it.copy(emotion = VelmorthEmotion.IDLE) }
            }
        }
    }

    /** Immediately clear the speech bubble. */
    fun dismissDialogue() {
        speaker.stop()
        _state.update { it.copy(dialogueText = "", targetLangLine = "", isSpeaking = false, emotion = VelmorthEmotion.IDLE) }
    }

    override fun onCleared() {
        super.onCleared()
        speaker.stop()
    }
}
