package com.velmorth.app.data.model

/**
 * Represents a virtual item available in the leaf shop.
 */
data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val type: String, // "leaf_booster", "theme", "shield", etc.
    val owned: Boolean
)
