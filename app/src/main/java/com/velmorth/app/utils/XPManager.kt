package com.velmorth.app.utils

/**
 * Handles XP-to-level mapping and leveling progress calculations.
 */
object XPManager {

    /**
     * Obtains the target total XP threshold needed to reach a specific level.
     */
    fun getXpThresholdForLevel(level: Int): Int {
        if (level <= 1) return 0
        // Leveling band algorithm: Level 2 = 50 XP, Level 3 = 120 XP, Level 4 = 220 XP, etc.
        return (level - 1) * 50 + (level - 1) * (level - 2) * 10
    }

    /**
     * Determines the current level based on total accrued XP.
     */
    fun getLevelForXp(xp: Int): Int {
        var currentLevel = 1
        while (xp >= getXpThresholdForLevel(currentLevel + 1)) {
            currentLevel++
        }
        return currentLevel
    }

    /**
     * Returns progress statistics for the current level.
     * @return Pair where:
     * - First: XP accumulated within current level bounds
     * - Second: Total XP needed within current level bounds to advance
     */
    fun getLevelProgress(xp: Int): Pair<Int, Int> {
        val level = getLevelForXp(xp)
        val currentLevelThreshold = getXpThresholdForLevel(level)
        val nextLevelThreshold = getXpThresholdForLevel(level + 1)
        
        val xpInCurrentLevel = xp - currentLevelThreshold
        val totalNeededForNextLevel = nextLevelThreshold - currentLevelThreshold

        return Pair(xpInCurrentLevel, totalNeededForNextLevel)
    }
}
