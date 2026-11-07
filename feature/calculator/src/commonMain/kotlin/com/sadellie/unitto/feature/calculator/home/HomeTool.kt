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

package com.sadellie.unitto.feature.calculator.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Check
import com.sadellie.unitto.core.designsystem.icons.symbols.Edit
import com.sadellie.unitto.core.designsystem.icons.symbols.Remove
import com.sadellie.unitto.core.designsystem.icons.symbols.Settings
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.navigation.CalculatorDrawerItem
import com.sadellie.unitto.core.navigation.DRAWER_LAYOUT_KEY
import com.sadellie.unitto.core.navigation.DrawerItem
import com.sadellie.unitto.core.navigation.DrawerLayout
import com.sadellie.unitto.core.navigation.LocalNavigator
import com.sadellie.unitto.core.navigation.NamedTool
import com.sadellie.unitto.core.navigation.ORDER_SEPARATOR
import com.sadellie.unitto.core.navigation.SettingsDrawerItem
import com.sadellie.unitto.core.navigation.TOOL_ORDER_KEY
import com.sadellie.unitto.core.navigation.alphabetical
import com.sadellie.unitto.core.navigation.ordered
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.rememberKeyPress
import com.sadellie.unitto.core.ui.squashable
import com.sadellie.unitto.core.ui.trackKeyPress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.app_name
import unitto.core.common.generated.resources.common_edit
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.home_edit_hint
import unitto.core.common.generated.resources.home_hidden
import unitto.core.common.generated.resources.home_hide
import unitto.core.common.generated.resources.home_reset
import unitto.core.common.generated.resources.home_show

/** The list with the element at [from] moved to [to]. */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> =
  toMutableList().also { list -> list.add(to, list.removeAt(from)) }

/** The tile being dragged around its section, and the order that gives. */
@Stable
private class HomeReorder(private val grid: LazyGridState, private val scope: CoroutineScope) {
  var section by mutableStateOf<String?>(null)
    private set

  var dragged by mutableStateOf<String?>(null)
    private set

  var draft by mutableStateOf<List<String>>(emptyList())
    private set

  var offset by mutableStateOf(Offset.Zero)
    private set

  // The list only moves on the next frame
  private var movedFrom: IntOffset? = null

  fun start(section: String, ids: List<String>, id: String) {
    this.section = section
    draft = ids
    dragged = id
    offset = Offset.Zero
  }

  fun drag(amount: Offset) {
    val id = dragged ?: return
    offset += amount
    val visible = grid.layoutInfo.visibleItemsInfo
    val current = visible.firstOrNull { it.key == key(section, id) } ?: return
    if (current.offset == movedFrom) return
    movedFrom = null
    val center = current.offset.toOffset() + Offset(current.size.width / 2f, current.size.height / 2f) + offset
    val target =
      visible.firstOrNull { info ->
        val isTile = (info.key as? String)?.substringBefore(KEY_SEPARATOR) == section
        isTile && info.key != current.key && center.x.toInt() - info.offset.x in 0 until info.size.width &&
          center.y.toInt() - info.offset.y in 0 until info.size.height
      }
    if (target != null) {
      draft = draft.moved(draft.indexOf(id), draft.indexOf((target.key as String).substringAfter(KEY_SEPARATOR)))
      offset -= (target.offset - current.offset).toOffset()
      movedFrom = current.offset
    }
    val edge = SCROLL_EDGE
    val end = grid.layoutInfo.viewportEndOffset
    when {
      center.y > end - edge -> scope.launch { grid.scrollBy(SCROLL_STEP) }
      center.y < edge -> scope.launch { grid.scrollBy(-SCROLL_STEP) }
    }
  }

  fun end(onDrop: (List<String>) -> Unit) {
    if (section != null) onDrop(draft)
    section = null
    dragged = null
    offset = Offset.Zero
  }
}

private fun key(section: String?, id: String) = "$section$KEY_SEPARATOR$id"

/** What can be done with a tile, the edit mode changes it. */
private class TileActions(
  val isEditing: Boolean,
  val reorder: HomeReorder,
  val onOpen: (NamedTool) -> Unit,
  val onHiddenChange: (NamedTool, Boolean) -> Unit,
  val onDrop: (List<String>) -> Unit,
)

/** Every tool as a tile, from A to Z until they are moved. In the edit mode tiles can be dragged and hidden. */
@Composable
internal fun HomeRoute() {
  val navigator = LocalNavigator.current
  val preferences = koinInject<UserPreferencesRepository>()
  val savedLayout by
    remember(preferences) { preferences.appData(DRAWER_LAYOUT_KEY) }.collectAsState(initial = "")
  val savedOrder by
    remember(preferences) { preferences.appData(TOOL_ORDER_KEY) }.collectAsState(initial = "")
  val scope = rememberCoroutineScope()
  val layout = DrawerLayout.parse(savedLayout)
  val tools = layout.shown.map { NamedTool(it, stringResource(it.name)) }.alphabetical().ordered(savedOrder)
  val hidden = layout.hidden.map { NamedTool(it, stringResource(it.name)) }.alphabetical()
  var isEditing by rememberSaveable { mutableStateOf(false) }
  val grid = rememberLazyGridState()
  val reorder = remember(grid, scope) { HomeReorder(grid, scope) }
  // Names load after the first frame: the grid would follow its old first key down the list
  LaunchedEffect(tools.firstOrNull()?.id) { grid.scrollToItem(0) }
  fun save(key: String, value: String) {
    scope.launch { preferences.updateAppData(key, value) }
  }
  val actions =
    TileActions(
      isEditing = isEditing,
      reorder = reorder,
      onOpen = { tool -> navigator.openTool(tool.item) },
      onHiddenChange = { tool, hide ->
        when {
          !hide -> save(DRAWER_LAYOUT_KEY, DrawerLayout(layout.shown + tool.item, layout.hidden - tool.item).save())
          // At least one tool stays
          tools.size > 1 -> save(DRAWER_LAYOUT_KEY, DrawerLayout(layout.shown - tool.item, layout.hidden + tool.item).save())
        }
      },
      onDrop = { ids -> save(TOOL_ORDER_KEY, ids.joinToString(ORDER_SEPARATOR)) },
    )

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.app_name)) },
    navigationIcon = {
      IconButton(onClick = { isEditing = !isEditing }) {
        if (isEditing) Icon(Symbols.Check, stringResource(Res.string.common_ok))
        else Icon(Symbols.Edit, stringResource(Res.string.common_edit))
      }
    },
    keepNavigationIconOnWideScreens = true,
    actions = {
      IconButton(onClick = { navigator.openTool(SettingsDrawerItem) }) {
        Icon(Symbols.Settings, stringResource(SettingsDrawerItem.name))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      if (isEditing) EditBar(showsReset = savedOrder.isNotEmpty()) { save(TOOL_ORDER_KEY, "") }
      val hiddenTitle = stringResource(Res.string.home_hidden)
      LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        state = grid,
        columns = GridCells.Adaptive(KEY_WIDTH),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tiles(ALL_SECTION, tools, actions)
        if (isEditing && hidden.isNotEmpty()) {
          header(hiddenTitle)
          tiles(HIDDEN_SECTION, hidden, actions)
        }
      }
    }
  }
}

/** What editing does, and the way back to the first order. */
@Composable
private fun EditBar(showsReset: Boolean, onReset: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      stringResource(Res.string.home_edit_hint),
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (showsReset) TextButton(onClick = onReset) { Text(stringResource(Res.string.home_reset)) }
  }
}

private fun LazyGridScope.header(text: String) {
  item(key = text, span = { GridItemSpan(maxLineSpan) }) {
    Text(
      text,
      modifier = Modifier.padding(start = 4.dp, top = 8.dp),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

private fun LazyGridScope.tiles(section: String, tools: List<NamedTool>, actions: TileActions) {
  val reorder = actions.reorder
  val shown =
    if (reorder.section == section) reorder.draft.mapNotNull { id -> tools.firstOrNull { it.id == id } } else tools
  itemsIndexed(shown, key = { _, tool -> key(section, tool.id) }) { index, tool ->
    ToolTile(section, tool, tools, actions, Modifier.appearing(index))
  }
}

/** A tool as a key of the keypad, the calculator is colored like its equal key. */
@Composable
private fun LazyGridItemScope.ToolTile(
  section: String,
  tool: NamedTool,
  sectionTools: List<NamedTool>,
  actions: TileActions,
  appearance: Modifier,
) {
  val reorder = actions.reorder
  val isDragged = reorder.section == section && reorder.dragged == tool.id
  val canMove = actions.isEditing && section != HIDDEN_SECTION
  val isHidden = section == HIDDEN_SECTION
  val currentTools = sectionTools.map { it.id }
  val placed =
    if (isDragged) {
      Modifier.zIndex(1f).graphicsLayer {
        translationX = reorder.offset.x
        translationY = reorder.offset.y
        scaleX = DRAGGED_SCALE
        scaleY = DRAGGED_SCALE
      }
    } else {
      Modifier.animateItem()
    }
  val dragging =
    if (canMove) {
      Modifier.pointerInput(section, tool.id) {
        detectDragGestures(
          onDragStart = { reorder.start(section, currentTools, tool.id) },
          onDrag = { change, amount ->
            change.consume()
            reorder.drag(amount)
          },
          onDragEnd = { reorder.end(actions.onDrop) },
          onDragCancel = { reorder.end(actions.onDrop) },
        )
      }
    } else {
      Modifier
    }
  val isCalculator = tool.item == CalculatorDrawerItem
  val background = if (isCalculator) Color(CALCULATOR_KEY) else MaterialTheme.colorScheme.surfaceContainerHigh
  val content = if (isCalculator) Color.White else MaterialTheme.colorScheme.onSurface
  val keyPress = rememberKeyPress()
  val interactionSource = remember { MutableInteractionSource() }
  // The badge is outside of the key: keys clip what is in them
  Box(modifier = placed.then(appearance).then(wiggle(actions.isEditing)).then(dragging).height(KEY_HEIGHT)) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .alpha(if (isHidden) HIDDEN_ALPHA else 1f)
          .trackKeyPress(keyPress)
          .squashable(
            onClick = { actions.onOpen(tool) },
            enabled = !actions.isEditing,
            interactionSource = interactionSource,
            cornerRadiusRange = KEY_CORNER..KEY_PRESSED_CORNER,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            isPressed = keyPress.isPressed,
          )
          .background(background)
          .padding(horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
      Icon(tool.item.defaultIcon, null, tint = content)
      Text(
        tool.name,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (actions.isEditing) {
      EditBadge(
        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
        icon = if (isHidden) Symbols.Add else Symbols.Remove,
        label = stringResource(if (isHidden) Res.string.home_show else Res.string.home_hide),
        color = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        onClick = { actions.onHiddenChange(tool, !isHidden) },
      )
    }
  }
}

/** The key rises and grows into place, a moment after the one before it. */
@Composable
private fun Modifier.appearing(index: Int): Modifier {
  val appeared = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    delay((index * APPEAR_DELAY).coerceAtMost(MAX_APPEAR_DELAY))
    appeared.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
  }
  return graphicsLayer {
    val part = appeared.value
    alpha = part.coerceIn(0f, 1f)
    scaleX = APPEAR_SCALE + (1 - APPEAR_SCALE) * part
    scaleY = APPEAR_SCALE + (1 - APPEAR_SCALE) * part
    translationY = (1 - part) * APPEAR_RISE.toPx()
  }
}

/** Keys gently shake while they can be moved. */
@Composable
private fun wiggle(isEditing: Boolean): Modifier {
  if (!isEditing) return Modifier
  val transition = rememberInfiniteTransition(label = "Editing tiles")
  val angle by
    transition.animateFloat(
      initialValue = -WIGGLE_ANGLE,
      targetValue = WIGGLE_ANGLE,
      animationSpec = infiniteRepeatable(tween(WIGGLE_MILLIS, easing = LinearEasing), RepeatMode.Reverse),
      label = "Wiggle",
    )
  return Modifier.graphicsLayer { rotationZ = angle }
}

@Composable
private fun EditBadge(modifier: Modifier, icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
  Box(
    modifier.size(BADGE_TOUCH).clip(CircleShape).clickable(onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Box(Modifier.size(BADGE_SIZE).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
      Icon(icon, label, modifier = Modifier.size(BADGE_ICON), tint = MaterialTheme.colorScheme.surface)
    }
  }
}

private const val ALL_SECTION = "all"
private const val HIDDEN_SECTION = "hidden"
private const val KEY_SEPARATOR = "/"
private const val DRAGGED_SCALE = 1.08f
private const val HIDDEN_ALPHA = 0.5f
private const val WIGGLE_ANGLE = 1f
private const val WIGGLE_MILLIS = 130
private const val SCROLL_EDGE = 120
private const val SCROLL_STEP = 12f
private const val APPEAR_DELAY = 28L
private const val MAX_APPEAR_DELAY = 400L
private const val APPEAR_SCALE = 0.7f
private val APPEAR_RISE = 24.dp
private val KEY_WIDTH = 104.dp
private val KEY_HEIGHT = 92.dp
private const val KEY_CORNER = 26
private const val KEY_PRESSED_CORNER = 36
private const val CALCULATOR_KEY = 0xFFF08A24
private val BADGE_SIZE = 22.dp
private val BADGE_ICON = 16.dp
private val BADGE_TOUCH = 36.dp
