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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.calculatorpascaline.feature.calculator.physics.LiveCard
import io.github.calculatorpascaline.feature.calculator.physics.PhysicsScreen
import io.github.calculatorpascaline.feature.calculator.physics.number
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import io.github.calculatorpascaline.feature.calculator.projectile.Condition
import io.github.calculatorpascaline.feature.calculator.projectile.ConditionRow
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.gears_add
import pascaline.core.common.generated.resources.gears_center
import pascaline.core.common.generated.resources.gears_contact
import pascaline.core.common.generated.resources.gears_contact_low
import pascaline.core.common.generated.resources.gears_diameter
import pascaline.core.common.generated.resources.gears_diametral
import pascaline.core.common.generated.resources.gears_efficiency
import pascaline.core.common.generated.resources.gears_face_width
import pascaline.core.common.generated.resources.gears_gear
import pascaline.core.common.generated.resources.gears_given_motor
import pascaline.core.common.generated.resources.gears_given_output
import pascaline.core.common.generated.resources.gears_hint
import pascaline.core.common.generated.resources.gears_input
import pascaline.core.common.generated.resources.gears_input_power
import pascaline.core.common.generated.resources.gears_link_belt
import pascaline.core.common.generated.resources.gears_link_rack
import pascaline.core.common.generated.resources.gears_link_ring
import pascaline.core.common.generated.resources.gears_link_worm
import pascaline.core.common.generated.resources.gears_material_acetal
import pascaline.core.common.generated.resources.gears_material_bronze
import pascaline.core.common.generated.resources.gears_material_cast_iron
import pascaline.core.common.generated.resources.gears_material_hardened_steel
import pascaline.core.common.generated.resources.gears_material_nylon
import pascaline.core.common.generated.resources.gears_material_steel
import pascaline.core.common.generated.resources.gears_meshes
import pascaline.core.common.generated.resources.gears_module
import pascaline.core.common.generated.resources.gears_motor
import pascaline.core.common.generated.resources.gears_move_down
import pascaline.core.common.generated.resources.gears_move_up
import pascaline.core.common.generated.resources.gears_output_power
import pascaline.core.common.generated.resources.gears_output_speed
import pascaline.core.common.generated.resources.gears_output_torque
import pascaline.core.common.generated.resources.gears_rack_force
import pascaline.core.common.generated.resources.gears_rack_speed
import pascaline.core.common.generated.resources.gears_ratio
import pascaline.core.common.generated.resources.gears_remove
import pascaline.core.common.generated.resources.gears_ring_small
import pascaline.core.common.generated.resources.gears_safety
import pascaline.core.common.generated.resources.gears_safety_low
import pascaline.core.common.generated.resources.gears_same_shaft
import pascaline.core.common.generated.resources.gears_self_locking
import pascaline.core.common.generated.resources.gears_side_view
import pascaline.core.common.generated.resources.gears_speed
import pascaline.core.common.generated.resources.gears_stage_diametral
import pascaline.core.common.generated.resources.gears_stage_module
import pascaline.core.common.generated.resources.gears_starts
import pascaline.core.common.generated.resources.gears_strength
import pascaline.core.common.generated.resources.gears_strength_hint
import pascaline.core.common.generated.resources.gears_stress
import pascaline.core.common.generated.resources.gears_tap_hint
import pascaline.core.common.generated.resources.gears_teeth
import pascaline.core.common.generated.resources.gears_title
import pascaline.core.common.generated.resources.gears_tooth_speed
import pascaline.core.common.generated.resources.gears_torque
import pascaline.core.common.generated.resources.gears_train
import pascaline.core.common.generated.resources.gears_train_efficiency
import pascaline.core.common.generated.resources.gears_undercut
import pascaline.core.common.generated.resources.gears_worm_efficiency
import pascaline.core.common.generated.resources.gears_worm_note

private val TEETH = Condition("z", "", Res.string.gears_teeth, 6.0..120.0, 1.0)
private val MODULE = Condition("m", "mm", Res.string.gears_module, 0.5..10.0, 0.25)
private val DIAMETRAL = Condition("Pd", "1/in", Res.string.gears_diametral, 2.0..48.0, 1.0)
private val STAGE_MODULE = Condition("m", "mm", Res.string.gears_stage_module, 0.5..10.0, 0.25)
private val STAGE_DIAMETRAL = Condition("Pd", "1/in", Res.string.gears_stage_diametral, 2.0..48.0, 1.0)
private val EFFICIENCY = Condition("η", "%", Res.string.gears_efficiency, 80.0..100.0, 0.5)
private val STARTS = Condition("z₁", "", Res.string.gears_starts, 1.0..4.0, 1.0)
private val WORM_EFFICIENCY = Condition("η", "%", Res.string.gears_worm_efficiency, 20.0..90.0, 1.0)
private val FACE_WIDTH = Condition("b", "mm", Res.string.gears_face_width, 2.0..100.0, 1.0)

private val Material.title: StringResource
  get() =
    when (this) {
      Material.HARDENED_STEEL -> Res.string.gears_material_hardened_steel
      Material.STEEL -> Res.string.gears_material_steel
      Material.CAST_IRON -> Res.string.gears_material_cast_iron
      Material.BRONZE -> Res.string.gears_material_bronze
      Material.ACETAL -> Res.string.gears_material_acetal
      Material.NYLON -> Res.string.gears_material_nylon
    }

/** The typed train, with the motor worked out when the output is what is given. */
private fun GearsContext.solveTrain(): Pair<GearTrain, TrainResult> {
  val inputs = inputs
  val base =
    GearTrain(
      teeth = inputs.teeth.number(),
      pitch = if (inputs.system == PitchSystem.MODULE) inputs.module.number() else inputs.diametral.number(),
      system = inputs.system,
      speed = 1.0,
      torque = 1.0,
      efficiency = inputs.efficiency.number() / PERCENT,
      gears =
        inputs.gears.map {
          GearSpec(it.teeth.number(), it.link, it.starts.number(), it.efficiency.number() / PERCENT, it.pitch.number())
        },
    )
  val train =
    if (inputs.given) {
      val torque = if (inputs.drive == Drive.TORQUE) torque() else torqueFromPower(powerWatts, speedRpm)
      base.copy(speed = speedRpm, torque = torque)
    } else {
      // Per rpm and per N·m of the motor, the output turns and pushes this much
      val unit = base.solve()
      val rack = unit.endsOnRack
      val wanted = inputs.speed.number() / if (rack) inputs.units.speedPerMeterPerSecond else inputs.speedUnit.perRpm
      val speed = wanted / abs(unit.output.speed)
      val torque =
        if (inputs.drive == Drive.TORQUE) torque(rack) / unit.output.torque
        else torqueFromPower(powerWatts / unit.efficiency, speed)
      base.copy(speed = speed, torque = torque)
    }
  return train to train.solve()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrainMode(screen: GearsContext) {
  val inputs = screen.inputs
  val (train, result) = screen.solveTrain()
  val requesters = remember(inputs.gears.size) { List(inputs.gears.size + 1) { BringIntoViewRequester() } }
  val scope = rememberCoroutineScope()
  PhysicsScreen(
    title = Res.string.gears_title,
    openDrawer = screen.openDrawer,
    live = {
      ModeSelector(screen)
      LiveCard {
        GearDrawing(result.gears) { index -> scope.launch { requesters.getOrNull(index)?.bringIntoView() } }
        Hint(stringResource(Res.string.gears_hint) + " " + stringResource(Res.string.gears_tap_hint))
        if (result.gears.any { it.link == Link.WORM } && result.gears.last().link != Link.WORM) {
          Hint(stringResource(Res.string.gears_worm_note))
        }
        if (result.gears.any { it.level > 0 }) {
          GearSideView(result.gears)
          Hint(stringResource(Res.string.gears_side_view))
        }
      }
      TrainCard(result, screen.texts, inputs.given)
    },
    settings = {
      ExamplesCard(screen)
      Box(Modifier.bringIntoViewRequester(requesters[0])) { InputCard(screen, result) }
      inputs.gears.forEachIndexed { index, entry ->
        GearCard(
          screen = screen,
          index = index,
          entry = entry,
          result = result,
          check = train.check(result, index + 1, inputs.faceWidth.number(), inputs.material),
          modifier = Modifier.bringIntoViewRequester(requesters[index + 1]),
        )
      }
      FilledTonalButton(
        onClick = {
          val teeth = inputs.gears.lastOrNull { it.link != Link.RACK }?.teeth ?: inputs.teeth
          screen.change(inputs.copy(gears = inputs.gears + GearEntry(teeth, Link.MESH)))
        }
      ) {
        Icon(Symbols.Add, contentDescription = null)
        Text(stringResource(Res.string.gears_add), modifier = Modifier.padding(start = 8.dp))
      }
      StrengthCard(screen)
      UnitsCard(screen, rack = !inputs.given && result.endsOnRack)
      SavedCard(screen)
    },
    explanation = { TrainExplanation(inputs.system) },
  )
}

/** Ratio, speed, torque and power from the first gear to the last, and the motor needed when the output is given. */
@Composable
private fun TrainCard(result: TrainResult, texts: GearTexts, given: Boolean) {
  val rack = result.endsOnRack
  Card(stringResource(Res.string.gears_train)) {
    if (rack) {
      Answers(
        Res.string.gears_rack_speed to texts.speed(result.output.speed, rack = true),
        Res.string.gears_rack_force to texts.torque(result.output.torque, rack = true),
      )
    } else {
      Answers(
        Res.string.gears_ratio to texts.ratio(result.ratio),
        Res.string.gears_output_speed to texts.speed(result.output.speed, result.output.rightAngle),
      )
      Answers(
        Res.string.gears_output_torque to texts.torque(result.output.torque),
        Res.string.gears_train_efficiency to texts.percent(result.efficiency),
      )
    }
    Answers(
      Res.string.gears_input_power to texts.power(result.inputPower),
      Res.string.gears_output_power to texts.power(result.outputPower),
    )
  }
  if (!given) {
    Card(stringResource(Res.string.gears_motor)) {
      Answers(
        Res.string.gears_speed to texts.speed(result.gears.first().speed),
        Res.string.gears_torque to texts.torque(result.gears.first().torque),
      )
    }
  }
}

/** The first gear and the motor, or what the output needs. */
@Composable
private fun InputCard(screen: GearsContext, result: TrainResult) {
  val inputs = screen.inputs
  val change = screen.change
  val fractional = screen.fractional
  val rack = !inputs.given && result.endsOnRack
  Card(stringResource(Res.string.gears_input)) {
    ConditionRow(TEETH, inputs.teeth, fractional) { change(inputs.copy(teeth = it)) }
    if (undercut(inputs.teeth.number(), inputs.gears.firstOrNull()?.link)) Warning(stringResource(Res.string.gears_undercut))
    ModeChips(PitchSystem.entries, inputs.system, { if (it == PitchSystem.MODULE) Res.string.gears_module else Res.string.gears_diametral }) {
      change(inputs.copy(system = it))
    }
    if (inputs.system == PitchSystem.MODULE) {
      ConditionRow(MODULE, inputs.module, fractional) { change(inputs.copy(module = it)) }
    } else {
      ConditionRow(DIAMETRAL, inputs.diametral, fractional) { change(inputs.copy(diametral = it)) }
    }
    ModeChips(Given.entries, if (inputs.given) Given.MOTOR else Given.OUTPUT, { it.title }) {
      change(inputs.copy(given = it == Given.MOTOR))
    }
    ConditionRow(screen.speedCondition(rack), inputs.speed, fractional) { change(inputs.copy(speed = it)) }
    ModeChips(Drive.entries, inputs.drive, { it.title }) { change(inputs.copy(drive = it)) }
    if (inputs.drive == Drive.TORQUE) {
      ConditionRow(screen.torqueCondition(rack), inputs.torque, fractional) { change(inputs.copy(torque = it)) }
    } else {
      ConditionRow(screen.powerCondition, inputs.power, fractional) { change(inputs.copy(power = it)) }
    }
    ConditionRow(EFFICIENCY, inputs.efficiency, fractional) { change(inputs.copy(efficiency = it)) }
    GearAnswers(result.gears.first(), screen.texts)
  }
}

/** Whether the motor or the output is given. */
private enum class Given(val title: StringResource) {
  MOTOR(Res.string.gears_given_motor),
  OUTPUT(Res.string.gears_given_output),
}

/**
 * Whether a gear of [teeth] meshes with teeth, by its [own] link or the [next] gear's, and has so few that they get cut
 * away at their root.
 */
private fun undercut(teeth: Double, next: Link?, own: Link? = null): Boolean {
  val withTeeth = setOf(Link.MESH, Link.INTERNAL, Link.RACK)
  return teeth < UNDERCUT_TEETH && (own in withTeeth || next in withTeeth)
}

/** Speed, torque and ratio of a gear, then its size and how far it is from the gear before it. */
@Composable
private fun GearAnswers(state: GearState, texts: GearTexts) {
  if (state.link == Link.RACK) {
    Answers(
      Res.string.gears_rack_speed to texts.speed(state.speed, rack = true),
      Res.string.gears_rack_force to texts.torque(state.torque, rack = true),
    )
    return
  }
  Answers(
    Res.string.gears_speed to texts.speed(state.speed, state.rightAngle),
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

/** A gear after the input: how many teeth, how it is turned, what it does, and how its teeth hold. */
@Composable
private fun GearCard(
  screen: GearsContext,
  index: Int,
  entry: GearEntry,
  result: TrainResult,
  check: MeshCheck?,
  modifier: Modifier,
) {
  val inputs = screen.inputs
  val number = index + 2
  val state = result.gears[index + 1]
  fun change(changed: GearEntry) = screen.change(inputs.copy(gears = inputs.gears.toMutableList().also { it[index] = changed }))
  Column(
    modifier =
      modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    GearHeader(number, isLast = index == inputs.gears.lastIndex, onMove = { by -> screen.change(inputs.copy(gears = inputs.gears.moved(index, index + by))) }) {
      screen.change(inputs.copy(gears = inputs.gears.toMutableList().also { it.removeAt(index) }))
    }
    if (entry.link != Link.RACK) ConditionRow(TEETH, entry.teeth, screen.fractional) { change(entry.copy(teeth = it)) }
    LinkChips(entry.link, number) { link ->
      // A new stage starts with the teeth of the stage before
      val pitch = if (link == Link.SHAFT && entry.pitch.isEmpty()) screen.texts.plain(result.gears[index].pitch) else entry.pitch
      change(entry.copy(link = link, pitch = pitch))
    }
    LinkFields(screen, entry, state, ::change)
    GearWarnings(entry, state, result.gears[index], inputs.gears.getOrNull(index + 1)?.link)
    GearAnswers(state, screen.texts)
    check?.let { CheckAnswers(it, screen.texts) }
  }
}

@Composable
private fun GearHeader(number: Int, isLast: Boolean, onMove: (Int) -> Unit, onRemove: () -> Unit) {
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
    IconButton(onClick = onRemove) { Icon(Symbols.Delete, contentDescription = stringResource(Res.string.gears_remove)) }
  }
}

/** How a gear is turned by gear [number] − 1. */
@Composable
private fun LinkChips(selected: Link, number: Int, onChoose: (Link) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Link.entries.forEach { link ->
      val label =
        when (link) {
          Link.MESH -> stringResource(Res.string.gears_meshes, number - 1)
          Link.SHAFT -> stringResource(Res.string.gears_same_shaft, number - 1)
          Link.INTERNAL -> stringResource(Res.string.gears_link_ring)
          Link.BELT -> stringResource(Res.string.gears_link_belt)
          Link.WORM -> stringResource(Res.string.gears_link_worm)
          Link.RACK -> stringResource(Res.string.gears_link_rack)
        }
      FilterChip(selected = link == selected, onClick = { onChoose(link) }, label = { Text(label) })
    }
  }
}

/** What only some links have: the tooth size of a new stage, or the threads and efficiency of a worm. */
@Composable
private fun LinkFields(screen: GearsContext, entry: GearEntry, state: GearState, change: (GearEntry) -> Unit) {
  when (entry.link) {
    Link.SHAFT -> {
      val condition = if (screen.inputs.system == PitchSystem.MODULE) STAGE_MODULE else STAGE_DIAMETRAL
      ConditionRow(condition, entry.pitch.ifEmpty { screen.texts.plain(state.pitch) }, screen.fractional) {
        change(entry.copy(pitch = it))
      }
    }
    Link.WORM -> {
      ConditionRow(STARTS, entry.starts, screen.fractional) { change(entry.copy(starts = it)) }
      ConditionRow(WORM_EFFICIENCY, entry.efficiency, screen.fractional) { change(entry.copy(efficiency = it)) }
      if (entry.efficiency.number() <= SELF_LOCKING_PERCENT) Hint(stringResource(Res.string.gears_self_locking))
    }
    else -> Unit
  }
}

@Composable
private fun GearWarnings(entry: GearEntry, state: GearState, before: GearState, next: Link?) {
  if (undercut(state.teeth, next, entry.link)) Warning(stringResource(Res.string.gears_undercut))
  if (entry.link == Link.INTERNAL && entry.teeth.number() <= before.teeth) Warning(stringResource(Res.string.gears_ring_small))
}

/** How the teeth of a gear and the one before it get along. */
@Composable
private fun CheckAnswers(check: MeshCheck, texts: GearTexts) {
  Answers(
    Res.string.gears_contact to texts.plain(check.contactRatio),
    Res.string.gears_tooth_speed to texts.meterSpeed(check.toothSpeed),
  )
  Answers(
    Res.string.gears_stress to texts.stress(check.stress),
    Res.string.gears_safety to texts.plain(check.safety),
  )
  if (check.contactRatio < ROUGH_CONTACT) Warning(stringResource(Res.string.gears_contact_low))
  if (check.safety < LOW_SAFETY) Warning(stringResource(Res.string.gears_safety_low))
}

/** How wide the teeth are and what they are made of, for the stress in every mesh. */
@Composable
private fun StrengthCard(screen: GearsContext) {
  val inputs = screen.inputs
  Card(stringResource(Res.string.gears_strength)) {
    ConditionRow(FACE_WIDTH, inputs.faceWidth, screen.fractional) { screen.change(inputs.copy(faceWidth = it)) }
    ModeChips(Material.entries, inputs.material, { it.title }) { screen.change(inputs.copy(material = it)) }
    Hint(stringResource(Res.string.gears_strength_hint))
  }
}

private fun <T> List<T>.moved(from: Int, to: Int): List<T> =
  if (to !in indices) this else toMutableList().also { it.add(to, it.removeAt(from)) }

private const val SELF_LOCKING_PERCENT = 50.0
private val CARD_SHAPE = RoundedCornerShape(24.dp)
