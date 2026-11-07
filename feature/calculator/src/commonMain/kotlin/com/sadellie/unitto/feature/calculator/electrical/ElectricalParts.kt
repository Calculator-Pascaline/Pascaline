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

package com.sadellie.unitto.feature.calculator.electrical

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.projectile.Answer
import kotlin.enums.EnumEntries
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** One of a few ways to use a tool, with names that scroll when they don't fit. */
@Composable
internal fun <T : Enum<T>> ModeChips(entries: EnumEntries<T>, selected: T, title: (T) -> StringResource, onChoose: (T) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    entries.forEach {
      FilterChip(selected = it == selected, onClick = { onChoose(it) }, label = { Text(stringResource(title(it))) })
    }
  }
}

/** Tiles of the same width with the results. */
@Composable
internal fun Answers(vararg answers: Pair<StringResource, String>) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    answers.forEach { (label, value) -> Answer(Modifier.weight(1f), label, value) }
  }
}

@Composable
internal fun Hint(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A line through [count] + 1 points, from the left to the right of the canvas. */
internal fun DrawScope.drawLineGraph(color: Color, width: Float, count: Int, y: (Double) -> Float) {
  val path = Path()
  repeat(count + 1) { index ->
    val share = index.toDouble() / count
    val px = (share * size.width).toFloat()
    if (index == 0) path.moveTo(px, y(share)) else path.lineTo(px, y(share))
  }
  drawPath(path, color, style = Stroke(width = width.dp.toPx()))
}

internal fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, style: TextStyle, center: Offset) {
  val layout = measurer.measure(text, style)
  drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}
