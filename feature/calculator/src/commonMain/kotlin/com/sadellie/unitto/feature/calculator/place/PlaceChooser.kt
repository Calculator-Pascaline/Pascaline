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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.designsystem.icons.symbols.Edit
import com.sadellie.unitto.core.designsystem.icons.symbols.Event
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.place_search_city

/** The chosen city with its coordinates, a tap changes it, and its date, a tap opens the calendar. */
@Composable
internal fun CityAndDate(viewModel: PlaceViewModel, language: String, onCityClick: () -> Unit, onDateClick: () -> Unit) {
  val date = formatDate(dayStart(viewModel.date, viewModel.zone) + NOON, viewModel.zone)
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(CARD_SHAPE)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f).clip(CHIP_SHAPE).clickable(onClick = onCityClick).padding(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = viewModel.nearCity?.name(language) ?: formatPoint(viewModel.point),
          modifier = Modifier.weight(1f, fill = false),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Icon(Symbols.Edit, stringResource(Res.string.place_search_city), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
      }
      Text(
        text = formatPoint(viewModel.point),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
    Row(
      modifier =
        Modifier.clip(CHIP_SHAPE)
          .background(MaterialTheme.colorScheme.surface)
          .clickable(onClick = onDateClick)
          .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(Symbols.Event, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
      Text(text = date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
    }
  }
}

private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val CHIP_SHAPE = RoundedCornerShape(16.dp)
