package com.tucolectivo.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun InicioComposeScreen(
    onNavigate: (Int) -> Unit,
    syncing: Boolean,
    onSync: () -> Unit
) {
    val selected = remember { mutableIntStateOf(0) }

    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            NeonHeader(
                menuItems = listOf("INICIO", "LÍNEAS", "MAPA", "FAVORITOS", "PARADAS CERCANAS"),
                onMenuItem = { index ->
                    selected.intValue = index
                    onNavigate(index)
                },
                onSearch = { }
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

            NeonSyncButton(
                syncing = syncing,
                onClick = onSync
            )

            NeonBottomBar(
                selected = selected.intValue,
                onSelect = {
                    selected.intValue = it
                    onNavigate(it)
                }
            )
        }
    }
}
