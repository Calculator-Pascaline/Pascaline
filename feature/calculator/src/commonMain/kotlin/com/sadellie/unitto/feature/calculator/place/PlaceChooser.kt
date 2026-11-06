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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.designsystem.icons.symbols.Event
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.place_continue

/**
 * The first step of Place: the map fills the screen, a slim bar over it holds the chosen city, its
 * date and [onContinue], which opens the sun, moon and everything else.
 */
@Composable
internal fun PlaceChooser(
  viewModel: PlaceViewModel,
  language: String,
  onDateClick: () -> Unit,
  onContinue: () -> Unit,
) {
  var barHeight by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current
  Box(modifier = Modifier.fillMaxSize()) {
    WorldMap(
      land = viewModel.land,
      cities = viewModel.cities,
      language = language,
      point = viewModel.point,
      focus = viewModel.focus,
      onChoose = viewModel::choosePoint,
      modifier = Modifier.fillMaxSize(),
      buttonsBottom = with(density) { barHeight.toDp() } + BAR_MARGIN * 2,
    )
    CitySearch(
      cities = viewModel.cities,
      language = language,
      onChoose = viewModel::chooseCity,
      modifier = Modifier.align(Alignment.TopCenter).padding(BAR_MARGIN).widthIn(max = BAR_WIDTH),
    )
    Surface(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(BAR_MARGIN)
          .widthIn(max = BAR_WIDTH)
          .onSizeChanged { barHeight = it.height },
      shape = RoundedCornerShape(BAR_CORNER),
      shadowElevation = 6.dp,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        CityAndDate(viewModel, language, onDateClick, Modifier.weight(1f))
        Button(onClick = onContinue) { Text(stringResource(Res.string.place_continue)) }
      }
    }
  }
}

/** The name of the place, and its date that opens the calendar. */
@Composable
private fun CityAndDate(viewModel: PlaceViewModel, language: String, onDateClick: () -> Unit, modifier: Modifier) {
  val date = formatDate(dayStart(viewModel.date, viewModel.zone) + NOON, viewModel.zone)
  Column(
    modifier =
      modifier.clip(RoundedCornerShape(BAR_CORNER - 8.dp)).clickable(onClick = onDateClick).padding(horizontal = 12.dp, vertical = 4.dp)
  ) {
    Text(
      text = viewModel.nearCity?.name(language) ?: formatPoint(viewModel.point),
      style = MaterialTheme.typography.titleMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Icon(Symbols.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
      Text(
        text = date,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private val BAR_WIDTH = 480.dp
private val BAR_MARGIN = 12.dp
private val BAR_CORNER = 24.dp
