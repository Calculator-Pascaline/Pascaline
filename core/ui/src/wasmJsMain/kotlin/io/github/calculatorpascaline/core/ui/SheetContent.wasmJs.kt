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

package io.github.calculatorpascaline.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.calculatorpascaline.core.navigation.DrawerItem
import org.jetbrains.compose.resources.getString

@Composable
internal actual fun DrawerItem(
  modifier: Modifier,
  destination: DrawerItem,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  colors: NavigationDrawerItemColors,
) {
  // Workaround for empty strings in wasm
  var label by rememberSaveable { mutableStateOf("") }
  LaunchedEffect(Unit) { label = getString(destination.name) }
  NavigationDrawerItem(
    modifier = modifier,
    label = { Text(label) },
    icon = { Icon(icon, label) },
    selected = selected,
    onClick = onClick,
    colors = colors,
  )
}
