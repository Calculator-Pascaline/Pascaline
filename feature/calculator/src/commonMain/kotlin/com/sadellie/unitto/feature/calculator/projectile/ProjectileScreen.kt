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

package com.sadellie.unitto.feature.calculator.projectile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.FineSlider
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.probability.sliderValue
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.place_tab_moon
import unitto.core.common.generated.resources.projectile_air
import unitto.core.common.generated.resources.projectile_air_effect
import unitto.core.common.generated.resources.projectile_air_hint
import unitto.core.common.generated.resources.projectile_angle
import unitto.core.common.generated.resources.projectile_baseball
import unitto.core.common.generated.resources.projectile_basketball
import unitto.core.common.generated.resources.projectile_best_angle
import unitto.core.common.generated.resources.projectile_density
import unitto.core.common.generated.resources.projectile_diameter
import unitto.core.common.generated.resources.projectile_earth
import unitto.core.common.generated.resources.projectile_explain_air
import unitto.core.common.generated.resources.projectile_explain_air_text
import unitto.core.common.generated.resources.projectile_explain_apex
import unitto.core.common.generated.resources.projectile_explain_apex_text
import unitto.core.common.generated.resources.projectile_explain_forces
import unitto.core.common.generated.resources.projectile_explain_forces_text
import unitto.core.common.generated.resources.projectile_explain_impact
import unitto.core.common.generated.resources.projectile_explain_impact_text
import unitto.core.common.generated.resources.projectile_explain_motion
import unitto.core.common.generated.resources.projectile_explain_motion_text
import unitto.core.common.generated.resources.projectile_explain_range
import unitto.core.common.generated.resources.projectile_explain_range_text
import unitto.core.common.generated.resources.projectile_explain_result
import unitto.core.common.generated.resources.projectile_explain_result_text
import unitto.core.common.generated.resources.projectile_explain_split
import unitto.core.common.generated.resources.projectile_explain_split_text
import unitto.core.common.generated.resources.projectile_explain_steps
import unitto.core.common.generated.resources.projectile_explain_steps_text
import unitto.core.common.generated.resources.projectile_explain_time
import unitto.core.common.generated.resources.projectile_explain_time_text
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_gravity
import unitto.core.common.generated.resources.projectile_height
import unitto.core.common.generated.resources.projectile_hint
import unitto.core.common.generated.resources.projectile_initial
import unitto.core.common.generated.resources.projectile_invalid
import unitto.core.common.generated.resources.projectile_jupiter
import unitto.core.common.generated.resources.projectile_launch
import unitto.core.common.generated.resources.projectile_mars
import unitto.core.common.generated.resources.projectile_mass
import unitto.core.common.generated.resources.projectile_max_height
import unitto.core.common.generated.resources.projectile_max_range
import unitto.core.common.generated.resources.projectile_max_range_air
import unitto.core.common.generated.resources.projectile_max_range_hint
import unitto.core.common.generated.resources.projectile_max_range_how
import unitto.core.common.generated.resources.projectile_max_range_how_text
import unitto.core.common.generated.resources.projectile_ping_pong
import unitto.core.common.generated.resources.projectile_range
import unitto.core.common.generated.resources.projectile_bullet
import unitto.core.common.generated.resources.projectile_shot_put
import unitto.core.common.generated.resources.projectile_soccer
import unitto.core.common.generated.resources.projectile_speed
import unitto.core.common.generated.resources.projectile_tennis
import unitto.core.common.generated.resources.projectile_terminal
import unitto.core.common.generated.resources.projectile_time
import unitto.core.common.generated.resources.projectile_title
import unitto.core.common.generated.resources.projectile_use_angle
import unitto.core.common.generated.resources.projectile_with_air
import unitto.core.common.generated.resources.projectile_without_air
import unitto.core.common.generated.resources.projectile_your_throw

/** One starting condition, typed or on its slider. */
internal class Condition(
  val symbol: String,
  val unit: String,
  val label: StringResource,
  val range: ClosedFloatingPointRange<Double>,
  val step: Double,
)

private val SPEED = Condition("v₀", "m/s", Res.string.projectile_speed, 0.0..100.0, 0.5)
private val ANGLE = Condition("θ", "°", Res.string.projectile_angle, -90.0..90.0, 1.0)
private val HEIGHT = Condition("h₀", "m", Res.string.projectile_height, 0.0..100.0, 0.5)
internal val GRAVITY = Condition("g", "m/s²", Res.string.projectile_gravity, 0.5..30.0, 0.01)

private val DIAMETER = Condition("d", "cm", Res.string.projectile_diameter, 1.0..100.0, 0.1)
private val MASS = Condition("m", "kg", Res.string.projectile_mass, 0.0..10.0, 0.001)
private val DENSITY = Condition("ρ", "kg/m³", Res.string.projectile_density, 0.0..3.0, 0.001)

/** Gravity of a world and how dense its air is at the ground. */
internal class Planet(val name: StringResource, val gravity: String, val density: String)

private val PLANETS =
  listOf(
    Planet(Res.string.projectile_earth, "9.81", "1.225"),
    Planet(Res.string.place_tab_moon, "1.62", "0"),
    Planet(Res.string.projectile_mars, "3.71", "0.02"),
    Planet(Res.string.projectile_jupiter, "24.79", "0.16"),
  )

/** A ball you could throw, its size in centimeters and its weight in kilograms. */
/** A thing to throw, with the drag of its shape, and its own speed when it has one, like a bullet. */
private class Ball(
  val name: StringResource,
  val diameter: String,
  val mass: String,
  val drag: Double = SPHERE_DRAG,
  val speed: String? = null,
)

private val BALLS =
  listOf(
    Ball(Res.string.projectile_ping_pong, "4", "0.0027"),
    Ball(Res.string.projectile_tennis, "6.7", "0.058"),
    Ball(Res.string.projectile_baseball, "7.4", "0.145"),
    Ball(Res.string.projectile_soccer, "22", "0.43"),
    Ball(Res.string.projectile_basketball, "24", "0.62"),
    Ball(Res.string.projectile_shot_put, "12", "7.26"),
    Ball(Res.string.projectile_bullet, "0.9", "0.008", BULLET_DRAG, speed = "360"),
  )

/** A throw without air: the conditions, the flight it gives and how that is worked out. */
@Composable
internal fun ProjectileRoute(openDrawer: () -> Unit) {
  val preferences = koinInject<UserPreferencesRepository>()
  val symbols by
    remember(preferences) { preferences.calculatorPrefs.map { it.formatterSymbols.fractional } }
      .collectAsState(initial = ".")
  var speed by rememberSaveable { mutableStateOf("25") }
  var angle by rememberSaveable { mutableStateOf("50") }
  var height by rememberSaveable { mutableStateOf("5") }
  var gravity by rememberSaveable { mutableStateOf("9.81") }
  var hasAir by rememberSaveable { mutableStateOf(false) }
  var diameter by rememberSaveable { mutableStateOf("6.7") }
  var mass by rememberSaveable { mutableStateOf("0.058") }
  var density by rememberSaveable { mutableStateOf("1.225") }
  fun number(value: String) = value.toNumberOrNull() ?: Double.NaN
  val launch = Launch(number(speed), number(angle), number(height), number(gravity))
  // A shape only comes with a preset: anything else is a ball
  val shape = BALLS.firstOrNull { it.diameter.toDouble() == diameter.toNumberOrNull() && it.mass.toDouble() == mass.toNumberOrNull() }
  val air = Air(number(mass), number(diameter) / CENTIMETERS, number(density), shape?.drag ?: SPHERE_DRAG).takeIf { hasAir }
  val flight = remember(launch, air) { launch.trajectory(air) }
  val withoutAir = remember(launch, air) { launch.flight().takeIf { air != null && air.density > 0 } }
  val format = { value: Double -> value.rounded(DECIMALS).replace(".", symbols) }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.projectile_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    val conditions: @Composable () -> Unit = {
      Card(stringResource(Res.string.projectile_initial)) {
        ConditionRow(SPEED, speed, symbols) { speed = it }
        ConditionRow(ANGLE, angle, symbols) { angle = it }
        ConditionRow(HEIGHT, height, symbols) { height = it }
        ConditionRow(GRAVITY, gravity, symbols) { gravity = it }
        PlanetChips(gravity) { planet ->
          gravity = planet.gravity.replace(".", symbols)
          density = planet.density.replace(".", symbols)
        }
      }
      Card(stringResource(Res.string.projectile_air)) {
        AirSwitch(hasAir) { hasAir = it }
        if (hasAir) {
          BallChips(diameter, mass) { ball ->
            diameter = ball.diameter.replace(".", symbols)
            mass = ball.mass.replace(".", symbols)
            ball.speed?.let { speed = it }
          }
          ConditionRow(DIAMETER, diameter, symbols) { diameter = it }
          ConditionRow(MASS, mass, symbols) { mass = it }
          ConditionRow(DENSITY, density, symbols) { density = it }
        }
      }
    }
    val flightView: @Composable () -> Unit = {
      if (flight == null) {
        Text(stringResource(Res.string.projectile_invalid), color = MaterialTheme.colorScheme.error)
      } else {
        FlightCard(flight, withoutAir, format) { newSpeed, newAngle ->
          speed = (round(newSpeed.coerceIn(SPEED.range) * 2) / 2).rounded(1).replace(".", symbols)
          angle = round(newAngle.coerceIn(ANGLE.range)).rounded(0)
        }
      }
    }
    val explanation: @Composable () -> Unit = {
      MaxRangeCard(launch, air, flight, format) { angle = it.rounded(0) }
      when {
        flight == null -> Unit
        air != null && withoutAir != null -> {
          AirResults(flight, withoutAir, air, format)
          AirExplanation(flight, air, format)
        }
        else -> Explanation(flight, format)
      }
    }
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val spacing = Arrangement.spacedBy(12.dp)
      if (maxWidth >= WIDE_LAYOUT_WIDTH) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = spacing) {
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = spacing) {
            flightView()
            conditions()
          }
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = spacing) {
            explanation()
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
          verticalArrangement = spacing,
        ) {
          flightView()
          conditions()
          explanation()
        }
      }
    }
  }
}

@Composable
internal fun Card(title: String, content: @Composable () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    content()
  }
}

/** The animation, the time slider and the answers. It plays when opened and on Launch. */
@Composable
private fun FlightCard(
  flight: Trajectory,
  withoutAir: Trajectory?,
  format: (Double) -> String,
  onAimChange: (Double, Double) -> Unit,
) {
  val progress = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val duration = (flight.time * MILLIS_PER_SECOND).toInt().coerceIn(MIN_ANIMATION, MAX_ANIMATION)
  fun play() {
    scope.launch {
      progress.snapTo(0f)
      progress.animateTo(1f, tween(duration, easing = LinearEasing))
      progress.animateTo(1f + LANDING_PART, tween(LANDING_MILLIS, easing = LinearEasing))
    }
  }
  LaunchedEffect(Unit) { play() }
  // New conditions show their whole flight at once
  LaunchedEffect(flight) { if (progress.value > 0f) progress.snapTo(1f + LANDING_PART) }

  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ProjectileScene(Modifier.fillMaxWidth().height(SCENE_HEIGHT), flight, withoutAir, progress.value, format, onAimChange)
    Text(
      stringResource(Res.string.projectile_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val time = flight.time * progress.value.coerceAtMost(1f)
    Text(
      "t = ${format(time)} s · x = ${format(flight.x(time))} m · y = ${format(flight.y(time))} m · v = ${format(flight.speed(time))} m/s",
      style = MaterialTheme.typography.labelMedium,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FineSlider(
        modifier = Modifier.weight(1f),
        value = progress.value.coerceAtMost(1f),
        onValueChange = { scope.launch { progress.snapTo(it) } },
      )
      Button(onClick = ::play) { Text(stringResource(Res.string.projectile_launch)) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.projectile_range, "${format(flight.range)} m")
      Answer(Modifier.weight(1f), Res.string.projectile_max_height, "${format(flight.apexHeight)} m")
      Answer(Modifier.weight(1f), Res.string.projectile_time, "${format(flight.time)} s")
    }
  }
}

@Composable
internal fun Answer(modifier: Modifier, label: StringResource, value: String) = Answer(modifier, stringResource(label), value)

@Composable
internal fun Answer(modifier: Modifier, label: String, value: String) {
  val color = MaterialTheme.colorScheme.onPrimaryContainer
  Column(modifier.clip(TILE_SHAPE).background(MaterialTheme.colorScheme.primaryContainer).padding(10.dp)) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
  }
}

/** Name, typing box and slider. A typed number out of the usual range stays on the slider. */
@Composable
internal fun ConditionRow(condition: Condition, value: String, fractional: String, onValueChange: (String) -> Unit) {
  val number = value.toNumberOrNull()
  val start = condition.range.start
  val end = max(condition.range.endInclusive, number ?: 0.0)
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        condition.symbol,
        modifier = Modifier.width(SYMBOL_WIDTH),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(stringResource(condition.label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
      OutlinedTextField(
        modifier = Modifier.width(FIELD_WIDTH),
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = number == null,
        suffix = { Text(condition.unit) },
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      )
    }
    FineSlider(
      value = (number ?: start).coerceIn(start, end).toFloat(),
      onValueChange = { onValueChange(sliderValue(it.toDouble(), condition.step, fractional)) },
      valueRange = start.toFloat()..end.toFloat(),
      origin = 0.0.coerceIn(start, end).toFloat(),
    )
  }
}

@Composable
private fun BallChips(diameter: String, mass: String, onChoose: (Ball) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    BALLS.forEach { ball ->
      FilterChip(
        selected = diameter.toNumberOrNull() == ball.diameter.toDouble() && mass.toNumberOrNull() == ball.mass.toDouble(),
        onClick = { onChoose(ball) },
        label = { Text(stringResource(ball.name)) },
      )
    }
  }
}

@Composable
private fun AirSwitch(hasAir: Boolean, onChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      stringResource(Res.string.projectile_air_hint),
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Switch(checked = hasAir, onCheckedChange = onChange)
  }
}

/** What the air changes: how far it goes and the fastest fall. */
@Composable
private fun AirResults(
  flight: Trajectory,
  withoutAir: Trajectory,
  air: Air,
  format: (Double) -> String,
) {
  val launch = flight.launch
  val lost = (1 - flight.range / withoutAir.range) * PERCENT
  Card(stringResource(Res.string.projectile_air_effect)) {
    AirLine(Res.string.projectile_without_air, "${format(withoutAir.range)} m")
    AirLine(Res.string.projectile_with_air, "${format(flight.range)} m (−${format(lost)} %)")
    AirLine(Res.string.projectile_terminal, "${format(air.terminalSpeed(launch.gravity))} m/s")
  }
}

/** The angle to throw at to go furthest with this speed and height, and how this throw compares. */
@Composable
private fun MaxRangeCard(launch: Launch, air: Air?, flight: Trajectory?, format: (Double) -> String, onUseAngle: (Double) -> Unit) {
  // The angle doesn't change the answer, so moving it doesn't search again
  val aimless = launch.copy(angle = 0.0)
  val best = remember(aimless, air) { aimless.maxRange(air) } ?: return
  val (angle, range) = best
  val hasAir = air != null && air.density > 0
  Card(stringResource(Res.string.projectile_max_range)) {
    Text(
      stringResource(Res.string.projectile_max_range_hint),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.projectile_best_angle, "${format(angle)}°")
      Answer(Modifier.weight(1f), Res.string.projectile_range, "${format(range)} m")
    }
    if (hasAir) {
      Text(stringResource(Res.string.projectile_max_range_air), style = MaterialTheme.typography.bodySmall)
    } else {
      val v = format(launch.speed)
      val g = format(launch.gravity)
      val h = format(launch.height)
      Step(
        Res.string.projectile_max_range_how,
        Res.string.projectile_max_range_how_text,
        "tan θ = v₀ / √(v₀² + 2 · g · h₀) = $v / √($v² + 2 · $g · $h) → θ = ${format(angle)}°",
        "x = v₀ · √(v₀² + 2 · g · h₀) / g = ${format(range)} m",
      )
    }
    if (flight != null && abs(launch.angle - angle) >= HALF) {
      AirLine(Res.string.projectile_your_throw, "${format(flight.range)} m (${format(flight.range / range * PERCENT)} %)")
      TextButton(onClick = { onUseAngle(angle) }) { Text(stringResource(Res.string.projectile_use_angle)) }
    }
  }
}

@Composable
private fun AirLine(label: StringResource, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
  }
}

/** Air has no neat formula, so these are the forces and how the steps are taken. */
@Composable
private fun AirExplanation(flight: Trajectory, air: Air, format: (Double) -> String) {
  val launch = flight.launch
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.projectile_explain_air,
      Res.string.projectile_explain_air_text,
      "A = π · d² / 4 = ${formatSmall(air.area)} m²",
      "k = ½ · ρ · Cd · A / m = ½ · ${formatSmall(air.density)} · ${air.dragCoefficient} · ${formatSmall(air.area)} / ${formatSmall(air.mass)} = ${formatSmall(air.drag)} /m",
    )
    Step(
      Res.string.projectile_explain_forces,
      Res.string.projectile_explain_forces_text,
      "ax = −k · v · vx",
      "ay = −g − k · v · vy = −${format(launch.gravity)} − ${formatSmall(air.drag)} · v · vy",
    )
    Step(
      Res.string.projectile_explain_steps,
      Res.string.projectile_explain_steps_text,
      "Δt = $TIME_STEP s · ${(flight.time / TIME_STEP).toInt()} ×",
      "v(t + Δt) ≈ v(t) + a · Δt",
    )
    Step(
      Res.string.projectile_explain_result,
      Res.string.projectile_explain_result_text,
      "t = ${format(flight.time)} s · x = ${format(flight.range)} m · y = ${format(flight.apexHeight)} m",
      "v = ${format(flight.impactSpeed)} m/s · θ = ${format(flight.impactAngle)}°",
    )
  }
}

private fun formatSmall(value: Double) = value.rounded(SMALL_DECIMALS)

@Composable
internal fun PlanetChips(gravity: String, onChoose: (Planet) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PLANETS.forEach { planet ->
      FilterChip(
        selected = gravity.toNumberOrNull() == planet.gravity.toDouble(),
        onClick = { onChoose(planet) },
        label = { Text(stringResource(planet.name)) },
      )
    }
  }
}

/** Every step of the calculation with the numbers of this flight. */
@Composable
private fun Explanation(flight: Trajectory, format: (Double) -> String) {
  val launch = flight.launch
  val vx = format(flight.horizontalSpeed(0.0))
  val vy = format(flight.verticalSpeed(0.0))
  val g = format(launch.gravity)
  val h = format(launch.height)
  val v = format(launch.speed)
  val angle = format(launch.angle)
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.projectile_explain_split,
      Res.string.projectile_explain_split_text,
      "vx = v₀ · cos θ = $v · cos $angle° = $vx m/s",
      "vy = v₀ · sin θ = $v · sin $angle° = $vy m/s",
    )
    Step(
      Res.string.projectile_explain_motion,
      Res.string.projectile_explain_motion_text,
      "x(t) = vx · t = $vx · t",
      "y(t) = h₀ + vy · t − ½ · g · t² = $h + $vy · t − ½ · $g · t²",
    )
    Step(
      Res.string.projectile_explain_time,
      Res.string.projectile_explain_time_text,
      "t = (vy + √(vy² + 2 · g · h₀)) / g",
      "t = ($vy + √($vy² + 2 · $g · $h)) / $g = ${format(flight.time)} s",
    )
    Step(
      Res.string.projectile_explain_range,
      Res.string.projectile_explain_range_text,
      "x = vx · t = $vx · ${format(flight.time)} = ${format(flight.range)} m",
    )
    Step(
      Res.string.projectile_explain_apex,
      Res.string.projectile_explain_apex_text,
      "t = vy / g = $vy / $g = ${format(flight.apexTime)} s",
      "y = ${format(flight.y(flight.apexTime))} m",
    )
    Step(
      Res.string.projectile_explain_impact,
      Res.string.projectile_explain_impact_text,
      "v = √(vx² + vy²) = ${format(flight.impactSpeed)} m/s",
      "θ = ${format(flight.impactAngle)}°",
    )
  }
}

@Composable
internal fun Step(title: StringResource, text: StringResource, vararg formulas: String) {
  Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
    Text(
      stringResource(text),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    formulas.forEach { formula ->
      Text(
        formula,
        modifier =
          Modifier.fillMaxWidth().clip(TILE_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(8.dp),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

private const val DECIMALS = 2
private const val SMALL_DECIMALS = 5
private const val CENTIMETERS = 100
private const val PERCENT = 100
private const val HALF = 0.5
private const val MILLIS_PER_SECOND = 1000
private const val MIN_ANIMATION = 1500
private const val MAX_ANIMATION = 6000
private const val LANDING_MILLIS = 450
private val SCENE_HEIGHT = 260.dp
private val WIDE_LAYOUT_WIDTH = 720.dp
private val FIELD_WIDTH = 132.dp
private val SYMBOL_WIDTH = 36.dp
private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val TILE_SHAPE = RoundedCornerShape(12.dp)
