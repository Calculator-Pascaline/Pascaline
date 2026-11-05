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

import com.sadellie.unitto.core.common.WebStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A favorite time zone of the web app and the label given to it. */
internal data class WebFavoriteZone(val zone: String, val label: String)

/** Favorite time zones in their order, saved in the browser (so in the web backup too). */
internal object WebFavoriteZones {
  private val _favorites = MutableStateFlow(load())
  val favorites: StateFlow<List<WebFavoriteZone>> = _favorites.asStateFlow()

  fun add(zone: String) = change { list ->
    if (list.any { it.zone == zone }) list else list + WebFavoriteZone(zone, "")
  }

  fun remove(zone: String) = change { list -> list.filterNot { it.zone == zone } }

  fun updateLabel(zone: String, label: String) = change { list ->
    list.map { if (it.zone == zone) it.copy(label = label) else it }
  }

  /** Saves the order after dragging. */
  fun reorder(zones: List<WebFavoriteZone>) = change { zones }

  private fun change(transform: (List<WebFavoriteZone>) -> List<WebFavoriteZone>) {
    _favorites.update(transform)
    WebStorage.set(KEY, WebStorage.encode(_favorites.value.flatMap { listOf(it.zone, it.label) }))
  }

  private fun load(): List<WebFavoriteZone> =
    WebStorage.decode(WebStorage.get(KEY) ?: "")
      .chunked(2)
      .filter { it.size == 2 && WebZones.exists(it[0]) }
      .map { (zone, label) -> WebFavoriteZone(zone, label) }

  private const val KEY = "timeZones"
}
