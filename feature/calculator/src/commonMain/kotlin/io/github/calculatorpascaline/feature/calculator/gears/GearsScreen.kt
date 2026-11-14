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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Delete
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Symbols
import io.github.calculatorpascaline.feature.calculator.electrical.Hint
import io.github.calculatorpascaline.feature.calculator.electrical.ModeChips
import io.github.calculatorpascaline.feature.calculator.electrical.NameChips
import io.github.calculatorpascaline.feature.calculator.physics.number
import io.github.calculatorpascaline.feature.calculator.physics.numberFormat
import io.github.calculatorpascaline.feature.calculator.physics.rememberFractional
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import io.github.calculatorpascaline.feature.calculator.projectile.Condition
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.gears_examples
import pascaline.core.common.generated.resources.gears_name
import pascaline.core.common.generated.resources.gears_open
import pascaline.core.common.generated.resources.gears_power
import pascaline.core.common.generated.resources.gears_rack_force
import pascaline.core.common.generated.resources.gears_rack_speed
import pascaline.core.common.generated.resources.gears_remove
import pascaline.core.common.generated.resources.gears_save
import pascaline.core.common.generated.resources.gears_saved
import pascaline.core.common.generated.resources.gears_saved_empty
import pascaline.core.common.generated.resources.gears_speed
import pascaline.core.common.generated.resources.gears_torque
import pascaline.core.common.generated.resources.gears_units

/**
 * Gears: a train that turns, as many gears as wanted, each meshing, on a shared shaft, in a ring, on a belt, turned by
 * a worm or pushing a rack. Or a planetary set, or the trains that give a wanted ratio.
 */
@Composable
internal fun GearsRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  var inputs by rememberSaveable(stateSaver = INPUTS_SAVER) { mutableStateOf(GearsInputs()) }
  val screen = GearsContext(inputs, { inputs = it }, fractional, numberFormat(fractional), openDrawer)
  when (inputs.mode) {
    GearsMode.TRAIN -> TrainMode(screen)
    GearsMode.PLANETARY -> PlanetaryMode(screen)
    GearsMode.FIND -> FindMode(screen)
  }
}

/** What every part of the screen needs: the [inputs], how to [change] them, and how numbers are typed and written. */
internal class GearsContext(
  val inputs: GearsInputs,
  val change: (GearsInputs) -> Unit,
  val fractional: String,
  val format: (Double) -> String,
  val openDrawer: () -> Unit,
) {
  val texts = GearTexts(format, if (inputs.system == PitchSystem.MODULE) "mm" else "in", inputs.speedUnit, inputs.units)

  /** The turning speed typed, in rpm. */
  val speedRpm: Double
    get() = inputs.speed.number() / inputs.speedUnit.perRpm

  /** The torque typed, in N·m, or the force on a rack in N when [rack]. */
  fun torque(rack: Boolean = false): Double =
    inputs.torque.number() / if (rack) inputs.units.forcePerNewton else inputs.units.torquePerNewtonMeter

  /** The power typed, in watts. */
  val powerWatts: Double
    get() = inputs.power.number() / inputs.units.powerPerWatt

  /** The speed field, in the chosen unit, or the straight speed of a rack when [rack]. */
  fun speedCondition(rack: Boolean): Condition =
    if (rack) {
      if (inputs.units == ForceUnits.METRIC) RACK_SPEED_METRIC else RACK_SPEED_IMPERIAL
    } else {
      when (inputs.speedUnit) {
        SpeedUnit.RPM -> SPEED_RPM
        SpeedUnit.RAD_PER_SECOND -> SPEED_RAD
        SpeedUnit.HERTZ -> SPEED_HZ
      }
    }

  fun torqueCondition(rack: Boolean): Condition =
    when {
      rack && inputs.units == ForceUnits.METRIC -> FORCE_METRIC
      rack -> FORCE_IMPERIAL
      inputs.units == ForceUnits.METRIC -> TORQUE_METRIC
      else -> TORQUE_IMPERIAL
    }

  val powerCondition: Condition
    get() = if (inputs.units == ForceUnits.METRIC) POWER_METRIC else POWER_IMPERIAL
}

/** The three modes of the tool. */
@Composable
internal fun ModeSelector(screen: GearsContext) {
  ModeChips(GearsMode.entries, screen.inputs.mode, { it.title }) { screen.change(screen.inputs.copy(mode = it)) }
}

/** Trains to start from: picking one replaces what was typed, but keeps the units. */
@Composable
internal fun ExamplesCard(screen: GearsContext) {
  Card(stringResource(Res.string.gears_examples)) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Example.entries.forEach { example ->
        AssistChip(
          onClick = { screen.change(example.inputs.inSameUnits(screen.inputs, screen.format)) },
          label = { Text(stringResource(example.title)) },
        )
      }
    }
  }
}

/** Units of speed, and of torque and power. What was typed is converted, so that it stays the same. */
@Composable
internal fun UnitsCard(screen: GearsContext, rack: Boolean) {
  val inputs = screen.inputs
  Card(stringResource(Res.string.gears_units)) {
    NameChips(SpeedUnit.entries, inputs.speedUnit, { it.symbol }) { unit ->
      val speed = if (rack) inputs.speed else convert(inputs.speed, unit.perRpm / inputs.speedUnit.perRpm, screen.format)
      screen.change(inputs.copy(speedUnit = unit, speed = speed))
    }
    NameChips(ForceUnits.entries, inputs.units, { "${it.torque}, ${it.power}" }) { units ->
      screen.change(inputs.converted(units, rack, screen.format))
    }
  }
}

/** [inputs] typed again in [units]. */
private fun GearsInputs.converted(units: ForceUnits, rack: Boolean, format: (Double) -> String): GearsInputs {
  val torqueFactor =
    if (rack) units.forcePerNewton / this.units.forcePerNewton
    else units.torquePerNewtonMeter / this.units.torquePerNewtonMeter
  val speed =
    if (rack) convert(speed, units.speedPerMeterPerSecond / this.units.speedPerMeterPerSecond, format) else speed
  return copy(
    units = units,
    speed = speed,
    torque = convert(torque, torqueFactor, format),
    power = convert(power, units.powerPerWatt / this.units.powerPerWatt, format),
  )
}

/** An example in the units of [current]: examples are written in rpm and metric units. */
private fun GearsInputs.inSameUnits(current: GearsInputs, format: (Double) -> String): GearsInputs {
  val inRpm = copy(speedUnit = SpeedUnit.RPM, units = ForceUnits.METRIC)
  return inRpm
    .copy(speedUnit = current.speedUnit, speed = convert(speed, current.speedUnit.perRpm, format))
    .converted(current.units, rack = false, format = format)
}

private fun convert(text: String, factor: Double, format: (Double) -> String): String {
  val value = text.number()
  return if (value.isFinite()) format(value * factor) else text
}

/** Trains kept under a name, to open again later. */
@Composable
internal fun SavedCard(screen: GearsContext) {
  val saved = rememberSavedTrains()
  var name by rememberSaveable { mutableStateOf("") }
  Card(stringResource(Res.string.gears_saved)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(Res.string.gears_name)) },
        singleLine = true,
        modifier = Modifier.weight(1f),
      )
      FilledTonalButton(onClick = { saved.save(name.trim(), screen.inputs) }, enabled = name.isNotBlank()) {
        Text(stringResource(Res.string.gears_save))
      }
    }
    val trains = saved.trains.orEmpty()
    if (trains.isEmpty()) Hint(stringResource(Res.string.gears_saved_empty))
    trains.forEach { train ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(train.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { screen.change(train.inputs) }) { Text(stringResource(Res.string.gears_open)) }
        IconButton(onClick = { saved.delete(train) }) {
          Icon(Symbols.Delete, contentDescription = stringResource(Res.string.gears_remove))
        }
      }
    }
  }
}

/** Something that could go wrong, in the color of errors. */
@Composable
internal fun Warning(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

private val SPEED_RPM = Condition("n", "rpm", Res.string.gears_speed, 0.0..6000.0, 10.0)
private val SPEED_RAD = Condition("ω", "rad/s", Res.string.gears_speed, 0.0..630.0, 1.0)
private val SPEED_HZ = Condition("f", "Hz", Res.string.gears_speed, 0.0..100.0, 0.5)
private val TORQUE_METRIC = Condition("T", "N·m", Res.string.gears_torque, 0.0..200.0, 0.5)
private val TORQUE_IMPERIAL = Condition("T", "lbf·ft", Res.string.gears_torque, 0.0..150.0, 0.5)
private val POWER_METRIC = Condition("P", "kW", Res.string.gears_power, 0.0..20.0, 0.05)
private val POWER_IMPERIAL = Condition("P", "hp", Res.string.gears_power, 0.0..27.0, 0.05)
private val RACK_SPEED_METRIC = Condition("v", "m/s", Res.string.gears_rack_speed, 0.0..5.0, 0.01)
private val RACK_SPEED_IMPERIAL = Condition("v", "ft/s", Res.string.gears_rack_speed, 0.0..16.0, 0.05)
private val FORCE_METRIC = Condition("F", "N", Res.string.gears_rack_force, 0.0..5000.0, 10.0)
private val FORCE_IMPERIAL = Condition("F", "lbf", Res.string.gears_rack_force, 0.0..1100.0, 2.0)
