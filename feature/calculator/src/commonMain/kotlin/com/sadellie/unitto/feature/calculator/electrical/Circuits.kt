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

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

/**
 * The filter as it is built: the input on the left, one or two parts along the top and one or two
 * down to the ground, and the output on the right, taken across those.
 */
@Composable
internal fun FilterCircuit(filter: Filter, format: (Double) -> String) {
  val names =
    mapOf(
      PartKind.R to "R ${engineering(filter.resistance, "Ω", format)}",
      PartKind.C to "C ${engineering(filter.capacitance, "F", format)}",
      PartKind.L to "L ${engineering(filter.inductance, "H", format)}",
    )
  val stages = filter.stages()
  SchematicCanvas {
    val width = scope.size.width
    val height = scope.size.height
    val top = height * TOP
    val ground = height * GROUND
    val left = width * INPUT
    val right = width * OUTPUT
    wire(Offset(left, top), Offset(right, top))
    wire(Offset(left, ground), Offset(right, ground))
    val above = top - LABEL_RISE * scope.density
    var start = left
    stages.forEachIndexed { index, stage ->
      // The last stage ends a little before the output, the others share the rest
      val shunt = left + (width * SHUNT - left) * (index + 1) / stages.size
      wire(Offset(shunt, top), Offset(shunt, ground))
      stage.along.forEachIndexed { i, kind ->
        val x = start + (shunt - start) * (i + 1) / (stage.along.size + 1)
        part(kind, Offset(x, top), isVertical = false)
        label(names.getValue(kind), Offset(x, above))
      }
      stage.down.forEachIndexed { i, kind ->
        val y = top + (ground - top) * (i + 1) / (stage.down.size + 1)
        part(kind, Offset(shunt, y), isVertical = true)
        label(names.getValue(kind), Offset(shunt - LABEL_SIDE * scope.density, y))
      }
      joint(Offset(shunt, top))
      joint(Offset(shunt, ground))
      start = shunt
    }
    listOf(left, right).forEach { x ->
      val color = if (x == right) colors.accent else colors.wire
      terminal(Offset(x, top), color)
      terminal(Offset(x, ground), color)
    }
    label("in", Offset(left, above))
    label("out", Offset(right, above))
  }
}

/** Parts along the top wire, then parts down to the ground. */
private class Stage(val along: List<PartKind>, val down: List<PartKind>)

private fun Filter.stages(): List<Stage> {
  val isLow = kind == FilterKind.LOW_PASS
  return when {
    kind == FilterKind.BAND_PASS -> listOf(Stage(listOf(PartKind.L, PartKind.C), listOf(PartKind.R)))
    kind == FilterKind.BAND_STOP -> listOf(Stage(listOf(PartKind.R), listOf(PartKind.L, PartKind.C)))
    build == FilterBuild.RL -> listOf(if (isLow) Stage(listOf(PartKind.L), listOf(PartKind.R)) else Stage(listOf(PartKind.R), listOf(PartKind.L)))
    build == FilterBuild.RLC ->
      listOf(
        if (isLow) Stage(listOf(PartKind.R, PartKind.L), listOf(PartKind.C)) else Stage(listOf(PartKind.R, PartKind.C), listOf(PartKind.L))
      )
    else -> {
      val stage = if (isLow) Stage(listOf(PartKind.R), listOf(PartKind.C)) else Stage(listOf(PartKind.C), listOf(PartKind.R))
      List(build.order) { stage }
    }
  }
}

/**
 * A battery, a switch with two ways, a resistor and the capacitor or coil. Charging, the switch
 * joins the battery; discharging, it joins the wire that goes around it.
 */
@Composable
internal fun ChargeCircuit(isCoil: Boolean, isCharging: Boolean) {
  SchematicCanvas {
    val width = scope.size.width
    val height = scope.size.height
    val top = height * TOP
    val ground = height * GROUND
    val battery = width * BATTERY
    val pivot = Offset(width * PIVOT, top)
    val toBattery = Offset(width * CONTACT, top)
    val bypass = Offset(width * BYPASS, top + (ground - top) * BYPASS_DROP)
    val storage = width * STORAGE
    wire(Offset(battery, top), toBattery)
    wire(pivot, Offset(storage, top))
    wire(Offset(storage, top), Offset(storage, ground))
    wire(Offset(battery, top), Offset(battery, ground))
    wire(Offset(battery, ground), Offset(storage, ground))
    wire(bypass, Offset(bypass.x, ground))
    battery(Offset(battery, (top + ground) / 2), isVertical = true)
    val resistor = Offset((pivot.x + storage) / 2, top)
    resistor(resistor, isVertical = false)
    val middle = Offset(storage, (top + ground) / 2)
    if (isCoil) inductor(middle, isVertical = true) else capacitor(middle, isVertical = true)
    // The blade of the switch, in the color of the part it joins
    wire(pivot, if (isCharging) toBattery else bypass, colors.accent)
    joint(pivot, colors.accent)
    joint(toBattery)
    joint(bypass)
    joint(Offset(bypass.x, ground))
    val side = LABEL_SIDE * scope.density
    label("V", Offset(battery - side, (top + ground) / 2))
    label("R", Offset(resistor.x, top - LABEL_RISE * scope.density))
    label(if (isCoil) "L" else "C", Offset(storage - side, middle.y))
  }
}

private enum class PartKind {
  R,
  C,
  L,
}

private fun Schematic.part(kind: PartKind, center: Offset, isVertical: Boolean) =
  when (kind) {
    PartKind.R -> resistor(center, isVertical)
    PartKind.C -> capacitor(center, isVertical)
    PartKind.L -> inductor(center, isVertical)
  }

private const val TOP = 0.24f
private const val GROUND = 0.9f
private const val INPUT = 0.06f
private const val OUTPUT = 0.94f
private const val SHUNT = 0.74f
private const val LABEL_RISE = 18
private const val LABEL_SIDE = 40
private const val BATTERY = 0.14f
private const val CONTACT = 0.3f
private const val PIVOT = 0.42f
private const val BYPASS = 0.3f
private const val BYPASS_DROP = 0.45f
private const val STORAGE = 0.86f
