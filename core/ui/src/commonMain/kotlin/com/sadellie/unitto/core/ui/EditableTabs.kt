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

package com.sadellie.unitto.core.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sadellie.unitto.core.designsystem.icons.symbols.DragHandle
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.navigation.DrawerItem
import org.jetbrains.compose.resources.stringResource

/** The menu while it is edited: drag a tool up or down, [onMove] gets the new order on release. */
@Composable
internal fun EditableTabs(tabs: List<DrawerItem>, onMove: (List<DrawerItem>) -> Unit) {
  var order by remember(tabs) { mutableStateOf(tabs) }
  var dragged by remember { mutableStateOf<DrawerItem?>(null) }
  var offset by remember { mutableFloatStateOf(0f) }
  val save by rememberUpdatedState(onMove)

  fun swap(item: DrawerItem, to: Int) {
    order = (order - item).toMutableList().apply { add(to, item) }
  }

  order.forEach { item ->
    key(item) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .height(ROW_HEIGHT)
            .zIndex(if (item == dragged) 1f else 0f)
            .graphicsLayer { translationY = if (item == dragged) offset else 0f }
            .pointerInput(Unit) {
              detectDragGestures(
                onDragStart = {
                  dragged = item
                  offset = 0f
                },
                onDragEnd = {
                  dragged = null
                  save(order)
                },
                onDragCancel = { dragged = null },
              ) { change, amount ->
                change.consume()
                offset += amount.y
                val row = ROW_HEIGHT.toPx()
                val index = order.indexOf(item)
                if (offset > row / 2 && index < order.lastIndex) {
                  swap(item, index + 1)
                  offset -= row
                } else if (offset < -row / 2 && index > 0) {
                  swap(item, index - 1)
                  offset += row
                }
              }
            }
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(item.defaultIcon, contentDescription = null)
        Text(stringResource(item.name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Icon(Symbols.DragHandle, contentDescription = null)
      }
    }
  }
}

private val ROW_HEIGHT = 48.dp
