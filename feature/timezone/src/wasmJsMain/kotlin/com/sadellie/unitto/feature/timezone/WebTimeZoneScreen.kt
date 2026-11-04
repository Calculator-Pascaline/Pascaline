/*
 * Unitto is a calculator for Android
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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.Edit
import com.sadellie.unitto.core.designsystem.icons.symbols.History
import com.sadellie.unitto.core.designsystem.icons.symbols.Schedule
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.ListItemExpressiveDefaults
import com.sadellie.unitto.core.ui.ProvideColor
import com.sadellie.unitto.core.ui.ProvideStyle
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.datetime.LocalPlatformDateFormatSettings
import com.sadellie.unitto.core.ui.datetimepicker.TimePickerDialog
import com.sadellie.unitto.core.ui.squashable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_add
import unitto.core.common.generated.resources.common_cancel
import unitto.core.common.generated.resources.common_delete
import unitto.core.common.generated.resources.common_label
import unitto.core.common.generated.resources.common_next
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.common_select_time
import unitto.core.common.generated.resources.common_tomorrow
import unitto.core.common.generated.resources.common_yesterday
import unitto.core.common.generated.resources.time_zone_add_title
import unitto.core.common.generated.resources.time_zone_reset
import unitto.core.common.generated.resources.time_zone_title
import unitto.core.common.generated.resources.unit_hour_short
import unitto.core.common.generated.resources.unit_minute_short

/** What the dialog asks, like on Android. */
private sealed class WebTimeZoneDialog {
  data object Nothing : WebTimeZoneDialog()

  data object UserTime : WebTimeZoneDialog()

  /** Comes after [UserTime], to change the day too. */
  data object UserDate : WebTimeZoneDialog()

  data class FavoriteTime(val zone: String) : WebTimeZoneDialog()

  data class Label(val favorite: WebFavoriteZone) : WebTimeZoneDialog()
}

/**
 * Time zones on the web: the browser's time (or a chosen one) at the top, and the same moment in
 * the favorite zones. [navigateToAddTimeZone] gets that moment, in milliseconds.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WebTimeZoneScreen(openDrawer: () -> Unit, navigateToAddTimeZone: (Double) -> Unit) {
  val userZone = remember { WebZones.userZone() }
  val is24Hour = LocalPlatformDateFormatSettings.current.is24Hour
  // null follows the clock
  var customMillis by remember { mutableStateOf<Double?>(null) }
  var nowMillis by remember { mutableStateOf(WebZones.now()) }
  val userMillis = customMillis ?: nowMillis
  var selected by remember { mutableStateOf<String?>(null) }
  var dialog by remember { mutableStateOf<WebTimeZoneDialog>(WebTimeZoneDialog.Nothing) }

  LaunchedEffect(customMillis) {
    while (customMillis == null && isActive) {
      nowMillis = WebZones.now()
      delay(USER_TIME_UPDATE_FREQUENCY_MS)
    }
  }

  val favorites = WebFavoriteZones.favorites.collectAsState().value
  var orderedFavorites by remember(favorites) { mutableStateOf(favorites) }
  val lazyListState = rememberLazyListState()
  val reorderableLazyListState =
    rememberReorderableLazyListState(
      lazyListState = lazyListState,
      onMove = { from, to ->
        // -1 because of the user's time above
        orderedFavorites =
          orderedFavorites.toMutableList().apply { add(to.index - 1, removeAt(from.index - 1)) }
      },
    )

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.time_zone_title)) },
    navigationIcon = { DrawerButton(openDrawer) },
    floatingActionButton = {
      LargeFloatingActionButton(onClick = { navigateToAddTimeZone(userMillis) }) {
        Icon(
          imageVector = Symbols.Add,
          contentDescription = stringResource(Res.string.time_zone_add_title),
          modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize),
        )
      }
    },
    floatingActionButtonPosition = FabPosition.Center,
    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
  ) { padding ->
    LazyColumn(
      state = lazyListState,
      modifier = Modifier.fillMaxSize(),
      contentPadding =
        padding + PaddingValues(start = Sizes.large, end = Sizes.large, bottom = 124.dp),
      verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
    ) {
      item(key = "user time") {
        WebUserTimeZone(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          millis = userMillis,
          zone = userZone,
          is24Hour = is24Hour,
          onClick = { dialog = WebTimeZoneDialog.UserTime },
          onResetClick = { customMillis = null },
          showReset = customMillis != null,
        )
      }

      itemsIndexed(items = orderedFavorites, key = { _, item -> item.zone }) { index, item ->
        ReorderableItem(reorderableLazyListState, item.zone) { isDragging ->
          val isSelected = selected == item.zone
          WebFavoriteTimeZoneItem(
            modifier =
              Modifier.draggedZoneItem(
                  isDragging,
                  ListItemExpressiveDefaults.listedShaped(index, orderedFavorites.size),
                )
                .longPressDraggableHandle(
                  onDragStopped = { WebFavoriteZones.reorder(orderedFavorites) }
                ),
            item = item,
            userMillis = userMillis,
            userZone = userZone,
            is24Hour = is24Hour,
            expanded = isSelected,
            isDragging = isDragging,
            onClick = { selected = if (isSelected) null else item.zone },
            onDelete = { WebFavoriteZones.remove(item.zone) },
            onLabelClick = { dialog = WebTimeZoneDialog.Label(item) },
            onSelectTimeClick = { dialog = WebTimeZoneDialog.FavoriteTime(item.zone) },
          )
        }
      }
    }
  }

  WebTimeZoneDialogs(
    dialog = dialog,
    userMillis = userMillis,
    userZone = userZone,
    setUserMillis = { customMillis = it },
    setDialog = { dialog = it },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WebTimeZoneDialogs(
  dialog: WebTimeZoneDialog,
  userMillis: Double,
  userZone: String,
  setUserMillis: (Double) -> Unit,
  setDialog: (WebTimeZoneDialog) -> Unit,
) {
  val close = { setDialog(WebTimeZoneDialog.Nothing) }
  when (dialog) {
    WebTimeZoneDialog.UserTime -> {
      val time = WebZones.local(userMillis, userZone)
      TimePickerDialog(
        hour = time.hour,
        minute = time.minute,
        onConfirm = { hour, minute ->
          setUserMillis(WebZones.millisOf(time.copy(hour = hour, minute = minute), userZone))
          setDialog(WebTimeZoneDialog.UserDate)
        },
        onCancel = close,
        confirmLabel = stringResource(Res.string.common_next),
      )
    }

    WebTimeZoneDialog.UserDate -> {
      val time = WebZones.local(userMillis, userZone)
      // The picker works in UTC days
      val pickerState =
        rememberDatePickerState(
          initialSelectedDateMillis =
            WebZones.millisOf(time.copy(hour = 0, minute = 0), "UTC").toLong(),
          yearRange = 1..9_999,
        )
      DatePickerDialog(
        onDismissRequest = close,
        confirmButton = {
          TextButton(
            onClick = {
              val picked = pickerState.selectedDateMillis ?: return@TextButton
              val day = WebZones.local(picked.toDouble(), "UTC")
              setUserMillis(
                WebZones.millisOf(
                  time.copy(year = day.year, month = day.month, day = day.day),
                  userZone,
                )
              )
              close()
            }
          ) {
            Text(stringResource(Res.string.common_ok))
          }
        },
        dismissButton = { TextButton(onClick = close) { Text(stringResource(Res.string.common_cancel)) } },
      ) {
        DatePicker(pickerState)
      }
    }

    is WebTimeZoneDialog.FavoriteTime -> {
      // The hour picked is in that zone, on its day
      val time = WebZones.local(userMillis, dialog.zone)
      TimePickerDialog(
        hour = time.hour,
        minute = time.minute,
        onConfirm = { hour, minute ->
          setUserMillis(WebZones.millisOf(time.copy(hour = hour, minute = minute), dialog.zone))
          close()
        },
        onCancel = close,
      )
    }

    is WebTimeZoneDialog.Label -> {
      val focusRequester = remember { FocusRequester() }
      val label = remember(dialog) { TextFieldState(dialog.favorite.label) }
      AlertDialog(
        title = { Text(text = stringResource(Res.string.common_label)) },
        text = {
          OutlinedTextField(state = label, modifier = Modifier.focusRequester(focusRequester))
          LaunchedEffect(Unit) { focusRequester.requestFocus() }
        },
        confirmButton = {
          TextButton(
            onClick = {
              WebFavoriteZones.updateLabel(dialog.favorite.zone, label.text.toString())
              close()
            },
            content = { Text(text = stringResource(Res.string.common_ok)) },
            shapes = ButtonDefaults.shapes(),
          )
        },
        dismissButton = {
          TextButton(
            onClick = close,
            content = { Text(text = stringResource(Res.string.common_cancel)) },
            shapes = ButtonDefaults.shapes(),
          )
        },
        onDismissRequest = close,
      )
    }

    WebTimeZoneDialog.Nothing -> Unit
  }
}

/** The big clock at the top. A click picks a time, a long press goes back to now. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WebUserTimeZone(
  modifier: Modifier,
  millis: Double,
  zone: String,
  is24Hour: Boolean,
  onClick: () -> Unit,
  onResetClick: () -> Unit,
  showReset: Boolean,
) {
  Row(
    modifier =
      modifier
        .squashable(
          onClick = onClick,
          onLongClick = onResetClick,
          cornerRadiusRange = Sizes.small..Sizes.extraLarge,
          interactionSource = remember { MutableInteractionSource() },
        )
        .background(MaterialTheme.colorScheme.tertiaryContainer)
        .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        text = WebZones.name(zone),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Row(verticalAlignment = Alignment.Bottom) {
        SlidingText(WebZones.formatHours(millis, zone, is24Hour))
        SlidingText(":")
        SlidingText(WebZones.formatMinutes(millis, zone))
        Spacer(Modifier.padding(4.dp))
        if (!is24Hour) SlidingText(WebZones.formatAmPm(millis, zone))
      }
      Text(
        text = WebZones.formatDate(millis, zone),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
    AnimatedVisibility(
      visible = showReset,
      enter = scaleIn() + fadeIn(),
      exit = scaleOut() + fadeOut(),
    ) {
      IconButton(onClick = onResetClick, shapes = IconButtonDefaults.shapes()) {
        Icon(
          imageVector = Symbols.History,
          contentDescription = stringResource(Res.string.time_zone_reset),
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }
  }
}

@Composable
private fun SlidingText(text: String) {
  AnimatedContent(
    targetState = text,
    label = "user time change",
    transitionSpec = {
      slideInVertically { height -> height } + fadeIn() togetherWith
        slideOutVertically { height -> -height } + fadeOut() using
        SizeTransform()
    },
  ) { target ->
    Text(
      text = target,
      style = MaterialTheme.typography.displayLarge,
      color = MaterialTheme.colorScheme.onTertiaryContainer,
      overflow = TextOverflow.Visible,
      maxLines = 1,
    )
  }
}

@Composable
private fun WebFavoriteTimeZoneItem(
  modifier: Modifier,
  item: WebFavoriteZone,
  userMillis: Double,
  userZone: String,
  is24Hour: Boolean,
  expanded: Boolean,
  isDragging: Boolean,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  onLabelClick: () -> Unit,
  onSelectTimeClick: () -> Unit,
) {
  var deleteAnimationRunning by remember { mutableStateOf(false) }
  val animatedAlpha by
    animateFloatAsState(
      label = "delete animation",
      targetValue = if (deleteAnimationRunning) 0f else 1f,
      finishedListener = { if (it == 0f) onDelete() },
    )
  val difference =
    formatDifference(
      zoneMillis = WebZones.offsetMillis(userMillis, item.zone),
      userMillis = WebZones.offsetMillis(userMillis, userZone),
      days = WebZones.local(userMillis, item.zone).daysFrom(WebZones.local(userMillis, userZone)),
      hourLabel = stringResource(Res.string.unit_hour_short),
      minuteLabel = stringResource(Res.string.unit_minute_short),
      tomorrowLabel = stringResource(Res.string.common_tomorrow),
      yesterdayLabel = stringResource(Res.string.common_yesterday),
    )

  Column(
    modifier =
      Modifier.graphicsLayer(alpha = animatedAlpha)
        .then(modifier)
        .clickable(enabled = !isDragging) { onClick() }
        .padding(vertical = 16.dp, horizontal = 12.dp)
  ) {
    WebTimeZoneLabel(label = item.label, expanded = expanded, onLabelClick = onLabelClick)
    Row(
      modifier = Modifier.heightIn(min = 56.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Column(
        modifier = Modifier.weight(1f).padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = WebZones.region(item.zone),
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedVisibility(visible = difference != null, label = "Nullable offset") {
          Text(
            text = difference ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      AnimatedContent(
        targetState = WebZones.formatTime(userMillis, item.zone, is24Hour),
        label = "Time change",
        transitionSpec = { fadeIn() togetherWith fadeOut() using (SizeTransform(clip = false)) },
      ) { time ->
        Text(
          text = time,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    AnimatedVisibility(visible = expanded) {
      Column {
        WebTimeZoneOption(
          title = stringResource(Res.string.common_select_time),
          icon = Symbols.Schedule,
          onClick = onSelectTimeClick,
        )
        WebTimeZoneOption(
          title = stringResource(Res.string.common_delete),
          icon = Symbols.Delete,
          onClick = { deleteAnimationRunning = true },
        )
      }
    }
  }
}

/** Like "+5h 30min, tomorrow", null for the same time as the user. */
private fun formatDifference(
  zoneMillis: Double,
  userMillis: Double,
  days: Int,
  hourLabel: String,
  minuteLabel: String,
  tomorrowLabel: String,
  yesterdayLabel: String,
): String? {
  val minutes = ((zoneMillis - userMillis) / 60_000).toInt()
  if (minutes == 0) return null
  val hour = kotlin.math.abs(minutes) / 60
  val minute = kotlin.math.abs(minutes) % 60
  var result = if (minutes > 0) "+" else "-"
  if (hour != 0) result += "$hour$hourLabel"
  if (minute != 0) {
    if (hour != 0) result += " "
    result += "$minute$minuteLabel"
  }
  when {
    days > 0 -> result += ", ${tomorrowLabel.lowercase()}"
    days < 0 -> result += ", ${yesterdayLabel.lowercase()}"
  }
  return result
}

@Composable
private fun WebTimeZoneOption(title: String, icon: ImageVector, onClick: () -> Unit) =
  ProvideColor(MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
      modifier =
        Modifier.clip(MaterialTheme.shapes.small)
          .clickable { onClick() }
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(imageVector = icon, contentDescription = title)
      Text(text = title)
    }
  }

@Composable
private fun WebTimeZoneLabel(label: String, expanded: Boolean, onLabelClick: () -> Unit) =
  ProvideStyle(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textStyle = MaterialTheme.typography.bodyMedium,
  ) {
    AnimatedContent(
      label = "Expand animation",
      targetState = label.isBlank(),
      modifier = if (expanded) Modifier.clickable { onLabelClick() } else Modifier,
    ) { blank ->
      if (blank) {
        AnimatedVisibility(expanded) {
          Row(modifier = Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Not required because there is text next to it
            Icon(Symbols.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(text = stringResource(Res.string.common_add))
          }
        }
      } else {
        Row(modifier = Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
          AnimatedVisibility(visible = expanded) {
            Icon(Symbols.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
          }
          Text(text = label)
        }
      }
    }
  }

private const val USER_TIME_UPDATE_FREQUENCY_MS = 5_000L
