package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "teen_patti_gold_prefs")

data class PlayerProfile(
    val playerName: String = "Raja",
    val chips: Long = 25_000L,
    val handsPlayed: Int = 0,
    val handsWon: Int = 0,
    val biggestPotWon: Long = 0L,
    val bestHandName: String = "—",
    val lastDailyBonusEpochDay: Long = 0L,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
) {
    val winRatePercent: Int
        get() = if (handsPlayed == 0) 0 else ((handsWon * 100f) / handsPlayed).toInt()

    val canClaimDailyBonus: Boolean
        get() = LocalDate.now().toEpochDay() > lastDailyBonusEpochDay
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val CHIPS = longPreferencesKey("chips")
        val HANDS_PLAYED = intPreferencesKey("hands_played")
        val HANDS_WON = intPreferencesKey("hands_won")
        val BIGGEST_POT_WON = longPreferencesKey("biggest_pot_won")
        val BEST_HAND_NAME = stringPreferencesKey("best_hand_name")
        val LAST_DAILY_BONUS_DAY = longPreferencesKey("last_daily_bonus_day")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val profileFlow: Flow<PlayerProfile> = context.dataStore.data.map { prefs ->
        PlayerProfile(
            playerName = prefs[Keys.PLAYER_NAME] ?: "Raja",
            chips = prefs[Keys.CHIPS] ?: 25_000L,
            handsPlayed = prefs[Keys.HANDS_PLAYED] ?: 0,
            handsWon = prefs[Keys.HANDS_WON] ?: 0,
            biggestPotWon = prefs[Keys.BIGGEST_POT_WON] ?: 0L,
            bestHandName = prefs[Keys.BEST_HAND_NAME] ?: "—",
            lastDailyBonusEpochDay = prefs[Keys.LAST_DAILY_BONUS_DAY] ?: 0L,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true
        )
    }

    suspend fun updateChips(newChips: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHIPS] = newChips.coerceAtLeast(0L)
        }
    }

    suspend fun recordHandResult(
        newChips: Long,
        won: Boolean,
        potWon: Long,
        handLabel: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHIPS] = newChips.coerceAtLeast(0L)
            prefs[Keys.HANDS_PLAYED] = (prefs[Keys.HANDS_PLAYED] ?: 0) + 1
            if (won) {
                prefs[Keys.HANDS_WON] = (prefs[Keys.HANDS_WON] ?: 0) + 1
                val currentBestPot = prefs[Keys.BIGGEST_POT_WON] ?: 0L
                if (potWon > currentBestPot) {
                    prefs[Keys.BIGGEST_POT_WON] = potWon
                }
            }
            if (!handLabel.isNullOrBlank() && (prefs[Keys.BEST_HAND_NAME] == null || prefs[Keys.BEST_HAND_NAME] == "—" || won)) {
                prefs[Keys.BEST_HAND_NAME] = handLabel
            }
        }
    }

    suspend fun claimDailyBonus(bonusAmount: Long = 10_000L): Long {
        val today = LocalDate.now().toEpochDay()
        var updatedTotal = 0L
        context.dataStore.edit { prefs ->
            val lastDay = prefs[Keys.LAST_DAILY_BONUS_DAY] ?: 0L
            val currentChips = prefs[Keys.CHIPS] ?: 25_000L
            if (today > lastDay) {
                updatedTotal = currentChips + bonusAmount
                prefs[Keys.CHIPS] = updatedTotal
                prefs[Keys.LAST_DAILY_BONUS_DAY] = today
            } else {
                updatedTotal = currentChips
            }
        }
        return updatedTotal
    }

    suspend fun claimEmergencyRefill(refillAmount: Long = 5_000L) {
        context.dataStore.edit { prefs ->
            val currentChips = prefs[Keys.CHIPS] ?: 0L
            if (currentChips < 1_000L) {
                prefs[Keys.CHIPS] = currentChips + refillAmount
            }
        }
    }

    suspend fun updatePlayerName(name: String) {
        val cleaned = name.trim().take(16).ifEmpty { "Raja" }
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAYER_NAME] = cleaned
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }
}
