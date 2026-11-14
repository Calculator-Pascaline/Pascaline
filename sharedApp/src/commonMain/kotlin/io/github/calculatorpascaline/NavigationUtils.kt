/*
 * Unitto is a calculator for Android
 * Copyright (c) 2026 Elshan Agaev
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

package io.github.calculatorpascaline

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import io.github.calculatorpascaline.core.navigation.DrawerItem

/** Navigates to clicked [destination]. */
internal fun navigateToTab(destination: DrawerItem, backStack: NavBackStack<NavKey>) {
  // single top
  if (backStack.lastOrNull() == destination.topLevelRoute) return
  val root = backStack.firstOrNull()
  backStack.removeAll { it != root }
  backStack.add(destination.topLevelRoute)
}
