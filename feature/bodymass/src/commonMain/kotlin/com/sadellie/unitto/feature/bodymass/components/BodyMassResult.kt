/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2026 Elshan Agaev
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

package com.sadellie.unitto.feature.bodymass.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.textfield.formatExpression
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.body_mass_normal
import unitto.core.common.generated.resources.body_mass_normal_weight
import unitto.core.common.generated.resources.body_mass_obese_1
import unitto.core.common.generated.resources.body_mass_obese_2
import unitto.core.common.generated.resources.body_mass_obese_3
import unitto.core.common.generated.resources.body_mass_overweight
import unitto.core.common.generated.resources.body_mass_underweight

/**
 * BMI on its scale, then what is left to read: weight range and waist-to-height ratio side by side.
 * Comes in once there is something to show, stays still while numbers change.
 */
@Composable
internal fun BodyMassResult(
  modifier: Modifier = Modifier,
  value: KBigDecimal,
  range: Pair<KBigDecimal, KBigDecimal>,
  rangeSuffix: String,
  waistToHeight: KBigDecimal,
  formatterSymbols: FormatterSymbols,
) {
  val hasBodyMass = value > KBigDecimal.ZERO
  val hasWaist = waistToHeight > KBigDecimal.ZERO
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    AnimatedVisibility(visible = hasBodyMass) {
      if (hasBodyMass) BodyMassCard(value, formatterSymbols)
    }
    AnimatedVisibility(visible = hasBodyMass || hasWaist) {
      Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (hasBodyMass) {
          WeightRangeTile(Modifier.weight(1f).fillMaxHeight(), range, rangeSuffix, formatterSymbols)
        }
        if (hasWaist) {
          WaistToHeightTile(Modifier.weight(1f).fillMaxHeight(), waistToHeight, formatterSymbols)
        }
      }
    }
  }
}

@Composable
private fun BodyMassCard(value: KBigDecimal, formatterSymbols: FormatterSymbols) {
  val text =
    remember(value, formatterSymbols) {
      value.toFormattedString(BODY_MASS_VALUE_SCALE, OutputFormat.PLAIN).formatExpression(formatterSymbols)
    }
  val classification = remember(value) { getBodyMassData(value) }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.extraLarge)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .padding(Sizes.large),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = stringResource(classification.classification),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    BodyMassScale(value, formatterSymbols)
  }
}

/** Categories as a colored bar with their limits under it, a marker shows [value]. */
@Composable
private fun BodyMassScale(value: KBigDecimal, formatterSymbols: FormatterSymbols) {
  val measurer = rememberTextMeasurer()
  val labelStyle =
    MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val markerColor = MaterialTheme.colorScheme.onSurface
  val markerBorder = MaterialTheme.colorScheme.surfaceContainerHigh
  val shown = value.toFormattedString(MARKER_SCALE, OutputFormat.PLAIN).toFloat()
  val position by animateFloatAsState(scalePosition(shown), label = "Body mass marker")
  val limits =
    remember(formatterSymbols) {
      indexes.dropLast(1).map { it.minValue to it.minValue.toPlainString().formatExpression(formatterSymbols) }
    }
  Canvas(modifier = Modifier.fillMaxWidth().height(SCALE_HEIGHT)) {
    val barHeight = SCALE_BAR_HEIGHT.toPx()
    val top = SCALE_MARKER_RADIUS.toPx() - barHeight / 2
    val gap = SCALE_GAP.toPx()
    var end = size.width
    clipPath(
      Path().apply {
        addRoundRect(RoundRect(0f, top, size.width, top + barHeight, CornerRadius(barHeight / 2)))
      }
    ) {
      indexes.forEach { index ->
        val start = (scalePosition(index.minValue.toPlainString().toFloat()) * size.width)
        drawRect(index.color, Offset(start, top), Size((end - start - gap).coerceAtLeast(0f), barHeight))
        end = start
      }
    }
    val labelTop = SCALE_MARKER_RADIUS.toPx() * 2 + SCALE_GAP.toPx()
    limits.forEach { (limit, text) ->
      val layout = measurer.measure(text, labelStyle)
      val center = scalePosition(limit.toPlainString().toFloat()) * size.width
      val left = (center - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
      drawText(layout, topLeft = Offset(left, labelTop))
    }
    val marker = Offset(position * size.width, SCALE_MARKER_RADIUS.toPx())
    drawCircle(markerBorder, SCALE_MARKER_RADIUS.toPx(), marker)
    drawCircle(markerColor, SCALE_MARKER_RADIUS.toPx() - SCALE_GAP.toPx(), marker)
  }
}

/** Where a BMI is on the scale, from 0 to 1. */
private fun scalePosition(bodyMass: Float): Float =
  ((bodyMass - SCALE_START) / (SCALE_END - SCALE_START)).coerceIn(0f, 1f)

@Composable
private fun WeightRangeTile(
  modifier: Modifier,
  range: Pair<KBigDecimal, KBigDecimal>,
  rangeSuffix: String,
  formatterSymbols: FormatterSymbols,
) {
  val text =
    remember(range, formatterSymbols, rangeSuffix) {
      val low =
        range.first.toFormattedString(BODY_MASS_RANGE_SCALE, OutputFormat.PLAIN).formatExpression(formatterSymbols)
      val high =
        range.second.toFormattedString(BODY_MASS_RANGE_SCALE, OutputFormat.PLAIN).formatExpression(formatterSymbols)
      "$low – $high $rangeSuffix"
    }
  BodyMassTile(
    modifier = modifier,
    color = MaterialTheme.colorScheme.secondaryContainer,
    value = text,
    label = stringResource(Res.string.body_mass_normal_weight),
  )
}

/** Small card with a number and what it is. */
@Composable
internal fun BodyMassTile(modifier: Modifier, color: Color, value: String, label: String) {
  Column(
    modifier =
      modifier.clip(MaterialTheme.shapes.large).background(color).padding(Sizes.large),
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
  }
}

@Immutable
private data class BodyMassData(
  val minValue: KBigDecimal,
  val color: Color,
  val classification: StringResource,
)

@Stable
private fun getBodyMassData(value: KBigDecimal): BodyMassData =
  indexes.first { value >= it.minValue }

private val indexes by lazy {
  listOf(
    BodyMassData(
      minValue = KBigDecimal("40"),
      color = Color(0x80FF2323),
      classification = Res.string.body_mass_obese_3,
    ),
    BodyMassData(
      minValue = KBigDecimal("35"),
      color = Color(0x80F85F31),
      classification = Res.string.body_mass_obese_2,
    ),
    BodyMassData(
      minValue = KBigDecimal("30"),
      color = Color(0x80FF9634),
      classification = Res.string.body_mass_obese_1,
    ),
    BodyMassData(
      minValue = KBigDecimal("25"),
      color = Color(0x80DBEC18),
      classification = Res.string.body_mass_overweight,
    ),
    BodyMassData(
      minValue = KBigDecimal("18.5"),
      color = Color(0x805BF724),
      classification = Res.string.body_mass_normal,
    ),
    BodyMassData(
      minValue = KBigDecimal("0"),
      color = Color(0x800EACDD),
      classification = Res.string.body_mass_underweight,
    ),
  )
}
private const val BODY_MASS_VALUE_SCALE = 1
private const val MARKER_SCALE = 2
private const val SCALE_START = 15f
private const val SCALE_END = 42f
private val SCALE_HEIGHT = 44.dp
private val SCALE_BAR_HEIGHT = 12.dp
private val SCALE_MARKER_RADIUS = 11.dp
private val SCALE_GAP = 2.dp
private const val BODY_MASS_RANGE_SCALE = 0

@Preview
@Composable
fun PreviewBodyMassResult() {
  BodyMassResult(
    value = KBigDecimal(18.5),
    range = KBigDecimal(50.0) to KBigDecimal(80.0),
    rangeSuffix = "kg",
    waistToHeight = KBigDecimal(0.47),
    formatterSymbols = FormatterSymbols(Token.SPACE, Token.PERIOD, false),
  )
}
