package com.tucolectivo.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun InicioComposeScreen(
    onNavigate: (Int) -> Unit,
    syncing: Boolean,
    onSync: () -> Unit
) {
    val selected = remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf(HeaderPanel.None) }
    val menuItems = listOf("INICIO", "LÍNEAS", "MAPA", "FAVORITOS", "PARADAS CERCANAS")

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                NeonHeader(
                    menuOpen = panel == HeaderPanel.Menu,
                    searchOpen = panel == HeaderPanel.Search,
                    notificationsOpen = panel == HeaderPanel.Notifications,
                    onMenuClick = {
                        panel = if (panel == HeaderPanel.Menu) HeaderPanel.None else HeaderPanel.Menu
                    },
                    onSearchClick = {
                        panel = if (panel == HeaderPanel.Search) HeaderPanel.None else HeaderPanel.Search
                    },
                    onNotificationsClick = {
                        panel = if (panel == HeaderPanel.Notifications) HeaderPanel.None else HeaderPanel.Notifications
                    },
                    animationsEnabled = false
                )

                Box(modifier = Modifier.weight(1f)) {
                    NeonMenuScreen(
                        onLineas = {
                            selected.intValue = 1
                            onNavigate(1)
                        },
                        onMapa = {
                            selected.intValue = 2
                            onNavigate(2)
                        },
                        onParadas = {
                            selected.intValue = 4
                            onNavigate(4)
                        },
                        onFavoritos = {
                            selected.intValue = 3
                            onNavigate(3)
                        }
                    )
                }

                NeonSyncButton(syncing = syncing, onClick = onSync, animationsEnabled = false)

                NeonBottomBar(
                    selected = selected.intValue,
                    onSelect = {
                        selected.intValue = it
                        onNavigate(it)
                    },
                    animationsEnabled = false
                )
            }

            CyberContextOverlays(
                panel = panel,
                menuItems = menuItems,
                notifications = emptyList(),
                onClose = { panel = HeaderPanel.None },
                onMenuItem = { index ->
                    panel = HeaderPanel.None
                    selected.intValue = index
                    onNavigate(index)
                },
                onSearch = { panel = HeaderPanel.None },
                onNotification = { panel = HeaderPanel.None }
            )
        }
    }
}
