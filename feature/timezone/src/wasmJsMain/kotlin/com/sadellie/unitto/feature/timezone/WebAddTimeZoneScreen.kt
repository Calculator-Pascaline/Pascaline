/*
 * Pascaline is a calculator for Android
 * Copyright (c) 2026 Calculator-Pascaline
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.feature.timezone

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sadellie.unitto.core.common.lev
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.ListItemExpressive
import com.sadellie.unitto.core.ui.ListItemExpressiveDefaults
import com.sadellie.unitto.core.ui.SearchBar
import com.sadellie.unitto.core.ui.SearchPlaceholder
import com.sadellie.unitto.core.ui.datetime.LocalPlatformDateFormatSettings
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res

/** A zone found by the search, with its names worked out once. */
private data class WebSearchResult(val zone: String, val name: String, val region: String)

/** Adds a favorite zone. [userMillis] is the moment shown on the main screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebAddTimeZoneScreen(navigateUp: () -> Unit, userMillis: Double) {
  val query = rememberTextFieldState()
  val favorites = WebFavoriteZones.favorites.collectAsState().value
  val zones =
    remember(favorites) {
      val favoriteZones = favorites.map { it.zone }.toSet()
      WebZones.all()
        .filter { it !in favoriteZones }
        .map { WebSearchResult(it, WebZones.name(it), WebZones.region(it)) }
    }
  val results = remember(zones, query.text) { search(zones, query.text.toString()) }
  val is24Hour = LocalPlatformDateFormatSettings.current.is24Hour
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    topBar = { SearchBar(state = query, navigateUp = navigateUp, scrollBehavior = scrollBehavior) },
  ) { paddingValues ->
    Crossfade(
      modifier = Modifier.fillMaxSize(),
      targetState = results.isEmpty() && query.text.isNotEmpty(),
      label = "Placeholder",
    ) { empty ->
      if (empty) {
        SearchPlaceholder(modifier = Modifier.padding(paddingValues))
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding =
            paddingValues +
              PaddingValues(start = Sizes.large, end = Sizes.large, bottom = Sizes.large),
          verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
        ) {
          itemsIndexed(results, { _, item -> item.zone }) { index, item ->
            ListItemExpressive(
              shape = ListItemExpressiveDefaults.listedShaped(index, results.size),
              modifier = Modifier.animateItem(),
              onClick = {
                WebFavoriteZones.add(item.zone)
                navigateUp()
              },
              headlineContent = { Text(item.name) },
              supportingContent = { Text(item.region) },
              trailingContent = {
                Text(
                  text = WebZones.formatTime(userMillis, item.zone, is24Hour),
                  style = MaterialTheme.typography.headlineSmall,
                )
              },
            )
          }
        }
      }
    }
  }
}

/** Like Android: the start of a name first, then a part of it, then close spellings. */
private fun search(zones: List<WebSearchResult>, searchQuery: String): List<WebSearchResult> {
  if (searchQuery.isBlank()) return zones.sortedBy { it.name }
  val query = searchQuery.trim().lowercase()
  val threshold = query.length / 2
  return zones
    .mapNotNull { zone ->
      val rank =
        listOf(zone.name, zone.region, zone.zone).mapNotNull { match(it, query, threshold) }.minOrNull()
      rank?.let { zone to it }
    }
    .sortedBy { it.second }
    .map { it.first }
}

private fun match(prop: String, query: String, threshold: Int): Int? {
  if (prop.startsWith(query, ignoreCase = true)) return 0
  if (prop.contains(query, ignoreCase = true)) return 1
  val distance = prop.lowercase().substring(0, minOf(query.length, prop.length)).lev(query)
  return if (distance < threshold) distance + 2 else null
}
