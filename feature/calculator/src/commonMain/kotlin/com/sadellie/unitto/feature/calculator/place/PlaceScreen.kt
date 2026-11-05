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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.LocalLocale
import com.sadellie.unitto.core.designsystem.icons.symbols.ArrowBack
import com.sadellie.unitto.core.designsystem.icons.symbols.Close
import com.sadellie.unitto.core.designsystem.icons.symbols.Search
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.BackHandler
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_cancel
import unitto.core.common.generated.resources.common_clear
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.place_coordinates
import unitto.core.common.generated.resources.place_credits
import unitto.core.common.generated.resources.place_latitude
import unitto.core.common.generated.resources.place_loading_error
import unitto.core.common.generated.resources.place_longitude
import unitto.core.common.generated.resources.place_no_city
import unitto.core.common.generated.resources.place_real_elevation
import unitto.core.common.generated.resources.place_real_moon
import unitto.core.common.generated.resources.place_real_rain
import unitto.core.common.generated.resources.place_real_sky
import unitto.core.common.generated.resources.place_real_sun
import unitto.core.common.generated.resources.place_real_temperature
import unitto.core.common.generated.resources.place_real_tides
import unitto.core.common.generated.resources.place_search_city
import unitto.core.common.generated.resources.place_tab_elevation
import unitto.core.common.generated.resources.place_tab_moon
import unitto.core.common.generated.resources.place_tab_rain
import unitto.core.common.generated.resources.place_tab_sky
import unitto.core.common.generated.resources.place_tab_sun
import unitto.core.common.generated.resources.place_tab_temperature
import unitto.core.common.generated.resources.place_tab_tides
import unitto.core.common.generated.resources.place_title
import unitto.core.common.generated.resources.probability_back

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
 * First the city and the date, on [PlaceChooser], then one chip at a time: a big answer, a graph to
 * drag along and the details. Everything is worked out on the device.
 */
@Composable
private fun PlaceScreen(openDrawer: () -> Unit, viewModel: PlaceViewModel, symbols: FormatterSymbols) {
  var dialog by remember { mutableStateOf<PlaceDialog?>(null) }
  var isChoosing by rememberSaveable { mutableStateOf(true) }
  val language = LocalLocale.current.language
  val units = rememberPlaceUnits(viewModel.isMetric, symbols)
  LaunchedEffect(viewModel.cities, language) { viewModel.startAt(language) }
  BackHandler(enabled = !isChoosing) { isChoosing = true }
  ScaffoldWithTopBar(
    title = {
      val city = viewModel.nearCity?.name(language)
      Text(if (isChoosing || city == null) stringResource(Res.string.place_title) else city)
    },
    navigationIcon = {
      if (isChoosing) {
        DrawerButton(onClick = openDrawer)
      } else {
        IconButton(onClick = { isChoosing = true }) {
          Icon(Symbols.ArrowBack, stringResource(Res.string.probability_back))
        }
      }
    },
    keepNavigationIconOnWideScreens = !isChoosing,
    actions = {
      if (!isChoosing) {
        TextButton(onClick = { viewModel.isMetric = !viewModel.isMetric }) { Text(units.temperatureUnit) }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    AnimatedContent(
      targetState = isChoosing,
      modifier = Modifier.padding(paddingValues).fillMaxSize(),
      transitionSpec = {
        val direction = if (targetState) -1 else 1
        (slideInHorizontally { it * direction } + fadeIn()) togetherWith
          (slideOutHorizontally { -it * direction } + fadeOut())
      },
    ) { choosing ->
      if (choosing) {
        PlaceChooser(viewModel, language, { dialog = it }) { isChoosing = false }
      } else {
        PlaceMenu(viewModel, units)
      }
    }
  }
  PlaceDialogs(dialog, viewModel) { dialog = null }
}

/** What the place is like on the chosen date, one chip at a time. */
@Composable
private fun PlaceMenu(viewModel: PlaceViewModel, units: PlaceUnits) {
  Box(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = MENU_WIDTH).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      TabChips(viewModel.tab) { viewModel.tab = it }
      viewModel.error?.let {
        Text(
          stringResource(Res.string.place_loading_error, it),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      val report = viewModel.report
      if (report != null) {
        PlaceTabContent(viewModel.tab, report, viewModel, units)
        RealLifeNote(viewModel.tab.realLife)
      }
      Text(
        stringResource(Res.string.place_credits),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

internal enum class PlaceDialog {
  COORDINATES,
  DATE,
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

/** A search box, the cities found under it, a tap chooses one and flies the map there. */
@Composable
internal fun CitySearch(cities: List<City>, language: String, onChoose: (City) -> Unit, modifier: Modifier = Modifier) {
  var query by remember { mutableStateOf("") }
  val focusManager = LocalFocusManager.current
  val results = remember(query, cities, language) { searchCities(cities, query, language, SEARCH_RESULTS) }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text(stringResource(Res.string.place_search_city)) },
      leadingIcon = { Icon(Symbols.Search, null) },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { query = "" }) { Icon(Symbols.Close, stringResource(Res.string.common_clear)) }
        }
      },
      singleLine = true,
      shape = MaterialTheme.shapes.extraLarge,
      // Over the map
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
    if (query.isNotBlank()) {
      Column(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer)
      ) {
        if (results.isEmpty()) {
          Text(
            stringResource(Res.string.place_no_city),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        results.forEach { city ->
          ListItem(
            headlineContent = { Text(city.name(language)) },
            supportingContent = { Text(city.zone.replace('_', ' ')) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier =
              Modifier.fillMaxWidth().clickable {
                onChoose(city)
                query = ""
                focusManager.clearFocus()
              },
          )
        }
      }
    }
  }
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

private val PlaceTab.realLife: StringResource
  get() =
    when (this) {
      PlaceTab.SUN -> Res.string.place_real_sun
      PlaceTab.MOON -> Res.string.place_real_moon
      PlaceTab.SKY -> Res.string.place_real_sky
      PlaceTab.TIDES -> Res.string.place_real_tides
      PlaceTab.TEMPERATURE -> Res.string.place_real_temperature
      PlaceTab.RAIN -> Res.string.place_real_rain
      PlaceTab.ELEVATION -> Res.string.place_real_elevation
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

private val MENU_WIDTH = 720.dp
internal const val NOON = 43_200_000L
private const val COORDINATE_LENGTH = 9
private const val SEARCH_RESULTS = 6
