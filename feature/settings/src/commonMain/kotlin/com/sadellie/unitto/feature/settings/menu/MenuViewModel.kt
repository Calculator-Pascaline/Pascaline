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

package com.sadellie.unitto.feature.settings.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.navigation.DRAWER_LAYOUT_KEY
import com.sadellie.unitto.core.navigation.DrawerItem
import com.sadellie.unitto.core.navigation.DrawerLayout
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class MenuViewModel(private val userPrefsRepository: UserPreferencesRepository) :
  ViewModel() {
  val layout =
    userPrefsRepository
      .appData(DRAWER_LAYOUT_KEY)
      .map { DrawerLayout.parse(it) }
      .stateIn(viewModelScope, null)

  fun reorder(shown: List<DrawerItem>) = save { copy(shown = shown) }

  fun hide(item: DrawerItem) = save {
    if (shown.size > 1) DrawerLayout(shown - item, listOf(item) + hidden) else this
  }

  fun show(item: DrawerItem) = save { DrawerLayout(shown + item, hidden - item) }

  fun reset() = viewModelScope.launch { userPrefsRepository.updateAppData(DRAWER_LAYOUT_KEY, "") }

  private fun save(change: DrawerLayout.() -> DrawerLayout) =
    viewModelScope.launch {
      val current = layout.value ?: return@launch
      userPrefsRepository.updateAppData(DRAWER_LAYOUT_KEY, current.change().save())
    }
}
