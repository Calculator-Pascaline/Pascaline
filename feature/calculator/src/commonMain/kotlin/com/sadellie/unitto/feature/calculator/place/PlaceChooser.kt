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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.designsystem.icons.symbols.Event
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_today
import unitto.core.common.generated.resources.place_continue

/**
 * The first step of Place: the map fills the screen and a sheet over it holds the chosen city and
 * its date, then [onContinue] opens the sun, moon and everything else.
 */
@Composable
internal fun PlaceChooser(
  viewModel: PlaceViewModel,
  language: String,
  open: (PlaceDialog) -> Unit,
  onContinue: () -> Unit,
) {
  var sheetHeight by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= WIDE_WIDTH
    // On a phone the map ends under the sheet, so that the pin and the zoom buttons stay in sight
    val hidden = if (isWide) 0.dp else with(density) { sheetHeight.toDp() } - SHEET_CORNER
    WorldMap(
      land = viewModel.land,
      cities = viewModel.cities,
      language = language,
      point = viewModel.point,
      focus = viewModel.focus,
      onChoose = viewModel::choosePoint,
      modifier = Modifier.fillMaxSize().padding(bottom = hidden.coerceAtLeast(0.dp)),
      buttonsBottom = if (isWide) 8.dp else SHEET_CORNER + 8.dp,
    )
    CitySearch(
      cities = viewModel.cities,
      language = language,
      onChoose = viewModel::chooseCity,
      modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).widthIn(max = SHEET_WIDTH),
    )
    Surface(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(if (isWide) 16.dp else 0.dp)
          .widthIn(max = if (isWide) SHEET_WIDTH else maxWidth)
          .onSizeChanged { sheetHeight = it.height },
      shape =
        if (isWide) RoundedCornerShape(SHEET_CORNER)
        else RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER),
      shadowElevation = 8.dp,
    ) {
      Column(
        modifier = Modifier.navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        CityHeader(viewModel, language, open)
        DayChips(viewModel, open)
        Button(
          onClick = onContinue,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = PaddingValues(vertical = 16.dp),
        ) {
          Text(stringResource(Res.string.place_continue), style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}

/** The name of the place in big, its coordinates open their dialog. */
@Composable
private fun CityHeader(viewModel: PlaceViewModel, language: String, open: (PlaceDialog) -> Unit) {
  val coordinates = formatPoint(viewModel.point)
  Text(viewModel.nearCity?.name(language) ?: coordinates, style = MaterialTheme.typography.headlineMedium)
  AssistChip(onClick = { open(PlaceDialog.COORDINATES) }, label = { Text(coordinates) })
}

/** Today and the days after it in one tap, the calendar for any other. */
@Composable
private fun DayChips(viewModel: PlaceViewModel, open: (PlaceDialog) -> Unit) {
  val zone = viewModel.zone
  val date = viewModel.date
  val today = remember(zone) { viewModel.today() }
  val days = remember(today) { List(DAY_CHIPS) { today.plus(it, DateTimeUnit.DAY) } }
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    days.forEach { day ->
      FilterChip(
        selected = day == date,
        onClick = { viewModel.chooseDate(day) },
        label = {
          val noon = dayStart(day, zone) + NOON
          Text(if (day == today) stringResource(Res.string.common_today) else formatShortDay(noon, zone))
        },
      )
    }
    if (date in days) {
      FilledTonalIconButton(onClick = { open(PlaceDialog.DATE) }) {
        Icon(Symbols.Event, formatDate(dayStart(date, zone) + NOON, zone))
      }
    } else {
      FilterChip(
        selected = true,
        onClick = { open(PlaceDialog.DATE) },
        label = { Text(formatDate(dayStart(date, zone) + NOON, zone)) },
        leadingIcon = { Icon(Symbols.Event, null) },
      )
    }
  }
}

private val WIDE_WIDTH = 720.dp
private val SHEET_WIDTH = 480.dp
private val SHEET_CORNER = 28.dp
private const val DAY_CHIPS = 4
