package com.velmorth.app.ui.settings

import androidx.lifecycle.ViewModel
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

/**
 * Represents a language the user can learn (i.e., the target/active language).
 *
 * Flutter equivalent — the `Locale` objects in:
 * ```dart
 * supportedLocales: const [
 *   Locale('en'),
 *   Locale('hi'),
 *   Locale('ja'),
 * ]
 * ```
 */
data class SupportedLanguage(
    /** Internal key used in PrefsManager / Firestore (e.g. "japanese", "hindi", "english"). */
    val id: String,
    /** Display name shown in the UI. */
    val displayName: String,
    /** Emoji flag shown in lesson cards. */
    val flag: String,
    /** BCP-47 locale tag for text rendering / TTS (e.g. "ja", "hi", "en"). */
    val localeTag: String
)

/**
 * Reactive locale/language state provider.
 *
 * Flutter equivalent: `LocaleProvider` (ChangeNotifier).
 *
 * In this Android app, "locale" tracks two distinct things:
 *  1. **Learning language** (`selectedLanguage`) — the language the user is actively studying
 *     (Japanese, French, Sanskrit, English). Stored in [PrefsManager.selectedLanguage].
 *  2. **Native language** (`nativeLanguage`) — the language lessons are taught *in*
 *     (English, Hindi). Stored in [PrefsManager.nativeLanguage].
 *
 * Note: Full app-UI locale switching (menus/labels in Hindi) requires an Activity restart
 * and is handled via [AppCompatDelegate.setApplicationLocales] at the Settings screen level.
 * This ViewModel tracks the *content* language state only.
 *
 * Flutter equivalent:
 * ```dart
 * Consumer<LocaleProvider>(
 *   builder: (context, localeProvider, _) => MaterialApp(
 *     locale: localeProvider.locale,
 *   ),
 * )
 * ```
 *
 * Usage in Compose:
 * ```kotlin
 * val locale by viewModel.selectedLanguage.collectAsState()
 * Text(text = "Learning: ${locale.displayName}")
 * ```
 */
@HiltViewModel
class LocaleViewModel @Inject constructor(
    private val prefs: PrefsManager
) : ViewModel() {

    companion object {
        /** All languages the app supports for learning. */
        val SUPPORTED_LEARNING_LANGUAGES: List<SupportedLanguage> = listOf(
            SupportedLanguage(
                id          = "japanese",
                displayName = "Japanese",
                flag        = "🇯🇵",
                localeTag   = "ja"
            ),
            SupportedLanguage(
                id          = "french",
                displayName = "French",
                flag        = "🇫🇷",
                localeTag   = "fr"
            ),
            SupportedLanguage(
                id          = "sanskrit",
                displayName = "Sanskrit",
                flag        = "🇮🇳",
                localeTag   = "sa"
            ),
            SupportedLanguage(
                id          = "english",
                displayName = "English",
                flag        = "🇬🇧",
                localeTag   = "en"
            )
        )

        /** Languages the user can receive instruction in (native/UI language). */
        val SUPPORTED_NATIVE_LANGUAGES: List<SupportedLanguage> = listOf(
            SupportedLanguage(
                id          = "english",
                displayName = "English",
                flag        = "🇬🇧",
                localeTag   = "en"
            ),
            SupportedLanguage(
                id          = "hindi",
                displayName = "Hindi",
                flag        = "🇮🇳",
                localeTag   = "hi"
            )
        )

        fun findById(id: String): SupportedLanguage =
            SUPPORTED_LEARNING_LANGUAGES.firstOrNull { it.id == id }
                ?: SUPPORTED_LEARNING_LANGUAGES.first()
    }

    // ── Selected learning language (what the user is studying) ─────────────────

    private val _selectedLanguage = MutableStateFlow(
        findById(prefs.selectedLanguage)
    )

    /**
     * The language the user is currently studying.
     *
     * Flutter equivalent: `localeProvider.locale` (for the target language).
     */
    val selectedLanguage: StateFlow<SupportedLanguage> = _selectedLanguage.asStateFlow()

    // ── Native language (what language lessons are taught in) ─────────────────

    private val _nativeLanguage = MutableStateFlow(
        SUPPORTED_NATIVE_LANGUAGES.firstOrNull { it.id == prefs.nativeLanguage }
            ?: SUPPORTED_NATIVE_LANGUAGES.first()
    )

    /**
     * The language lessons are presented in (instruction language).
     */
    val nativeLanguage: StateFlow<SupportedLanguage> = _nativeLanguage.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Changes the active learning language and persists to prefs + Firestore.
     *
     * Flutter equivalent:
     * ```dart
     * context.read<LocaleProvider>().setLocale(Locale('ja'));
     * ```
     */
    fun setSelectedLanguage(languageId: String) {
        val lang = findById(languageId)
        prefs.selectedLanguage = lang.id
        _selectedLanguage.value = lang
        FirestoreProgressRepository.saveActiveLanguage(lang.id)
    }

    /**
     * Changes the native (instruction) language.
     * Call this from the Settings screen. Changes take full effect on next
     * Activity restart (for string resources to reload).
     */
    fun setNativeLanguage(languageId: String) {
        val lang = SUPPORTED_NATIVE_LANGUAGES.firstOrNull { it.id == languageId }
            ?: SUPPORTED_NATIVE_LANGUAGES.first()
        prefs.nativeLanguage = lang.id
        _nativeLanguage.value = lang
    }

    /**
     * Returns the [Locale] object for the currently selected learning language.
     * Useful for TTS (text-to-speech) initialization.
     */
    fun getSelectedLocale(): Locale =
        Locale.forLanguageTag(_selectedLanguage.value.localeTag)

    /**
     * Returns the [Locale] for the native/instruction language.
     * Useful for loading locale-specific string resources.
     */
    fun getNativeLocale(): Locale =
        Locale.forLanguageTag(_nativeLanguage.value.localeTag)
}
