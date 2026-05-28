package com.example.learnwithvelmorth.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra small — chips, small tags
    extraSmall = RoundedCornerShape(8.dp),
    // Small — small cards, input fields
    small = RoundedCornerShape(12.dp),
    // Medium — standard cards, dialogs
    medium = RoundedCornerShape(20.dp),
    // Large — bottom sheets, feature cards
    large = RoundedCornerShape(28.dp),
    // Extra large — lesson cards, full-width cards
    extraLarge = RoundedCornerShape(36.dp),
)

// Custom shape tokens for Velmorth design
val PillShape = RoundedCornerShape(50.dp)        // Pill buttons
val LeafShape = RoundedCornerShape(             // Leaf-like asymmetric organic feel
    topStart = 24.dp,
    topEnd = 8.dp,
    bottomStart = 8.dp,
    bottomEnd = 24.dp
)
val ForestCardShape = RoundedCornerShape(24.dp)  // Standard forest card
val MascotBubbleShape = RoundedCornerShape(     // Chat bubble for Velmorth
    topStart = 4.dp,
    topEnd = 20.dp,
    bottomStart = 20.dp,
    bottomEnd = 20.dp
)
