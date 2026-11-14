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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.calculatorpascaline.feature.calculator.electrical.Answers
import io.github.calculatorpascaline.feature.calculator.electrical.Hint
import io.github.calculatorpascaline.feature.calculator.electrical.ModeChips
import io.github.calculatorpascaline.feature.calculator.electrical.NameChips
import io.github.calculatorpascaline.feature.calculator.physics.LiveCard
import io.github.calculatorpascaline.feature.calculator.physics.PhysicsScreen
import io.github.calculatorpascaline.feature.calculator.physics.number
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import io.github.calculatorpascaline.feature.calculator.projectile.Condition
import io.github.calculatorpascaline.feature.calculator.projectile.ConditionRow
import io.github.calculatorpascaline.feature.calculator.projectile.Step
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.gears_belt_text
import pascaline.core.common.generated.resources.gears_carrier
import pascaline.core.common.generated.resources.gears_contact
import pascaline.core.common.generated.resources.gears_contact_text
import pascaline.core.common.generated.resources.gears_crowded_problem
import pascaline.core.common.generated.resources.gears_diameter
import pascaline.core.common.generated.resources.gears_efficiency
import pascaline.core.common.generated.resources.gears_exact
import pascaline.core.common.generated.resources.gears_find_hint
import pascaline.core.common.generated.resources.gears_find_none
import pascaline.core.common.generated.resources.gears_held
import pascaline.core.common.generated.resources.gears_how
import pascaline.core.common.generated.resources.gears_hunting
import pascaline.core.common.generated.resources.gears_input_power
import pascaline.core.common.generated.resources.gears_link_belt
import pascaline.core.common.generated.resources.gears_link_rack
import pascaline.core.common.generated.resources.gears_link_ring
import pascaline.core.common.generated.resources.gears_link_worm
import pascaline.core.common.generated.resources.gears_max_teeth
import pascaline.core.common.generated.resources.gears_min_teeth
import pascaline.core.common.generated.resources.gears_mode_find
import pascaline.core.common.generated.resources.gears_mode_planetary
import pascaline.core.common.generated.resources.gears_off_by
import pascaline.core.common.generated.resources.gears_output
import pascaline.core.common.generated.resources.gears_output_power
import pascaline.core.common.generated.resources.gears_output_speed
import pascaline.core.common.generated.resources.gears_output_torque
import pascaline.core.common.generated.resources.gears_planet_spin
import pascaline.core.common.generated.resources.gears_planet_teeth
import pascaline.core.common.generated.resources.gears_planet_teeth_problem
import pascaline.core.common.generated.resources.gears_planetary_hint
import pascaline.core.common.generated.resources.gears_planetary_text
import pascaline.core.common.generated.resources.gears_planets
import pascaline.core.common.generated.resources.gears_power
import pascaline.core.common.generated.resources.gears_power_text
import pascaline.core.common.generated.resources.gears_rack_text
import pascaline.core.common.generated.resources.gears_ratio
import pascaline.core.common.generated.resources.gears_ring
import pascaline.core.common.generated.resources.gears_ring_text
import pascaline.core.common.generated.resources.gears_searching
import pascaline.core.common.generated.resources.gears_size_text
import pascaline.core.common.generated.resources.gears_spacing_problem
import pascaline.core.common.generated.resources.gears_speed
import pascaline.core.common.generated.resources.gears_speed_text
import pascaline.core.common.generated.resources.gears_stages
import pascaline.core.common.generated.resources.gears_stress
import pascaline.core.common.generated.resources.gears_stress_text
import pascaline.core.common.generated.resources.gears_sun
import pascaline.core.common.generated.resources.gears_target
import pascaline.core.common.generated.resources.gears_title
import pascaline.core.common.generated.resources.gears_torque
import pascaline.core.common.generated.resources.gears_torque_text
import pascaline.core.common.generated.resources.gears_turned
import pascaline.core.common.generated.resources.gears_use
import pascaline.core.common.generated.resources.gears_worm_text

private val SUN = Condition("z", "", Res.string.gears_sun, 6.0..60.0, 1.0)
private val RING = Condition("z", "", Res.string.gears_ring, 24.0..150.0, 1.0)
private val PLANETS = Condition("N", "", Res.string.gears_planets, 1.0..8.0, 1.0)
private val PLANET_EFFICIENCY = Condition("η", "%", Res.string.gears_efficiency, 80.0..100.0, 0.5)
private val TARGET = Condition("i", "", Res.string.gears_target, 0.1..100.0, 0.1)
private val MIN_TEETH_FIELD = Condition("z ≥", "", Res.string.gears_min_teeth, 6.0..40.0, 1.0)
private val MAX_TEETH_FIELD = Condition("z ≤", "", Res.string.gears_max_teeth, 20.0..120.0, 1.0)

internal val Member.title: StringResource
  get() =
    when (this) {
      Member.SUN -> Res.string.gears_sun
      Member.CARRIER -> Res.string.gears_carrier
      Member.RING -> Res.string.gears_ring
    }

/** A planetary set: sun, planets on a carrier and a ring, one held, one turned and the third the output. */
@Composable
internal fun PlanetaryMode(screen: GearsContext) {
  val inputs = screen.inputs
  val speed = screen.speedRpm
  val set =
    Planetary(
      sun = inputs.sun.number(),
      ring = inputs.ring.number(),
      planets = inputs.planets.number(),
      fixed = inputs.fixed,
      input = inputs.input,
      speed = speed,
      torque = if (inputs.drive == Drive.TORQUE) screen.torque() else torqueFromPower(screen.powerWatts, speed),
      efficiency = inputs.efficiency.number() / PERCENT,
    )
  val result = set.solve()
  PhysicsScreen(
    title = Res.string.gears_title,
    openDrawer = screen.openDrawer,
    live = {
      ModeSelector(screen)
      LiveCard {
        PlanetaryDrawing(set, result)
        Member.entries.forEach { member ->
          Hint(stringResource(member.title) + ": " + screen.texts.speed(result.speeds.getValue(member)))
        }
        Hint(stringResource(Res.string.gears_planetary_hint))
      }
      PlanetaryAnswers(result, screen.texts)
    },
    settings = {
      ExamplesCard(screen)
      PlanetaryCard(screen)
      UnitsCard(screen, rack = false)
      SavedCard(screen)
    },
    explanation = {
      Card(stringResource(Res.string.gears_how)) {
        Step(
          Res.string.gears_mode_planetary,
          Res.string.gears_planetary_text,
          "z_s · n_s + z_r · n_r = (z_s + z_r) · n_c",
          "z_p = (z_r − z_s) / 2",
        )
      }
    },
  )
}

@Composable
private fun PlanetaryAnswers(result: PlanetaryResult, texts: GearTexts) {
  Card(stringResource(Res.string.gears_output) + ": " + stringResource(result.output.title)) {
    Answers(
      Res.string.gears_ratio to texts.ratio(result.ratio),
      Res.string.gears_output_speed to texts.speed(result.speeds.getValue(result.output)),
    )
    Answers(
      Res.string.gears_output_torque to texts.torque(result.outputTorque),
      Res.string.gears_planet_teeth to texts.plain(result.planetTeeth),
    )
    Answers(
      Res.string.gears_planet_spin to texts.speed(result.planetSpin),
      Res.string.gears_output_power to texts.power(result.outputPower),
    )
    Answers(Res.string.gears_input_power to texts.power(result.inputPower))
    result.problems.forEach { problem ->
      val text =
        when (problem) {
          PlanetaryProblem.PLANET_TEETH -> Res.string.gears_planet_teeth_problem
          PlanetaryProblem.SPACING -> Res.string.gears_spacing_problem
          PlanetaryProblem.CROWDED -> Res.string.gears_crowded_problem
        }
      Warning(stringResource(text))
    }
  }
}

@Composable
private fun PlanetaryCard(screen: GearsContext) {
  val inputs = screen.inputs
  val change = screen.change
  val fractional = screen.fractional
  Card(stringResource(Res.string.gears_mode_planetary)) {
    ConditionRow(SUN, inputs.sun, fractional) { change(inputs.copy(sun = it)) }
    ConditionRow(RING, inputs.ring, fractional) { change(inputs.copy(ring = it)) }
    ConditionRow(PLANETS, inputs.planets, fractional) { change(inputs.copy(planets = it)) }
    Text(stringResource(Res.string.gears_held), style = MaterialTheme.typography.labelLarge)
    ModeChips(Member.entries, inputs.fixed, { it.title }) { fixed ->
      // The part turned can't be the one held
      val input = if (inputs.input == fixed) Member.entries.first { it != fixed } else inputs.input
      change(inputs.copy(fixed = fixed, input = input))
    }
    Text(stringResource(Res.string.gears_turned), style = MaterialTheme.typography.labelLarge)
    ModeChips(Member.entries, inputs.input, { it.title }) { input ->
      val fixed = if (inputs.fixed == input) Member.entries.first { it != input } else inputs.fixed
      change(inputs.copy(input = input, fixed = fixed))
    }
    ConditionRow(screen.speedCondition(rack = false), inputs.speed, fractional) { change(inputs.copy(speed = it)) }
    ModeChips(Drive.entries, inputs.drive, { it.title }) { change(inputs.copy(drive = it)) }
    if (inputs.drive == Drive.TORQUE) {
      ConditionRow(screen.torqueCondition(rack = false), inputs.torque, fractional) { change(inputs.copy(torque = it)) }
    } else {
      ConditionRow(screen.powerCondition, inputs.power, fractional) { change(inputs.copy(power = it)) }
    }
    ConditionRow(PLANET_EFFICIENCY, inputs.efficiency, fractional) { change(inputs.copy(efficiency = it)) }
  }
}

/** Trains of one to three stages that give a wanted ratio, to open as a gear train. */
@Composable
internal fun FindMode(screen: GearsContext) {
  val inputs = screen.inputs
  val target = inputs.target.number()
  val minTeeth = inputs.minTeeth.number().roundToInt()
  val maxTeeth = inputs.maxTeeth.number().roundToInt()
  val suggestions by
    produceState<List<Suggestion>?>(null, target, inputs.stages, minTeeth, maxTeeth) {
      value = null
      value = withContext(Dispatchers.Default) { findTrains(target, inputs.stages, minTeeth, maxTeeth, SUGGESTIONS) }
    }
  PhysicsScreen(
    title = Res.string.gears_title,
    openDrawer = screen.openDrawer,
    live = {
      ModeSelector(screen)
      Card(stringResource(Res.string.gears_mode_find)) {
        val found = suggestions
        when {
          found == null -> Hint(stringResource(Res.string.gears_searching))
          found.isEmpty() -> Hint(stringResource(Res.string.gears_find_none))
          else -> found.forEach { SuggestionRow(it, screen) }
        }
      }
    },
    settings = {
      Card(stringResource(Res.string.gears_target)) {
        ConditionRow(TARGET, inputs.target, screen.fractional) { screen.change(inputs.copy(target = it)) }
        Text(stringResource(Res.string.gears_stages), style = MaterialTheme.typography.labelLarge)
        NameChips((1..MAX_STAGES).toList(), inputs.stages, { it.toString() }) { screen.change(inputs.copy(stages = it)) }
        ConditionRow(MIN_TEETH_FIELD, inputs.minTeeth, screen.fractional) { screen.change(inputs.copy(minTeeth = it)) }
        ConditionRow(MAX_TEETH_FIELD, inputs.maxTeeth, screen.fractional) { screen.change(inputs.copy(maxTeeth = it)) }
      }
    },
    explanation = {
      Card(stringResource(Res.string.gears_how)) { Step(Res.string.gears_hunting, Res.string.gears_find_hint) }
    },
  )
}

/** A train found: its teeth stage by stage, how close it gets, and a button that opens it. */
@Composable
private fun SuggestionRow(suggestion: Suggestion, screen: GearsContext) {
  val texts = screen.texts
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        suggestion.stages.joinToString("  ·  ") { "${it.driver} → ${it.driven}" },
        style = MaterialTheme.typography.titleMedium,
      )
      val closeness =
        if (suggestion.error < EXACT) stringResource(Res.string.gears_exact)
        else stringResource(Res.string.gears_off_by) + " " + texts.percent(suggestion.error)
      val wear = if (suggestion.hunting) "  ·  " + stringResource(Res.string.gears_hunting) else ""
      Text(
        texts.ratio(suggestion.ratio) + "  ·  " + closeness + wear,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    TextButton(onClick = { screen.change(screen.inputs.opening(suggestion)) }) { Text(stringResource(Res.string.gears_use)) }
  }
}

/** The gear train of [suggestion]: each stage after the first starts on the shaft of the stage before. */
private fun GearsInputs.opening(suggestion: Suggestion): GearsInputs =
  copy(
    mode = GearsMode.TRAIN,
    teeth = suggestion.stages.first().driver.toString(),
    gears =
      suggestion.stages.flatMapIndexed { index, stage ->
        listOfNotNull(
          if (index > 0) GearEntry(stage.driver.toString(), Link.SHAFT) else null,
          GearEntry(stage.driven.toString(), Link.MESH),
        )
      },
  )

/** How a gear train works, link by link, then how its teeth hold. */
@Composable
internal fun TrainExplanation(system: PitchSystem) {
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
    Step(Res.string.gears_link_ring, Res.string.gears_ring_text, "n₂ = n₁ · z₁ / z₂", "a = (d₂ − d₁) / 2")
    Step(Res.string.gears_link_belt, Res.string.gears_belt_text, "n₂ = n₁ · z₁ / z₂")
    Step(Res.string.gears_link_worm, Res.string.gears_worm_text, "n₂ = n₁ · z₁ / z₂")
    Step(Res.string.gears_link_rack, Res.string.gears_rack_text, "v = π · d · n / 60", "F = 2 · T / d")
    Step(
      Res.string.gears_contact,
      Res.string.gears_contact_text,
      "ε = (√(ra₁² − rb₁²) + √(ra₂² − rb₂²) − a · sin α) / (π · m · cos α)",
    )
    Step(Res.string.gears_stress, Res.string.gears_stress_text, "σ = Kv · Ft / (b · m · Y)", "Kv = (6.1 + v) / 6.1")
  }
}

private const val SUGGESTIONS = 8
private const val EXACT = 1e-9
