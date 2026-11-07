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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.number
import com.sadellie.unitto.feature.calculator.physics.numberFormat
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import kotlin.math.abs
import kotlin.math.pow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.resistors_digit
import unitto.core.common.generated.resources.resistors_find
import unitto.core.common.generated.resources.resistors_five
import unitto.core.common.generated.resources.resistors_four
import unitto.core.common.generated.resources.resistors_hint
import unitto.core.common.generated.resources.resistors_multiplier
import unitto.core.common.generated.resources.resistors_range
import unitto.core.common.generated.resources.resistors_series_hint
import unitto.core.common.generated.resources.resistors_title
import unitto.core.common.generated.resources.resistors_tolerance
import unitto.core.common.generated.resources.resistors_value
import unitto.core.common.generated.resources.resistors_wanted

private val WANTED = Condition("R", "Ω", Res.string.resistors_wanted, 0.0..10000.0, 10.0)

private val BAND_PAINT =
  mapOf(
    BandColor.BLACK to Color(0xFF1B1B1B),
    BandColor.BROWN to Color(0xFF6D3B1F),
    BandColor.RED to Color(0xFFD32F2F),
    BandColor.ORANGE to Color(0xFFF57C00),
    BandColor.YELLOW to Color(0xFFFBC02D),
    BandColor.GREEN to Color(0xFF388E3C),
    BandColor.BLUE to Color(0xFF1976D2),
    BandColor.VIOLET to Color(0xFF7B1FA2),
    BandColor.GREY to Color(0xFF8A8A8A),
    BandColor.WHITE to Color(0xFFF5F5F5),
    BandColor.GOLD to Color(0xFFB8962E),
    BandColor.SILVER to Color(0xFFB9BDC1),
  )
private val BODY = Color(0xFFD9C29C)
private val WIRE = Color(0xFF9E9E9E)

private val BandColor.paint: Color
  get() = BAND_PAINT.getValue(this)

private enum class BandRole(val title: StringResource) {
  DIGIT(Res.string.resistors_digit),
  MULTIPLIER(Res.string.resistors_multiplier),
  TOLERANCE(Res.string.resistors_tolerance),
}

private enum class BandCount(val digits: Int, val title: StringResource) {
  FOUR(2, Res.string.resistors_four),
  FIVE(MULTIPLIER_INDEX, Res.string.resistors_five),
}

private val STARTING_DIGITS = listOf(BandColor.YELLOW, BandColor.VIOLET, BandColor.BLACK)

private fun ColorCode.role(band: Int) =
  when {
    band < digits.size -> BandRole.DIGIT
    band == digits.size -> BandRole.MULTIPLIER
    else -> BandRole.TOLERANCE
  }

private fun ColorCode.withBand(band: Int, color: BandColor) =
  when (role(band)) {
    BandRole.DIGIT -> copy(digits = digits.mapIndexed { index, old -> if (index == band) color else old })
    BandRole.MULTIPLIER -> copy(multiplier = color)
    BandRole.TOLERANCE -> copy(tolerance = color)
  }

private fun BandRole.colors() =
  when (this) {
    BandRole.DIGIT -> BandColor.entries.filter { it.digit != null }
    BandRole.MULTIPLIER -> BandColor.entries
    BandRole.TOLERANCE -> BandColor.entries.filter { it.tolerance != null }
  }

/** What a color means for a band, written on the color. */
private fun BandRole.meaning(color: BandColor) =
  when (this) {
    BandRole.DIGIT -> "${color.digit}"
    BandRole.MULTIPLIER -> "×${multiplierText(TEN.pow(color.exponent))}"
    BandRole.TOLERANCE -> "±${color.tolerance?.rounded(2)}%"
  }

private fun multiplierText(value: Double) =
  if (value < 1) value.rounded(2) else engineering(value, "", { it.rounded(2) }).replace(" ", "")

/** Tap a band of the resistor and give it a color, or start from the value you need. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResistorsRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var count by rememberSaveable { mutableStateOf(BandCount.FOUR) }
  var chosen by rememberSaveable { mutableIntStateOf(0) }
  var saved by rememberSaveable { mutableStateOf(listOf(STARTING_DIGITS[0], STARTING_DIGITS[1], STARTING_DIGITS[2], BandColor.BROWN, BandColor.GOLD)) }
  var wanted by rememberSaveable { mutableStateOf("4700") }
  // Digits, multiplier and tolerance are kept as they were when going from 4 to 5 bands and back
  val code = ColorCode(saved.take(count.digits), saved[MULTIPLIER_INDEX], saved[TOLERANCE_INDEX])
  val band = chosen.coerceAtMost(code.bands.lastIndex)
  fun show(next: ColorCode) {
    saved = next.digits + saved.drop(next.digits.size).take(MULTIPLIER_INDEX - next.digits.size) + next.multiplier + next.tolerance
  }

  PhysicsScreen(
    title = Res.string.resistors_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(BandCount.entries, count, { it.title }) { count = it }
      LiveCard {
        ResistorDrawing(code.bands, band) { chosen = it }
        Hint(stringResource(Res.string.resistors_hint))
        Palette(code.role(band), code.bands[band]) { show(code.withBand(band, it)) }
        Answers(
          Res.string.resistors_value to engineering(code.resistance, "Ω", format),
          Res.string.resistors_tolerance to "±${format(code.share * PERCENT)}%",
        )
        Answers(
          Res.string.resistors_range to
            "${engineering(code.lowest, "Ω", format)} – ${engineering(code.highest, "Ω", format)}"
        )
      }
    },
    settings = {},
    explanation = {
      Card(stringResource(Res.string.resistors_find)) {
        ConditionRow(WANTED, wanted, fractional) { wanted = it }
        Hint(stringResource(Res.string.resistors_series_hint))
        val ohms = wanted.number()
        if (ohms > 0) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResistorSeries.entries.forEach { series ->
              val value = series.nearest(ohms)
              AssistChip(
                onClick = { colorCode(value, count.digits, saved[TOLERANCE_INDEX])?.let { show(it) } },
                label = { Text("${series.name}: ${engineering(value, "Ω", format)}") },
              )
            }
          }
        }
      }
    },
  )
}

@Composable
private fun ResistorDrawing(bands: List<BandColor>, chosen: Int, onChoose: (Int) -> Unit) {
  val choose by rememberUpdatedState(onChoose)
  val count by rememberUpdatedState(bands.size)
  val outline = MaterialTheme.colorScheme.onSurface
  Canvas(
    Modifier.fillMaxWidth().height(RESISTOR_HEIGHT).pointerInput(Unit) {
      detectTapGestures { position ->
        val index = (0 until count).minBy { abs(bandCenter(it, count, size.width.toFloat()) - position.x) }
        choose(index)
      }
    }
  ) {
    drawResistor(bands, chosen, outline)
  }
}

/** Evenly spaced digits and multiplier, then a gap before the tolerance. */
private fun bandCenter(index: Int, count: Int, width: Float): Float {
  val start = width * BODY_START + width * BODY_SHARE * BAND_INSET
  val end = width * BODY_START + width * BODY_SHARE * (1 - BAND_INSET)
  val share = if (index == count - 1) 1f else index.toFloat() / (count - 1) * BEFORE_GAP
  return start + (end - start) * share
}

private fun DrawScope.drawResistor(bands: List<BandColor>, chosen: Int, outline: Color) {
  val middle = size.height / 2
  drawLine(WIRE, Offset(0f, middle), Offset(size.width, middle), strokeWidth = WIRE_WIDTH.dp.toPx())
  val body = Offset(size.width * BODY_START, size.height * BODY_MARGIN)
  val bodySize = Size(size.width * BODY_SHARE, size.height * (1 - 2 * BODY_MARGIN))
  val corner = CornerRadius(bodySize.height / 2)
  drawRoundRect(BODY, body, bodySize, corner)
  val width = BAND_WIDTH.dp.toPx()
  bands.forEachIndexed { index, band ->
    val x = bandCenter(index, bands.size, size.width) - width / 2
    drawRect(band.paint, Offset(x, body.y), Size(width, bodySize.height))
    if (index == chosen) {
      drawRect(
        outline,
        Offset(x - RING.dp.toPx(), body.y - RING.dp.toPx()),
        Size(width + 2 * RING.dp.toPx(), bodySize.height + 2 * RING.dp.toPx()),
        style = Stroke(width = RING.dp.toPx()),
      )
    }
  }
}

/** The colors the chosen band can have and what they mean. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Palette(role: BandRole, current: BandColor, onChoose: (BandColor) -> Unit) {
  Text(stringResource(role.title), style = MaterialTheme.typography.titleSmall)
  FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    role.colors().forEach { color ->
      val onColor = if (color.paint.luminance() > DARK_TEXT_ABOVE) Color.Black else Color.White
      Box(
        modifier =
          Modifier.size(height = SWATCH_HEIGHT, width = SWATCH)
            .clip(SWATCH_SHAPE)
            .background(color.paint)
            .border(
              if (color == current) RING.dp * 2 else 1.dp,
              if (color == current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
              SWATCH_SHAPE,
            )
            .clickable { onChoose(color) }
            .padding(2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(role.meaning(color), style = MaterialTheme.typography.labelSmall, color = onColor, maxLines = 1)
      }
    }
  }
}

private const val MULTIPLIER_INDEX = 3
private const val TOLERANCE_INDEX = 4
private const val PERCENT = 100
private const val TEN = 10.0
private const val BODY_START = 0.14f
private const val BODY_SHARE = 0.72f
private const val BODY_MARGIN = 0.12f
private const val BAND_INSET = 0.12f
private const val BEFORE_GAP = 0.72f
private const val DARK_TEXT_ABOVE = 0.45f
private const val WIRE_WIDTH = 5
private const val BAND_WIDTH = 14
private const val RING = 2
private val RESISTOR_HEIGHT = 96.dp
private val SWATCH = 52.dp
private val SWATCH_HEIGHT = 36.dp
private val SWATCH_SHAPE = RoundedCornerShape(10.dp)
