package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.UserPreferencesRepository
import com.example.domain.GameVariation
import com.example.domain.TableTier
import com.example.ui.lobby.LobbyScreen
import com.example.ui.lobby.LobbyViewModel
import com.example.ui.table.TableScreen
import com.example.ui.table.TableViewModel
import com.example.ui.theme.TeenPattiGoldTheme
import kotlinx.serialization.Serializable

@Serializable
object LobbyRoute

@Serializable
data class TableRoute(
    val tierName: String,
    val variationName: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val repository = UserPreferencesRepository(applicationContext)

        setContent {
            TeenPattiGoldTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = LobbyRoute
                ) {
                    composable<LobbyRoute> {
                        val lobbyViewModel: LobbyViewModel = viewModel(
                            factory = remember { LobbyViewModel.Factory(repository) }
                        )
                        LobbyScreen(
                            viewModel = lobbyViewModel,
                            onEnterTable = { tier, variation ->
                                navController.navigate(
                                    TableRoute(
                                        tierName = tier.name,
                                        variationName = variation.name
                                    )
                                )
                            }
                        )
                    }

                    composable<TableRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<TableRoute>()
                        val tier = TableTier.entries.firstOrNull { it.name == route.tierName }
                            ?: TableTier.BRONZE
                        val variation = GameVariation.entries.firstOrNull { it.name == route.variationName }
                            ?: GameVariation.CLASSIC

                        val tableViewModel: TableViewModel = viewModel(
                            key = "${tier.name}_${variation.name}",
                            factory = remember(tier, variation) {
                                TableViewModel.Factory(repository, tier, variation)
                            }
                        )
                        TableScreen(
                            viewModel = tableViewModel,
                            onLeaveTable = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
