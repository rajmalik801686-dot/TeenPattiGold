package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.PlayerProfile
import com.example.ui.theme.EmeraldFeltMid
import com.example.ui.theme.MonoChipFontFamily
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.RoyalGoldBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ChipFormatter

@Composable
fun ProfileAndBonusDialog(
    profile: PlayerProfile,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onClaimDailyBonus: () -> Unit,
    onClaimEmergencyRefill: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit
) {
    var nameInput by remember(profile.playerName) { mutableStateOf(profile.playerName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.5.dp, RoyalGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Player Lounge",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RoyalGold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close profile",
                            tint = TextSecondary
                        )
                    }
                }

                // Name editor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 16) nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalGold,
                            unfocusedBorderColor = RoyalGold.copy(alpha = 0.4f),
                            focusedLabelColor = RoyalGold,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_name_input")
                    )
                    OutlinedButton(
                        onClick = { onSaveName(nameInput) },
                        border = BorderStroke(1.dp, RoyalGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalGold),
                        modifier = Modifier.testTag("save_name_button")
                    ) {
                        Text("Save")
                    }
                }

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        label = "Hands Played",
                        value = "${profile.handsPlayed}",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Win Rate",
                        value = "${profile.winRatePercent}%",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        label = "Biggest Pot",
                        value = ChipFormatter.formatCompact(profile.biggestPotWon),
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Recent Best",
                        value = profile.bestHandName,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Daily Bonus
                Button(
                    onClick = onClaimDailyBonus,
                    enabled = profile.canClaimDailyBonus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalGold,
                        contentColor = ObsidianNight,
                        disabledContainerColor = ObsidianElevated,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("claim_daily_bonus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = if (profile.canClaimDailyBonus) "Claim Daily Bonus (+10,000)" else "Daily Bonus Claimed Today",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (profile.chips < 1_000L) {
                    Button(
                        onClick = onClaimEmergencyRefill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldFeltMid,
                            contentColor = RoyalGoldBright
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("emergency_refill_button")
                    ) {
                        Text("Free Virtual Refill (+5,000 Chips)", fontWeight = FontWeight.Bold)
                    }
                }

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Table Sound Effects", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = profile.soundEnabled,
                        onCheckedChange = onSoundChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianNight, checkedTrackColor = RoyalGold)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tactile Haptics", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = profile.hapticsEnabled,
                        onCheckedChange = onHapticsChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianNight, checkedTrackColor = RoyalGold)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ObsidianElevated,
        border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = MonoChipFontFamily,
                fontWeight = FontWeight.Bold,
                color = RoyalGoldBright,
                maxLines = 1
            )
        }
    }
}
