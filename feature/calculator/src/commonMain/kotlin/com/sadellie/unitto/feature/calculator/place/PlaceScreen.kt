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

package com.sadellie.unitto.feature.calculator.place

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlin.math.abs
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_cancel
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.common_search_text_field_placeholder
import unitto.core.common.generated.resources.place_coordinates
import unitto.core.common.generated.resources.place_credits
import unitto.core.common.generated.resources.place_latitude
import unitto.core.common.generated.resources.place_loading_error
import unitto.core.common.generated.resources.place_longitude
import unitto.core.common.generated.resources.place_map_hint
import unitto.core.common.generated.resources.place_tab_elevation
import unitto.core.common.generated.resources.place_tab_moon
import unitto.core.common.generated.resources.place_tab_rain
import unitto.core.common.generated.resources.place_tab_sky
import unitto.core.common.generated.resources.place_tab_sun
import unitto.core.common.generated.resources.place_tab_temperature
import unitto.core.common.generated.resources.place_tab_tides
import unitto.core.common.generated.resources.place_time_zone
import unitto.core.common.generated.resources.place_title

@Composable
internal fun PlaceRoute(openDrawer: () -> Unit) {
  val viewModel: PlaceViewModel = koinViewModel()
  val symbols = viewModel.formatterSymbols.collectAsStateWithLifecycleKMP().value
  if (symbols == null) {
    EmptyScreen()
    return
  }
  PlaceScreen(openDrawer, viewModel, symbols)
}

/**
 * Choose a point on the map, a date and a time zone, then read one chip at a time: a big answer,
 * a graph to drag along and the details. Everything is worked out on the device.
 */
@Composable
private fun PlaceScreen(openDrawer: () -> Unit, viewModel: PlaceViewModel, symbols: FormatterSymbols) {
  var dialog by remember { mutableStateOf<PlaceDialog?>(null) }
  val units = rememberPlaceUnits(viewModel.isMetric, symbols)
  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.place_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    actions = {
      TextButton(onClick = { viewModel.isMetric = !viewModel.isMetric }) { Text(units.temperatureUnit) }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val isWide = maxWidth >= WIDE_LAYOUT_WIDTH
      val chooser: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          WorldMap(
            land = viewModel.land,
            point = viewModel.point,
            onChoose = viewModel::choosePoint,
            modifier = Modifier.fillMaxWidth().height(if (isWide) WIDE_MAP_HEIGHT else MAP_HEIGHT),
          )
          Text(
            stringResource(Res.string.place_map_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          PlaceChips(viewModel) { dialog = it }
        }
      }
      val results: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          TabChips(viewModel.tab) { viewModel.tab = it }
          viewModel.error?.let {
            Text(
              stringResource(Res.string.place_loading_error, it),
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
          val report = viewModel.report
          if (report != null) PlaceTabContent(viewModel.tab, report, viewModel, units)
          Text(
            stringResource(Res.string.place_credits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      val scrolling = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
      if (isWide) {
        Row(modifier = scrolling, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          chooser(Modifier.weight(1f))
          results(Modifier.weight(1f))
        }
      } else {
        Column(modifier = scrolling, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          chooser(Modifier.fillMaxWidth())
          results(Modifier.fillMaxWidth())
        }
      }
    }
  }
  PlaceDialogs(dialog, viewModel) { dialog = null }
}

private enum class PlaceDialog {
  COORDINATES,
  DATE,
  ZONE,
}

/** The point, the date and the zone, each opens its dialog. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceChips(viewModel: PlaceViewModel, open: (PlaceDialog) -> Unit) {
  val point = viewModel.point
  val date = viewModel.date
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    AssistChip(onClick = { open(PlaceDialog.COORDINATES) }, label = { Text(formatPoint(point)) })
    AssistChip(
      onClick = { open(PlaceDialog.DATE) },
      label = { Text(formatDate(dayStart(date, viewModel.zone) + NOON, viewModel.zone)) },
    )
    AssistChip(onClick = { open(PlaceDialog.ZONE) }, label = { Text(viewModel.zone.replace('_', ' ')) })
  }
}

/** 48.86° N, 2.35° E */
internal fun formatPoint(point: GeoPoint): String {
  fun part(value: Double, positive: Char, negative: Char): String {
    val rounded = (abs(value) * 100).toLong()
    val text = "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}"
    return "$text° ${if (value >= 0) positive else negative}"
  }
  return "${part(point.latitude, 'N', 'S')}, ${part(point.longitude, 'E', 'W')}"
}

@Composable
private fun TabChips(selected: PlaceTab, onSelect: (PlaceTab) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PlaceTab.entries.forEach { tab ->
      FilterChip(selected = tab == selected, onClick = { onSelect(tab) }, label = { Text(stringResource(tab.title)) })
    }
  }
}

private val PlaceTab.title: StringResource
  get() =
    when (this) {
      PlaceTab.SUN -> Res.string.place_tab_sun
      PlaceTab.MOON -> Res.string.place_tab_moon
      PlaceTab.SKY -> Res.string.place_tab_sky
      PlaceTab.TIDES -> Res.string.place_tab_tides
      PlaceTab.TEMPERATURE -> Res.string.place_tab_temperature
      PlaceTab.RAIN -> Res.string.place_tab_rain
      PlaceTab.ELEVATION -> Res.string.place_tab_elevation
    }

@Composable
private fun PlaceDialogs(dialog: PlaceDialog?, viewModel: PlaceViewModel, close: () -> Unit) {
  when (dialog) {
    PlaceDialog.COORDINATES -> CoordinatesDialog(viewModel.point, { viewModel.choosePoint(it) }, close)
    PlaceDialog.DATE -> DateDialog(viewModel.date, viewModel::chooseDate, close)
    PlaceDialog.ZONE -> ZoneDialog(viewModel::chooseZone, close)
    null -> Unit
  }
}

@Composable
private fun CoordinatesDialog(point: GeoPoint, onConfirm: (GeoPoint) -> Unit, close: () -> Unit) {
  var latitude by remember { mutableStateOf(point.latitude.toString().take(COORDINATE_LENGTH)) }
  var longitude by remember { mutableStateOf(point.longitude.toString().take(COORDINATE_LENGTH)) }
  val lat = latitude.toNumberOrNull()?.takeIf { it in -90.0..90.0 }
  val lon = longitude.toNumberOrNull()?.takeIf { it in -180.0..180.0 }
  AlertDialog(
    onDismissRequest = close,
    title = { Text(stringResource(Res.string.place_coordinates)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoordinateField(latitude, { latitude = it }, stringResource(Res.string.place_latitude), lat == null)
        CoordinateField(longitude, { longitude = it }, stringResource(Res.string.place_longitude), lon == null)
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (lat != null && lon != null) {
            onConfirm(GeoPoint(lat, lon))
            close()
          }
        }
      ) {
        Text(stringResource(Res.string.common_ok))
      }
    },
    dismissButton = { TextButton(onClick = close) { Text(stringResource(Res.string.common_cancel)) } },
  )
}

@Composable
private fun CoordinateField(value: String, onChange: (String) -> Unit, label: String, isError: Boolean) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    singleLine = true,
    isError = isError,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(date: LocalDate, onConfirm: (LocalDate) -> Unit, close: () -> Unit) {
  val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
  DatePickerDialog(
    onDismissRequest = close,
    confirmButton = {
      TextButton(
        onClick = {
          state.selectedDateMillis?.let {
            onConfirm(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
          }
          close()
        }
      ) {
        Text(stringResource(Res.string.common_ok))
      }
    },
    dismissButton = { TextButton(onClick = close) { Text(stringResource(Res.string.common_cancel)) } },
  ) {
    DatePicker(state)
  }
}

@Composable
private fun ZoneDialog(onChoose: (String) -> Unit, close: () -> Unit) {
  val zones = remember { knownZones() }
  var search by remember { mutableStateOf("") }
  val shown = remember(search) { zones.filter { it.replace('_', ' ').contains(search.trim(), ignoreCase = true) } }
  AlertDialog(
    onDismissRequest = close,
    title = { Text(stringResource(Res.string.place_time_zone)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = search,
          onValueChange = { search = it },
          placeholder = { Text(stringResource(Res.string.common_search_text_field_placeholder)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(modifier = Modifier.heightIn(max = ZONE_LIST_HEIGHT)) {
          items(shown) { zone ->
            ListItem(
              headlineContent = { Text(zone.replace('_', ' ')) },
              modifier =
                Modifier.fillMaxWidth().clickable {
                  onChoose(zone)
                  close()
                },
            )
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = { TextButton(onClick = close) { Text(stringResource(Res.string.common_cancel)) } },
  )
}

private val WIDE_LAYOUT_WIDTH = 720.dp
private val MAP_HEIGHT = 220.dp
private val WIDE_MAP_HEIGHT = 320.dp
private val ZONE_LIST_HEIGHT = 360.dp
private const val NOON = 43_200_000L
private const val COORDINATE_LENGTH = 9
