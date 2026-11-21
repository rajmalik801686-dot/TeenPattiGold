package com.example.ui.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PlayerProfile
import com.example.data.UserPreferencesRepository
import com.example.domain.GameVariation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LobbyUiState(
    val profile: PlayerProfile = PlayerProfile(),
    val selectedVariation: GameVariation = GameVariation.CLASSIC,
    val bonusMessage: String? = null
)

class LobbyViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    private val selectedVariation = MutableStateFlow(GameVariation.CLASSIC)
    private val bonusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LobbyUiState> = combine(
        repository.profileFlow,
        selectedVariation,
        bonusMessage
    ) { profile, variation, message ->
        LobbyUiState(
            profile = profile,
            selectedVariation = variation,
            bonusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LobbyUiState()
    )

    fun selectVariation(variation: GameVariation) {
        selectedVariation.value = variation
    }

    fun claimDailyBonus() {
        viewModelScope.launch {
            if (uiState.value.profile.canClaimDailyBonus) {
                repository.claimDailyBonus(10_000L)
                bonusMessage.value = "+10,000 Virtual Chips added!"
            }
        }
    }

    fun claimEmergencyRefill() {
        viewModelScope.launch {
            repository.claimEmergencyRefill(5_000L)
            bonusMessage.value = "+5,000 Free Refill Chips added!"
        }
    }

    fun clearBonusMessage() {
        bonusMessage.value = null
    }

    fun updatePlayerName(name: String) {
        viewModelScope.launch {
            repository.updatePlayerName(name)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticsEnabled(enabled)
        }
    }

    class Factory(private val repository: UserPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LobbyViewModel(repository) as T
        }
    }
}
