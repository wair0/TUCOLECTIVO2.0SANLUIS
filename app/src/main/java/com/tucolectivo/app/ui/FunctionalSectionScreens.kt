package com.tucolectivo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.transpuntano.app.model.TransitStop

@Composable
fun FavoritesComposeScreen(favorites: List<FavoriteStopUi>, onFavoriteClick: (FavoriteStopUi) -> Unit) {
 Column(Modifier.fillMaxSize().padding(12.dp)) {
  Text("FAVORITOS")
  if (favorites.isEmpty()) Text("NO HAY FAVORITOS", Modifier.padding(12.dp))
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
   items(favorites) { favorite ->
    Button(onClick = { onFavoriteClick(favorite) }, modifier = Modifier.fillMaxWidth()) { Text(favorite.description) }
   }
  }
 }
}

@Composable
fun NearbyComposeScreen(stops: List<TransitStop>, loading: Boolean, error: String?, onRefresh: () -> Unit, onStopClick: (TransitStop) -> Unit) {
 Column(Modifier.fillMaxSize().padding(12.dp)) {
  Text("PARADAS CERCANAS")
  Button(onClick = onRefresh, enabled = !loading) { Text(if (loading) "BUSCANDO..." else "ACTUALIZAR") }
  error?.let { Text(it) }
  if (!loading && stops.isEmpty() && error == null) Text("SIN PARADAS CERCANAS", Modifier.padding(12.dp))
  else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
   items(stops) { stop ->
    Button(onClick = { onStopClick(stop) }, modifier = Modifier.fillMaxWidth()) { Text(stop.description) }
   }
  }
 }
}
