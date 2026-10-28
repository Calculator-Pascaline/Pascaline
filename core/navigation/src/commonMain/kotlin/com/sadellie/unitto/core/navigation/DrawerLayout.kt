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

package com.sadellie.unitto.core.navigation

/**
 * Apps of the menu in the order chosen by the user, and the ones they hid.
 *
 * Saved as `shown;hidden` with route ids separated by commas. Apps that are in neither, like new
 * ones, are shown at the end.
 */
data class DrawerLayout(val shown: List<DrawerItem>, val hidden: List<DrawerItem>) {
  fun save(): String =
    shown.joinToString(",") { it.topLevelRoute.routeId } +
      ";" +
      hidden.joinToString(",") { it.topLevelRoute.routeId }

  companion object {
    fun parse(saved: String, items: List<DrawerItem> = mainDrawerItems): DrawerLayout {
      val byId = items.associateBy { it.topLevelRoute.routeId }
      val parts = saved.split(";")
      fun part(index: Int) =
        parts.getOrNull(index).orEmpty().split(",").mapNotNull { byId[it] }.distinct()

      val hidden = part(1)
      val shown = (part(0) - hidden.toSet()).let { it + (items - it.toSet() - hidden.toSet()) }
      // Every app hidden would leave an empty menu
      return if (shown.isEmpty()) DrawerLayout(items, emptyList()) else DrawerLayout(shown, hidden)
    }
  }
}

/** Key of the saved [DrawerLayout] in app data. */
const val DRAWER_LAYOUT_KEY = "DRAWER_LAYOUT"
