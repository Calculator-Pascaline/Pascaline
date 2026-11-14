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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Check
import com.sadellie.unitto.core.designsystem.icons.symbols.Close
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.Edit
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowDown
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowUp
import com.sadellie.unitto.core.designsystem.icons.symbols.Remove
import com.sadellie.unitto.core.designsystem.icons.symbols.Search
import com.sadellie.unitto.core.designsystem.icons.symbols.Settings
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.sharedTool
import com.sadellie.unitto.core.designsystem.sharedToolKey
import com.sadellie.unitto.core.navigation.BodyMassDrawerItem
import com.sadellie.unitto.core.navigation.CalculatorDrawerItem
import com.sadellie.unitto.core.navigation.ChargingDrawerItem
import com.sadellie.unitto.core.navigation.ConverterDrawerItem
import com.sadellie.unitto.core.navigation.DRAWER_LAYOUT_KEY
import com.sadellie.unitto.core.navigation.DateCalculatorDrawerItem
import com.sadellie.unitto.core.navigation.DrawerItem
import com.sadellie.unitto.core.navigation.DrawerLayout
import com.sadellie.unitto.core.navigation.ElementsDrawerItem
import com.sadellie.unitto.core.navigation.FiltersDrawerItem
import com.sadellie.unitto.core.navigation.FinanceDrawerItem
import com.sadellie.unitto.core.navigation.FunctionsDrawerItem
import com.sadellie.unitto.core.navigation.InferenceDrawerItem
import com.sadellie.unitto.core.navigation.IntegralsDrawerItem
import com.sadellie.unitto.core.navigation.LocalNavigator
import com.sadellie.unitto.core.navigation.LogicDrawerItem
import com.sadellie.unitto.core.navigation.NamedTool
import com.sadellie.unitto.core.navigation.ORDER_SEPARATOR
import com.sadellie.unitto.core.navigation.OrbitsDrawerItem
import com.sadellie.unitto.core.navigation.PlaceDrawerItem
import com.sadellie.unitto.core.navigation.PricesDrawerItem
import com.sadellie.unitto.core.navigation.ProbabilityDrawerItem
import com.sadellie.unitto.core.navigation.ProgrammerDrawerItem
import com.sadellie.unitto.core.navigation.ProjectileDrawerItem
import com.sadellie.unitto.core.navigation.RegressionDrawerItem
import com.sadellie.unitto.core.navigation.ResistorsDrawerItem
import com.sadellie.unitto.core.navigation.SequencesDrawerItem
import com.sadellie.unitto.core.navigation.SettingsDrawerItem
import com.sadellie.unitto.core.navigation.StatisticsDrawerItem
import com.sadellie.unitto.core.navigation.VectorFieldsDrawerItem
import com.sadellie.unitto.core.navigation.inDefaultOrder
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.keyHaptics
import com.sadellie.unitto.core.ui.rememberKeyPress
import com.sadellie.unitto.core.ui.squashable
import com.sadellie.unitto.core.ui.trackKeyPress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.app_name
import unitto.core.common.generated.resources.common_cancel
import unitto.core.common.generated.resources.common_clear
import unitto.core.common.generated.resources.common_edit
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.home_add_section
import unitto.core.common.generated.resources.home_data
import unitto.core.common.generated.resources.home_delete_section
import unitto.core.common.generated.resources.home_edit_sections_hint
import unitto.core.common.generated.resources.home_electronics
import unitto.core.common.generated.resources.home_everyday
import unitto.core.common.generated.resources.home_hidden
import unitto.core.common.generated.resources.home_hide
import unitto.core.common.generated.resources.home_math
import unitto.core.common.generated.resources.home_move_down
import unitto.core.common.generated.resources.home_move_to
import unitto.core.common.generated.resources.home_move_up
import unitto.core.common.generated.resources.home_new_section
import unitto.core.common.generated.resources.home_no_tool
import unitto.core.common.generated.resources.home_recent
import unitto.core.common.generated.resources.home_rename
import unitto.core.common.generated.resources.home_reset
import unitto.core.common.generated.resources.home_science
import unitto.core.common.generated.resources.home_search
import unitto.core.common.generated.resources.home_section_name
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

  fun end(onDrop: (String, List<String>) -> Unit) {
    section?.let { onDrop(it, draft) }
    section = null
    dragged = null
    offset = Offset.Zero
  }
}

private fun key(section: String?, id: String) = "$section$KEY_SEPARATOR$id"

/** What can be done with a tile, the edit mode changes it. */
private class TileActions(
  val state: TileState,
  val reorder: HomeReorder,
  val onOpen: (String, NamedTool) -> Unit,
  val onHiddenChange: (NamedTool, Boolean) -> Unit,
  val onDrop: (String, List<String>) -> Unit,
  val onMove: (NamedTool) -> Unit,
) {
  val isEditing get() = state.isEditing
  val animatesIn get() = state.animatesIn
  val opened get() = state.opened
}

/**
 * @property animatesIn Tiles rise into place, only when Home first shows.
 * @property opened The tile a tool was opened from, that the tool shrinks back into.
 */
private class TileState(val isEditing: Boolean, val animatesIn: Boolean, val opened: String?)

/**
 * App data that Home keeps while a tool is open, so it comes back as it was instead of filling in
 * a moment later and pushing everything down. Null until it is first read.
 */
@Composable
private fun savedAppData(preferences: UserPreferencesRepository, key: String): String? {
  var value by rememberSaveable(key) { mutableStateOf<String?>(null) }
  LaunchedEffect(preferences, key) { preferences.appData(key).collect { value = it } }
  return value
}

/** The first sections: one per topic, with its tools. */
private val DEFAULT_SECTIONS by lazy {
  Topic.entries.map { topic -> HomeSection(topic.name, null, topic.items.map { it.topLevelRoute.routeId }) }
}

/**
 * Every tool in its section, under a search and the tools used last. In the edit mode tiles can
 * be dragged, hidden and moved to another section, and sections renamed, moved and added.
 */
@Composable
internal fun HomeRoute() {
  val navigator = LocalNavigator.current
  val preferences = koinInject<UserPreferencesRepository>()
  val savedLayout = savedAppData(preferences, DRAWER_LAYOUT_KEY)
  val savedSections = savedAppData(preferences, SECTIONS_KEY)
  val latestRecent = savedAppData(preferences, RECENT_KEY)
  // Opening a tool puts it first in the recent ones. Home shows that when it comes back, not while
  // the tool grows out of it
  val savedRecent = remember(latestRecent == null) { latestRecent }
  if (savedLayout == null || savedSections == null || savedRecent == null) return
  val scope = rememberCoroutineScope()
  val layout = DrawerLayout.parse(savedLayout)
  val tools = layout.shown.map { NamedTool(it, stringResource(it.name)) }
  val hidden = layout.hidden.map { NamedTool(it, stringResource(it.name)) }.inDefaultOrder()
  val sections = arrangeSections(HomeSection.decode(savedSections), DEFAULT_SECTIONS, tools.map { it.id })
  var isEditing by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var moving by remember { mutableStateOf<NamedTool?>(null) }
  var renaming by remember { mutableStateOf<HomeSection?>(null) }
  val recent = savedRecent.split(ORDER_SEPARATOR).mapNotNull { id -> tools.firstOrNull { it.id == id } }
  val names = sectionNames(sections)
  val grid = rememberLazyGridState()
  val hasAppeared = rememberHasAppeared()
  var opened by rememberSaveable { mutableStateOf<String?>(null) }
  val reorder = remember(grid, scope) { HomeReorder(grid, scope) }
  fun save(key: String, value: String) {
    scope.launch { preferences.updateAppData(key, value) }
  }
  fun saveSections(changed: List<HomeSection>) = save(SECTIONS_KEY, HomeSection.encode(changed))
  val actions =
    TileActions(
      state = TileState(isEditing, animatesIn = !hasAppeared, opened = opened),
      reorder = reorder,
      onOpen = { section, tool ->
        opened = key(section, tool.id)
        save(RECENT_KEY, (listOf(tool.id) + recent.map { it.id }.filter { it != tool.id }).take(RECENT_COUNT).joinToString(ORDER_SEPARATOR))
        navigator.openTool(tool.item)
      },
      onHiddenChange = { tool, hide ->
        when {
          !hide -> save(DRAWER_LAYOUT_KEY, DrawerLayout(layout.shown + tool.item, layout.hidden - tool.item).save())
          // At least one tool stays
          tools.size > 1 -> save(DRAWER_LAYOUT_KEY, DrawerLayout(layout.shown - tool.item, layout.hidden + tool.item).save())
        }
      },
      onDrop = { id, order -> saveSections(sections.map { if (it.id == id) it.copy(tools = order) else it }) },
      onMove = { moving = it },
    )
  val edits =
    SectionEdits(
      onRename = { renaming = it },
      onMove = { section, step -> saveSections(sections.withSectionMoved(section.id, step)) },
      onDelete = { section -> saveSections(sections - section) },
    )

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.app_name)) },
    navigationIcon = { EditButton(isEditing) { isEditing = !isEditing } },
    keepNavigationIconOnWideScreens = true,
    actions = {
      IconButton(onClick = { navigator.openTool(SettingsDrawerItem) }) {
        Icon(Symbols.Settings, stringResource(SettingsDrawerItem.name))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      AnimatedVisibility(isEditing, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        EditBar(showsReset = savedSections.isNotEmpty()) { save(SECTIONS_KEY, "") }
      }
      val labels = HomeLabels(stringResource(Res.string.home_recent), stringResource(Res.string.home_no_tool), stringResource(Res.string.home_hidden))
      val newName = stringResource(Res.string.home_new_section)
      LazyVerticalGrid(
        modifier = Modifier.weight(1f),
        state = grid,
        columns = GridCells.Adaptive(KEY_WIDTH),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        val content = HomeContent(sections, names, tools, labels)
        if (isEditing) {
          editedSections(content, actions, edits)
          wide("add") {
            TextButton(onClick = { saveSections(sections + HomeSection(sections.newSectionId(), newName, emptyList())) }) {
              Icon(Symbols.Add, null)
              Text(stringResource(Res.string.home_add_section))
            }
          }
          if (hidden.isNotEmpty()) {
            header(labels.hidden)
            tiles(HIDDEN_SECTION, hidden, actions)
          }
        } else {
          wide("search") { ToolSearch(query) { query = it } }
          shownSections(content, query, recent, actions)
        }
      }
    }
  }
  SectionDialogs(moving, renaming, sections, names, ::saveSections) {
    moving = null
    renaming = null
  }
}

/** Moving a tool to another section, or renaming one, while one of them is chosen. */
@Composable
private fun SectionDialogs(
  moving: NamedTool?,
  renaming: HomeSection?,
  sections: List<HomeSection>,
  names: Map<String, String>,
  onSave: (List<HomeSection>) -> Unit,
  onClose: () -> Unit,
) {
  moving?.let { tool ->
    MoveDialog(tool, sections, names, onDismiss = onClose) { to ->
      onSave(sections.withToolMoved(tool.id, to))
      onClose()
    }
  }
  renaming?.let { section ->
    RenameDialog(names.getValue(section.id), onDismiss = onClose) { name ->
      onSave(sections.map { if (it.id == section.id) it.copy(name = name.ifBlank { null }) else it })
      onClose()
    }
  }
}

/** The name of every section: the one the user gave, or the name of its topic. */
@Composable
private fun sectionNames(sections: List<HomeSection>): Map<String, String> {
  val topics = Topic.entries.associate { it.name to stringResource(it.title) }
  val fallback = stringResource(Res.string.home_new_section)
  return sections.associate { it.id to (it.name ?: topics[it.id] ?: fallback) }
}

private class HomeLabels(val recent: String, val nothingFound: String, val hidden: String)

/** The sections, their names, the tools and the words around them. */
private class HomeContent(
  val sections: List<HomeSection>,
  val names: Map<String, String>,
  val tools: List<NamedTool>,
  val labels: HomeLabels,
) {
  fun toolsOf(section: HomeSection) = section.tools.mapNotNull { id -> tools.firstOrNull { it.id == id } }
}

/** What can be done to a section in the edit mode. */
private class SectionEdits(
  val onRename: (HomeSection) -> Unit,
  val onMove: (HomeSection, Int) -> Unit,
  val onDelete: (HomeSection) -> Unit,
)

/** Tiles rise into place when Home first shows, not each time they scroll back or a tool closes. */
@Composable
private fun rememberHasAppeared(): Boolean {
  var hasAppeared by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(MAX_APPEAR_DELAY + APPEAR_MILLIS)
    hasAppeared = true
  }
  return hasAppeared
}

@Composable
private fun EditButton(isEditing: Boolean, onClick: () -> Unit) {
  IconButton(onClick = onClick) {
    Crossfade(isEditing, label = "Edit button") { editing ->
      if (editing) Icon(Symbols.Check, stringResource(Res.string.common_ok))
      else Icon(Symbols.Edit, stringResource(Res.string.common_edit))
    }
  }
}

/** What editing does, and the way back to the first sections. */
@Composable
private fun EditBar(showsReset: Boolean, onReset: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      stringResource(Res.string.home_edit_sections_hint),
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (showsReset) TextButton(onClick = onReset) { Text(stringResource(Res.string.home_reset)) }
  }
}

/** The tools found, or the ones used last and every section that has tools. */
private fun LazyGridScope.shownSections(content: HomeContent, query: String, recent: List<NamedTool>, actions: TileActions) {
  val words = query.trim().lowercase()
  if (words.isNotEmpty()) {
    val found = content.tools.filter { words in it.name.lowercase() }
    if (found.isEmpty()) header(content.labels.nothingFound) else tiles(SEARCH_SECTION, found, actions)
    return
  }
  var placed = 0
  if (recent.isNotEmpty()) {
    header(content.labels.recent, appearsAt = placed.takeIf { actions.animatesIn })
    tiles(RECENT_SECTION, recent, actions, placed)
    placed += recent.size
  }
  content.sections.forEach { section ->
    val inSection = content.toolsOf(section)
    if (inSection.isNotEmpty()) {
      header(content.names.getValue(section.id), section.id, placed.takeIf { actions.animatesIn })
      tiles(section.id, inSection, actions, placed)
      placed += inSection.size
    }
  }
}

/** Every section, empty ones too, with what can be changed on its title. */
private fun LazyGridScope.editedSections(content: HomeContent, actions: TileActions, edits: SectionEdits) {
  content.sections.forEachIndexed { index, section ->
    val inSection = content.toolsOf(section)
    wide("title/${section.id}") {
      SectionTitle(
        name = content.names.getValue(section.id),
        canMoveUp = index > 0,
        canMoveDown = index < content.sections.lastIndex,
        canDelete = inSection.isEmpty() && content.sections.size > 1,
      ) { action ->
        when (action) {
          TitleAction.RENAME -> edits.onRename(section)
          TitleAction.UP -> edits.onMove(section, -1)
          TitleAction.DOWN -> edits.onMove(section, 1)
          TitleAction.DELETE -> edits.onDelete(section)
        }
      }
    }
    tiles(section.id, inSection, actions)
  }
}

private enum class TitleAction {
  RENAME,
  UP,
  DOWN,
  DELETE,
}

@Composable
private fun SectionTitle(name: String, canMoveUp: Boolean, canMoveDown: Boolean, canDelete: Boolean, onAction: (TitleAction) -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(
      name,
      modifier = Modifier.weight(1f).clickable { onAction(TitleAction.RENAME) }.padding(4.dp),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    IconButton(onClick = { onAction(TitleAction.RENAME) }) { Icon(Symbols.Edit, stringResource(Res.string.home_rename)) }
    IconButton(onClick = { onAction(TitleAction.UP) }, enabled = canMoveUp) {
      Icon(Symbols.KeyboardArrowUp, stringResource(Res.string.home_move_up))
    }
    IconButton(onClick = { onAction(TitleAction.DOWN) }, enabled = canMoveDown) {
      Icon(Symbols.KeyboardArrowDown, stringResource(Res.string.home_move_down))
    }
    if (canDelete) {
      IconButton(onClick = { onAction(TitleAction.DELETE) }) { Icon(Symbols.Delete, stringResource(Res.string.home_delete_section)) }
    }
  }
}

/** The sections a tool can go to: a tap moves it to the end of that one. */
@Composable
private fun MoveDialog(tool: NamedTool, sections: List<HomeSection>, names: Map<String, String>, onDismiss: () -> Unit, onMove: (String) -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("${stringResource(Res.string.home_move_to)}: ${tool.name}") },
    text = {
      Column {
        sections.forEach { section ->
          val isHere = tool.id in section.tools
          Text(
            names.getValue(section.id),
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(enabled = !isHere) { onMove(section.id) }.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isHere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isHere) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
  )
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
  var name by remember { mutableStateOf(current) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.home_rename)) },
    text = {
      OutlinedTextField(value = name, onValueChange = { name = it.take(MAX_NAME) }, singleLine = true, label = { Text(stringResource(Res.string.home_section_name)) })
    },
    confirmButton = { TextButton(onClick = { onRename(name.trim()) }) { Text(stringResource(Res.string.common_ok)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
  )
}

/** Something as wide as the grid. */
private fun LazyGridScope.wide(key: String, content: @Composable () -> Unit) {
  item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun ToolSearch(query: String, onChange: (String) -> Unit) {
  OutlinedTextField(
    value = query,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = { Text(stringResource(Res.string.home_search)) },
    leadingIcon = { Icon(Symbols.Search, null) },
    trailingIcon = {
      if (query.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Symbols.Close, stringResource(Res.string.common_clear)) }
    },
    singleLine = true,
    shape = MaterialTheme.shapes.extraLarge,
  )
}

private fun LazyGridScope.header(text: String, key: String = text, appearsAt: Int? = null) {
  item(key = "header/$key", span = { GridItemSpan(maxLineSpan) }) {
    Text(
      text,
      modifier = Modifier.animateItem().then(if (appearsAt != null) Modifier.appearing(appearsAt) else Modifier).padding(start = 4.dp, top = 8.dp),
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

private fun LazyGridScope.tiles(section: String, tools: List<NamedTool>, actions: TileActions, first: Int = 0) {
  val reorder = actions.reorder
  val shown =
    if (reorder.section == section) reorder.draft.mapNotNull { id -> tools.firstOrNull { it.id == id } } else tools
  itemsIndexed(shown, key = { _, tool -> key(section, tool.id) }) { index, tool ->
    val appearance = if (actions.animatesIn) Modifier.appearing(first + index) else Modifier
    ToolTile(section, tool, tools, actions, appearance.then(wiggle(actions.isEditing, first + index)))
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
  val colors = tileColors(tool.item)
  val keyPress = rememberKeyPress()
  val interactionSource = remember { MutableInteractionSource() }
  // The badge is outside of the key: keys clip what is in them
  // Opening a tool grows this tile into it. A tool has one tile to grow from: the one it was
  // opened from, else the one in its section rather than in the recent ones
  val growsIntoTool =
    !actions.isEditing &&
      when (actions.opened?.substringAfter(KEY_SEPARATOR)) {
        tool.id -> actions.opened == key(section, tool.id)
        else -> section != RECENT_SECTION
      }
  val shape = RoundedCornerShape(KEY_CORNER.dp)
  Box(modifier = placed.then(appearance).then(dragging).height(KEY_HEIGHT)) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .then(
            if (growsIntoTool) Modifier.sharedTool(sharedToolKey(tool.item.topLevelRoute.routeId), shape, isScreen = false)
            else Modifier
          )
          .alpha(if (isHidden) HIDDEN_ALPHA else 1f)
          .trackKeyPress(keyPress)
          // Only real taps vibrate, not a touch that starts a scroll
          .keyHaptics(keyPress)
          .squashable(
            // In the edit mode a tap moves the tool to another section
            onClick = { if (actions.isEditing) actions.onMove(tool) else actions.onOpen(section, tool) },
            enabled = !isHidden,
            interactionSource = interactionSource,
            cornerRadiusRange = KEY_CORNER..KEY_PRESSED_CORNER,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            isPressed = keyPress.isPressed,
            withHaptics = false,
          )
          .background(colors.first)
          .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Icon(tool.item.defaultIcon, null, modifier = Modifier.size(ICON_SIZE), tint = colors.second)
      Text(
        tool.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = colors.second,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
    AnimatedVisibility(
      actions.isEditing,
      modifier = Modifier.align(Alignment.TopEnd),
      enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(),
      exit = scaleOut() + fadeOut(),
    ) {
      EditBadge(
        modifier = Modifier.padding(2.dp),
        icon = if (isHidden) Symbols.Add else Symbols.Remove,
        label = stringResource(if (isHidden) Res.string.home_show else Res.string.home_hide),
        color = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        onClick = { actions.onHiddenChange(tool, !isHidden) },
      )
    }
  }
}

/**
 * Soft color of a tile and the darker one of its name, close tools share one. In the dark theme
 * the tile is dark and the name light.
 */
private class TilePalette(val light: Color, val onLight: Color, val dark: Color, val onDark: Color)

private val PURPLE = TilePalette(Color(0xFFE6DEFA), Color(0xFF4A2E8C), Color(0xFF3A3058), Color(0xFFDCCFFF))
private val GREEN = TilePalette(Color(0xFFD4EEE2), Color(0xFF1C5C42), Color(0xFF22453A), Color(0xFFB9EBD4))
private val PEACH = TilePalette(Color(0xFFFBE1CB), Color(0xFF8A4512), Color(0xFF4B3424), Color(0xFFFFD3B0))
private val PINK = TilePalette(Color(0xFFFBDCE5), Color(0xFF8E2547), Color(0xFF4D2833), Color(0xFFFFCAD9))
private val SKY = TilePalette(Color(0xFFD5ECF6), Color(0xFF1C5B77), Color(0xFF203F4C), Color(0xFFBDE6F7))

/** Tools by what they are for. Each topic has its color and its own place on Home. */
private enum class Topic(val title: StringResource, val palette: TilePalette, val items: List<DrawerItem>) {
  MATH(
    Res.string.home_math,
    PURPLE,
    listOf(
      CalculatorDrawerItem,
      FunctionsDrawerItem,
      SequencesDrawerItem,
      ProgrammerDrawerItem,
      IntegralsDrawerItem,
      VectorFieldsDrawerItem,
    ),
  ),
  // What most people need, right after the calculator
  EVERYDAY(
    Res.string.home_everyday,
    PEACH,
    listOf(ConverterDrawerItem, PricesDrawerItem, FinanceDrawerItem, DateCalculatorDrawerItem, BodyMassDrawerItem),
  ),
  DATA(Res.string.home_data, GREEN, listOf(StatisticsDrawerItem, RegressionDrawerItem, InferenceDrawerItem, ProbabilityDrawerItem)),
  SCIENCE(Res.string.home_science, SKY, listOf(ElementsDrawerItem, ProjectileDrawerItem, OrbitsDrawerItem, PlaceDrawerItem)),
  ELECTRONICS(Res.string.home_electronics, PINK, listOf(FiltersDrawerItem, ResistorsDrawerItem, ChargingDrawerItem, LogicDrawerItem)),
}

/** New tools that are in no topic yet go with the everyday ones. */
private fun topicOf(item: DrawerItem) = Topic.entries.firstOrNull { item in it.items } ?: Topic.EVERYDAY

/** Background and content of a tile: the calculator is the one in the color of the app. */
@Composable
private fun tileColors(item: DrawerItem): Pair<Color, Color> {
  val scheme = MaterialTheme.colorScheme
  val palette = topicOf(item).palette
  val isDark = scheme.surface.luminance() < HALF
  return if (isDark) palette.dark to palette.onDark else palette.light to palette.onLight
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

/** Keys gently shake while they can be moved, each at its own moment and neighbors the other way. */
@Composable
private fun wiggle(isEditing: Boolean, index: Int): Modifier {
  if (!isEditing) return Modifier
  val transition = rememberInfiniteTransition(label = "Editing tiles")
  val direction = if (index % 2 == 0) 1f else -1f
  val angle by
    transition.animateFloat(
      initialValue = -WIGGLE_ANGLE * direction,
      targetValue = WIGGLE_ANGLE * direction,
      animationSpec =
        infiniteRepeatable(
          tween(WIGGLE_MILLIS, easing = FastOutSlowInEasing),
          RepeatMode.Reverse,
          StartOffset(index * WIGGLE_STEP % WIGGLE_MILLIS),
        ),
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

private const val HIDDEN_SECTION = "hidden"
private const val SEARCH_SECTION = "search"
private const val RECENT_SECTION = "recent"
private const val RECENT_KEY = "HOME_RECENT"
private const val SECTIONS_KEY = "HOME_SECTIONS"
private const val MAX_NAME = 30
private const val RECENT_COUNT = 3
private const val KEY_SEPARATOR = "/"
private const val DRAGGED_SCALE = 1.08f
private const val HIDDEN_ALPHA = 0.5f
private const val WIGGLE_ANGLE = 1f
private const val WIGGLE_MILLIS = 150
private const val WIGGLE_STEP = 47
private const val APPEAR_MILLIS = 600L
private const val SCROLL_EDGE = 120
private const val SCROLL_STEP = 12f
private const val APPEAR_DELAY = 28L
private const val MAX_APPEAR_DELAY = 400L
private const val APPEAR_SCALE = 0.7f
private val APPEAR_RISE = 24.dp
private val KEY_WIDTH = 104.dp
private val KEY_HEIGHT = 100.dp
private val ICON_SIZE = 26.dp
private const val KEY_CORNER = 22
private const val KEY_PRESSED_CORNER = 32
private const val HALF = 0.5f
private val BADGE_SIZE = 22.dp
private val BADGE_ICON = 16.dp
private val BADGE_TOUCH = 36.dp
