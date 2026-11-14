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

package io.github.calculatorpascaline.feature.calculator.gears

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Add
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Delete
import io.github.calculatorpascaline.core.designsystem.icons.symbols.KeyboardArrowDown
import io.github.calculatorpascaline.core.designsystem.icons.symbols.KeyboardArrowUp
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Symbols
import io.github.calculatorpascaline.feature.calculator.electrical.AnswerList
import io.github.calculatorpascaline.feature.calculator.electrical.Answers
import io.github.calculatorpascaline.feature.calculator.electrical.Hint
import io.github.calculatorpascaline.feature.calculator.electrical.ModeChips
import io.github.calculatorpascaline.feature.calculator.electrical.engineering
import io.github.calculatorpascaline.feature.calculator.physics.LiveCard
import io.github.calculatorpascaline.feature.calculator.physics.PhysicsScreen
import io.github.calculatorpascaline.feature.calculator.physics.number
import io.github.calculatorpascaline.feature.calculator.physics.numberFormat
import io.github.calculatorpascaline.feature.calculator.physics.rememberFractional
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import io.github.calculatorpascaline.feature.calculator.projectile.Condition
import io.github.calculatorpascaline.feature.calculator.projectile.ConditionRow
import io.github.calculatorpascaline.feature.calculator.projectile.Step
import kotlin.math.abs
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.gears_add
import pascaline.core.common.generated.resources.gears_center
import pascaline.core.common.generated.resources.gears_diameter
import pascaline.core.common.generated.resources.gears_diametral
import pascaline.core.common.generated.resources.gears_efficiency
import pascaline.core.common.generated.resources.gears_gear
import pascaline.core.common.generated.resources.gears_hint
import pascaline.core.common.generated.resources.gears_how
import pascaline.core.common.generated.resources.gears_input
import pascaline.core.common.generated.resources.gears_input_power
import pascaline.core.common.generated.resources.gears_meshes
import pascaline.core.common.generated.resources.gears_module
import pascaline.core.common.generated.resources.gears_move_down
import pascaline.core.common.generated.resources.gears_move_up
import pascaline.core.common.generated.resources.gears_output_power
import pascaline.core.common.generated.resources.gears_output_speed
import pascaline.core.common.generated.resources.gears_output_torque
import pascaline.core.common.generated.resources.gears_power
import pascaline.core.common.generated.resources.gears_power_text
import pascaline.core.common.generated.resources.gears_ratio
import pascaline.core.common.generated.resources.gears_remove
import pascaline.core.common.generated.resources.gears_same_shaft
import pascaline.core.common.generated.resources.gears_size_text
import pascaline.core.common.generated.resources.gears_speed
import pascaline.core.common.generated.resources.gears_speed_text
import pascaline.core.common.generated.resources.gears_teeth
import pascaline.core.common.generated.resources.gears_title
import pascaline.core.common.generated.resources.gears_torque
import pascaline.core.common.generated.resources.gears_torque_text
import pascaline.core.common.generated.resources.gears_train
import pascaline.core.common.generated.resources.gears_train_efficiency

private val TEETH = Condition("z", "", Res.string.gears_teeth, 6.0..120.0, 1.0)
private val MODULE = Condition("m", "mm", Res.string.gears_module, 0.5..10.0, 0.25)
private val DIAMETRAL = Condition("Pd", "1/in", Res.string.gears_diametral, 2.0..48.0, 1.0)
private val SPEED = Condition("n", "rpm", Res.string.gears_speed, 0.0..6000.0, 10.0)
private val TORQUE = Condition("T", "N·m", Res.string.gears_torque, 0.0..200.0, 0.5)
private val POWER = Condition("P", "kW", Res.string.gears_power, 0.0..20.0, 0.05)
private val EFFICIENCY = Condition("η", "%", Res.string.gears_efficiency, 80.0..100.0, 0.5)

/** What is given at the input: its torque, or the power that turns it. */
private enum class Drive(val title: StringResource) {
  TORQUE(Res.string.gears_torque),
  POWER(Res.string.gears_power),
}

private val PitchSystem.title
  get() = if (this == PitchSystem.MODULE) Res.string.gears_module else Res.string.gears_diametral

private val PitchSystem.unit
  get() = if (this == PitchSystem.MODULE) "mm" else "in"

/** A gear after the input as it is typed. */
private data class GearEntry(val teeth: String, val sameShaft: Boolean)

private val GEARS_SAVER =
  listSaver<List<GearEntry>, Any>(
    save = { gears -> gears.flatMap { listOf(it.teeth, it.sameShaft) } },
    restore = { saved -> saved.chunked(2).map { (teeth, sameShaft) -> GearEntry(teeth as String, sameShaft as Boolean) } },
  )

// 20 teeth on the motor, a 40 that halves the speed, and a 15 on its shaft that drives a 45: 6 to 1 in all
private val STARTING_GEARS = listOf(GearEntry("40", false), GearEntry("15", true), GearEntry("45", false))

/** A gear train that turns: the input, then as many gears as wanted, each meshing or on the shaft of the one before. */
@Composable
internal fun GearsRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var teeth by rememberSaveable { mutableStateOf("20") }
  var system by rememberSaveable { mutableStateOf(PitchSystem.MODULE) }
  var module by rememberSaveable { mutableStateOf("2") }
  var diametral by rememberSaveable { mutableStateOf("12") }
  var speed by rememberSaveable { mutableStateOf("1500") }
  var drive by rememberSaveable { mutableStateOf(Drive.TORQUE) }
  var torque by rememberSaveable { mutableStateOf("10") }
  var power by rememberSaveable { mutableStateOf("1.5") }
  var efficiency by rememberSaveable { mutableStateOf("98") }
  var gears by rememberSaveable(stateSaver = GEARS_SAVER) { mutableStateOf(STARTING_GEARS) }

  val rpm = speed.number()
  val train =
    GearTrain(
      teeth = teeth.number(),
      pitch = if (system == PitchSystem.MODULE) module.number() else diametral.number(),
      system = system,
      speed = rpm,
      torque = if (drive == Drive.TORQUE) torque.number() else torqueFromPower(power.number() * KILO, rpm),
      efficiency = efficiency.number() / PERCENT,
      gears = gears.map { GearSpec(it.teeth.number(), it.sameShaft) },
    )
  val result = train.solve()
  val texts = GearTexts(format, system.unit)

  PhysicsScreen(
    title = Res.string.gears_title,
    openDrawer = openDrawer,
    live = {
      LiveCard {
        GearDrawing(result.gears)
        Hint(stringResource(Res.string.gears_hint))
      }
      TrainCard(result, texts)
    },
    settings = {
      Card(stringResource(Res.string.gears_input)) {
        ConditionRow(TEETH, teeth, fractional) { teeth = it }
        ModeChips(PitchSystem.entries, system, { it.title }) { system = it }
        if (system == PitchSystem.MODULE) {
          ConditionRow(MODULE, module, fractional) { module = it }
        } else {
          ConditionRow(DIAMETRAL, diametral, fractional) { diametral = it }
        }
        ConditionRow(SPEED, speed, fractional) { speed = it }
        ModeChips(Drive.entries, drive, { it.title }) { drive = it }
        if (drive == Drive.TORQUE) {
          ConditionRow(TORQUE, torque, fractional) { torque = it }
        } else {
          ConditionRow(POWER, power, fractional) { power = it }
        }
        ConditionRow(EFFICIENCY, efficiency, fractional) { efficiency = it }
        GearAnswers(result.gears.first(), texts)
      }
      gears.forEachIndexed { index, entry ->
        GearCard(
          number = index + 2,
          entry = entry,
          state = result.gears[index + 1],
          texts = texts,
          fractional = fractional,
          onChange = { changed -> gears = gears.toMutableList().also { it[index] = changed } },
          onMove = { by -> gears = gears.moved(index, index + by) },
          onRemove = { gears = gears.toMutableList().also { it.removeAt(index) } },
          isLast = index == gears.lastIndex,
        )
      }
      FilledTonalButton(onClick = { gears = gears + GearEntry(gears.lastOrNull()?.teeth ?: teeth, false) }) {
        Icon(Symbols.Add, contentDescription = null)
        Text(stringResource(Res.string.gears_add), modifier = Modifier.padding(start = 8.dp))
      }
    },
    explanation = { Explanation(system) },
  )
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> =
  if (to !in indices) this else toMutableList().also { it.add(to, it.removeAt(from)) }

/** How numbers are written: speed with the way it turns, ratio as 6 : 1, sizes in mm or inches. */
private class GearTexts(val format: (Double) -> String, val unit: String) {
  fun speed(rpm: Double): String {
    if (!rpm.isFinite()) return DASH
    val arrow =
      when {
        rpm > 0 -> " ↻"
        rpm < 0 -> " ↺"
        else -> ""
      }
    return "${format(abs(rpm))} rpm$arrow"
  }

  fun torque(value: Double) = if (value.isFinite()) "${format(value)} N·m" else DASH

  fun ratio(value: Double) =
    when {
      !value.isFinite() || value <= 0 -> DASH
      value >= 1 -> "${format(value)} : 1"
      else -> "1 : ${format(1 / value)}"
    }

  fun size(value: Double) = if (value.isFinite()) "${format(value)} $unit" else DASH

  fun power(watts: Double) = engineering(watts, "W", format)

  fun percent(share: Double) = if (share.isFinite()) "${format(share * PERCENT)} %" else DASH
}

/** Ratio, speed, torque and power from the first gear to the last. */
@Composable
private fun TrainCard(result: TrainResult, texts: GearTexts) {
  Card(stringResource(Res.string.gears_train)) {
    Answers(
      Res.string.gears_ratio to texts.ratio(result.ratio),
      Res.string.gears_output_speed to texts.speed(result.output.speed),
    )
    Answers(
      Res.string.gears_output_torque to texts.torque(result.output.torque),
      Res.string.gears_train_efficiency to texts.percent(result.efficiency),
    )
    Answers(
      Res.string.gears_input_power to texts.power(result.inputPower),
      Res.string.gears_output_power to texts.power(result.outputPower),
    )
  }
}

/** Speed, torque and ratio of a gear, then its size and how far it is from the gear it meshes with. */
@Composable
private fun GearAnswers(state: GearState, texts: GearTexts) {
  Answers(
    Res.string.gears_speed to texts.speed(state.speed),
    Res.string.gears_torque to texts.torque(state.torque),
  )
  AnswerList(
    listOfNotNull(
      Res.string.gears_ratio to texts.ratio(state.ratio),
      Res.string.gears_diameter to texts.size(state.diameter),
      state.centerDistance?.let { Res.string.gears_center to texts.size(it) },
    )
  )
}

@Composable
private fun GearCard(
  number: Int,
  entry: GearEntry,
  state: GearState,
  texts: GearTexts,
  fractional: String,
  onChange: (GearEntry) -> Unit,
  onMove: (Int) -> Unit,
  onRemove: () -> Unit,
  isLast: Boolean,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        stringResource(Res.string.gears_gear, number),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      IconButton(onClick = { onMove(-1) }, enabled = number > 2) {
        Icon(Symbols.KeyboardArrowUp, contentDescription = stringResource(Res.string.gears_move_up))
      }
      IconButton(onClick = { onMove(1) }, enabled = !isLast) {
        Icon(Symbols.KeyboardArrowDown, contentDescription = stringResource(Res.string.gears_move_down))
      }
      IconButton(onClick = onRemove) {
        Icon(Symbols.Delete, contentDescription = stringResource(Res.string.gears_remove))
      }
    }
    ConditionRow(TEETH, entry.teeth, fractional) { onChange(entry.copy(teeth = it)) }
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = !entry.sameShaft,
        onClick = { onChange(entry.copy(sameShaft = false)) },
        label = { Text(stringResource(Res.string.gears_meshes, number - 1)) },
      )
      FilterChip(
        selected = entry.sameShaft,
        onClick = { onChange(entry.copy(sameShaft = true)) },
        label = { Text(stringResource(Res.string.gears_same_shaft, number - 1)) },
      )
    }
    GearAnswers(state, texts)
  }
}

@Composable
private fun Explanation(system: PitchSystem) {
  Card(stringResource(Res.string.gears_how)) {
    Step(Res.string.gears_speed, Res.string.gears_speed_text, "n₂ = −n₁ · z₁ / z₂")
    Step(Res.string.gears_torque, Res.string.gears_torque_text, "T₂ = T₁ · z₂ / z₁ · η")
    Step(
      Res.string.gears_diameter,
      Res.string.gears_size_text,
      if (system == PitchSystem.MODULE) "d = m · z" else "d = z / Pd",
      "a = (d₁ + d₂) / 2",
    )
    Step(Res.string.gears_power, Res.string.gears_power_text, "P = T · 2π · n / 60")
  }
}

private const val DASH = "—"
private const val PERCENT = 100.0
private const val KILO = 1000.0
private val CARD_SHAPE = RoundedCornerShape(24.dp)
