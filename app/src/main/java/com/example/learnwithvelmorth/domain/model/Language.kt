package com.example.learnwithvelmorth.domain.model

data class Language(
    val id: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val isAvailable: Boolean = true,
)

val AvailableLanguages = listOf(
    Language("es", "Spanish", "Español", "🇪🇸"),
    Language("fr", "French", "Français", "🇫🇷"),
    Language("de", "German", "Deutsch", "🇩🇪"),
    Language("ja", "Japanese", "日本語", "🇯🇵"),
    Language("it", "Italian", "Italiano", "🇮🇹"),
    Language("pt", "Portuguese", "Português", "🇧🇷"),
    Language("ko", "Korean", "한국어", "🇰🇷"),
    Language("zh", "Mandarin", "普通话", "🇨🇳"),
)
