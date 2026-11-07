/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.core.ui

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun Slider(
  modifier: Modifier = Modifier,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit = {},
  @IntRange(from = 0) steps: Int = 0,
) {
  val animated =
    animateFloatAsState(
      targetValue = value.roundToInt().toFloat(),
      animationSpec = spring(),
      label = "Thumb animation",
    )

  androidx.compose.material3.Slider(
    value = animated.value,
    onValueChange = onValueChange,
    modifier = modifier,
    valueRange = valueRange,
    onValueChangeFinished = onValueChangeFinished,
    steps = steps,
  )
}

/**
 * Slim slider for precise values: a thin line with a small dot that grows while it is held.
 *
 * @param origin Where the colored part starts, so that a range around 0 fills from 0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FineSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  origin: Float = valueRange.start,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isDragged by interactionSource.collectIsDraggedAsState()
  val active = MaterialTheme.colorScheme.primary
  val rest = MaterialTheme.colorScheme.outlineVariant
  androidx.compose.material3.Slider(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.height(FINE_SLIDER_HEIGHT),
    valueRange = valueRange,
    interactionSource = interactionSource,
    thumb = {
      val size by animateDpAsState(if (isPressed || isDragged) FINE_THUMB_HELD else FINE_THUMB, label = "Slider thumb")
      Box(Modifier.size(FINE_THUMB_HELD), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).background(active, CircleShape))
      }
    },
    track = { state ->
      Canvas(Modifier.fillMaxWidth().height(FINE_THUMB_HELD)) {
        val range = state.valueRange
        fun xOf(number: Float) = (number - range.start) / (range.endInclusive - range.start) * size.width
        val middle = size.height / 2
        val width = FINE_TRACK.toPx()
        drawLine(rest, Offset(0f, middle), Offset(size.width, middle), width, StrokeCap.Round)
        drawLine(active, Offset(xOf(origin), middle), Offset(xOf(state.value), middle), width, StrokeCap.Round)
        if (origin > range.start) drawCircle(active, width, Offset(xOf(origin), middle))
      }
    },
  )
}

private val FINE_SLIDER_HEIGHT = 32.dp
private val FINE_TRACK = 3.dp
private val FINE_THUMB = 14.dp
private val FINE_THUMB_HELD = 20.dp

@Preview
@Composable
private fun PreviewNewSlider() {
  var currentValue by remember { mutableFloatStateOf(9f) }

  com.sadellie.unitto.core.ui.Slider(
    value = currentValue,
    valueRange = 0f..16f,
    onValueChange = { currentValue = it },
  )
}
