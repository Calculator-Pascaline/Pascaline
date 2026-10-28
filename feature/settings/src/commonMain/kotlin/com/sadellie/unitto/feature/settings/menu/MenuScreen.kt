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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.icons.symbols.AddCircle
import com.sadellie.unitto.core.designsystem.icons.symbols.Cancel
import com.sadellie.unitto.core.designsystem.icons.symbols.DragHandle
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.icons.symbols.Undo
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.navigation.DrawerItem
import com.sadellie.unitto.core.navigation.DrawerLayout
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ListHeader
import com.sadellie.unitto.core.ui.ListItemExpressive
import com.sadellie.unitto.core.ui.ListItemExpressiveDefaults
import com.sadellie.unitto.core.ui.NavigateUpButton
import com.sadellie.unitto.core.ui.ScaffoldWithLargeTopBar
import com.sadellie.unitto.core.ui.plus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.settings_menu
import unitto.core.common.generated.resources.settings_menu_hidden
import unitto.core.common.generated.resources.settings_menu_hide
import unitto.core.common.generated.resources.settings_menu_reorder
import unitto.core.common.generated.resources.settings_menu_reset
import unitto.core.common.generated.resources.settings_menu_show
import unitto.core.common.generated.resources.settings_menu_shown

@Composable
internal fun MenuRoute(navigateUp: () -> Unit) {
  val viewModel: MenuViewModel = koinViewModel()
  when (val layout = viewModel.layout.collectAsStateWithLifecycleKMP().value) {
    null -> EmptyScreen()
    else ->
      MenuScreen(
        layout = layout,
        navigateUp = navigateUp,
        reorder = viewModel::reorder,
        hide = viewModel::hide,
        show = viewModel::show,
        reset = viewModel::reset,
      )
  }
}

/** Apps of the menu: drag to reorder, hide the ones that aren't needed. */
@Composable
private fun MenuScreen(
  layout: DrawerLayout,
  navigateUp: () -> Unit,
  reorder: (List<DrawerItem>) -> Unit,
  hide: (DrawerItem) -> Unit,
  show: (DrawerItem) -> Unit,
  reset: () -> Unit,
) {
  ScaffoldWithLargeTopBar(
    title = stringResource(Res.string.settings_menu),
    navigationIcon = { NavigateUpButton(navigateUp) },
    actions = {
      IconButton(onClick = reset, shapes = IconButtonDefaults.shapes()) {
        Icon(Symbols.Undo, stringResource(Res.string.settings_menu_reset))
      }
    },
  ) { paddingValues ->
    // Moved while dragging, saved when the drag stops
    val shown = rememberUpdatedState(layout.shown) as MutableState
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState =
      rememberReorderableLazyListState(lazyListState) { from, to ->
        // -1 for the header
        shown.value =
          shown.value.toMutableList().apply { add(to.index - 1, removeAt(from.index - 1)) }
      }
    val headerModifier =
      Modifier.padding(
        start = Sizes.small,
        end = Sizes.small,
        top = Sizes.large,
        bottom = Sizes.small,
      )

    LazyColumn(
      state = lazyListState,
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
      contentPadding =
        paddingValues + PaddingValues(start = Sizes.large, end = Sizes.large, bottom = Sizes.large),
    ) {
      item(key = "shown") {
        ListHeader(
          modifier = Modifier.animateItem().then(headerModifier),
          text = stringResource(Res.string.settings_menu_shown),
        )
      }
      itemsIndexed(shown.value, key = { _, item -> item.topLevelRoute.routeId }) { index, item ->
        ShownItem(
          reorderableLazyListState = reorderableLazyListState,
          item = item,
          // The menu keeps at least one app
          hide = if (shown.value.size > 1) ({ hide(item) }) else null,
          onDragStopped = { reorder(shown.value) },
          shape = ListItemExpressiveDefaults.listedShaped(index, shown.value.size),
        )
      }
      if (layout.hidden.isNotEmpty()) {
        item(key = "hidden") {
          ListHeader(
            modifier = Modifier.animateItem().then(headerModifier),
            text = stringResource(Res.string.settings_menu_hidden),
          )
        }
      }
      itemsIndexed(layout.hidden, key = { _, item -> item.topLevelRoute.routeId }) { index, item ->
        ListItemExpressive(
          modifier = Modifier.animateItem(),
          onClick = { show(item) },
          shape = ListItemExpressiveDefaults.listedShaped(index, layout.hidden.size),
          headlineContent = { Text(stringResource(item.name)) },
          leadingContent = { Icon(item.defaultIcon, null) },
          trailingContent = {
            IconButton(onClick = { show(item) }, shapes = IconButtonDefaults.shapes()) {
              Icon(Symbols.AddCircle, stringResource(Res.string.settings_menu_show))
            }
          },
        )
      }
    }
  }
}

@Composable
private fun LazyItemScope.ShownItem(
  reorderableLazyListState: ReorderableLazyListState,
  item: DrawerItem,
  hide: (() -> Unit)?,
  onDragStopped: () -> Unit,
  shape: Shape,
) {
  ReorderableItem(reorderableLazyListState, item.topLevelRoute.routeId) { isDragging ->
    val transition = updateTransition(isDragging, label = "draggedTransition")
    val background by
      transition.animateColor(label = "background") {
        if (it) MaterialTheme.colorScheme.surfaceContainerHighest
        else MaterialTheme.colorScheme.surfaceBright
      }
    val itemPadding by transition.animateDp(label = "itemPadding") { if (it) Sizes.large else 0.dp }
    val cornerRadius by
      transition.animateDp(label = "cornerRadius") { if (it) Sizes.large else 0.dp }

    ListItemExpressive(
      headlineContent = { Text(stringResource(item.name)) },
      onClick = {},
      modifier =
        Modifier.padding(horizontal = itemPadding)
          .clip(RoundedCornerShape(cornerRadius.value))
          .longPressDraggableHandle(onDragStopped = onDragStopped),
      colors = ListItemDefaults.colors(containerColor = background),
      shape = shape,
      leadingContent = { Icon(item.defaultIcon, null) },
      trailingContent = {
        Row {
          if (hide != null) {
            IconButton(onClick = hide, shapes = IconButtonDefaults.shapes()) {
              Icon(Symbols.Cancel, stringResource(Res.string.settings_menu_hide))
            }
          }
          IconButton(
            onClick = {},
            modifier = Modifier.draggableHandle(onDragStopped = onDragStopped),
            shapes = IconButtonDefaults.shapes(),
          ) {
            Icon(Symbols.DragHandle, stringResource(Res.string.settings_menu_reorder))
          }
        }
      },
    )
  }
}
