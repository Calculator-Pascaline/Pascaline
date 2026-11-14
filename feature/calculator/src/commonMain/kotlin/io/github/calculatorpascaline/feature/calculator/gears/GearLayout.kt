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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** What is drawn at a [Placement]. */
internal enum class Shape {
  /** A gear with its teeth outside. */
  GEAR,

  /** A ring with its teeth inside. */
  RING,

  /** A worm seen from its end, on the shaft of the gear before it. */
  WORM,

  /** The wheel of a worm seen from its edge, since it turns on an axis that lies in the screen. */
  WHEEL,

  /** A toothed bar along the bottom of the gear before it. */
  RACK,
}

/**
 * Where something is drawn, for a module of 1: [x] and [y] of its center (y goes down), its pitch [radius] and the
 * [angle] of its first tooth in radians, clockwise from the right. [state] is the gear of the train it shows, [level]
 * how far in front it is. [beltFrom] is the placement a belt comes from. A [Shape.WHEEL] lies along [direction]; a
 * [Shape.RACK] has its pitch line at [y], its middle at [x], and [angle] is where a gap between its teeth is, along x.
 */
internal data class Placement(
  val x: Double,
  val y: Double,
  val radius: Double,
  val teeth: Int,
  val angle: Double,
  val shape: Shape = Shape.GEAR,
  val state: Int = 0,
  val level: Int = 0,
  val direction: Double = 0.0,
  val beltFrom: Int? = null,
)

/**
 * The gears of [states] side by side, up and down in turn so that they take less room, with the first one turned by
 * [turn] radians. A gear that meshes has a gap where the tooth of the other one goes in, and turns the other way at
 * the speed its teeth give; one on the same shaft is drawn on top of the gear before it. Stops at the first gear
 * without a valid number of teeth, and after a worm or a rack, after which gears turn on another axis or nothing turns.
 */
internal fun layoutGears(states: List<GearState>, turn: Double): List<Placement> {
  val first = states.firstOrNull()?.takeIf { !it.teeth.isNaN() } ?: return emptyList()
  val placements = mutableListOf(Placement(0.0, 0.0, first.teeth / 2, first.teeth.toInt(), turn))
  var meshes = 0
  var ended = false
  for ((index, state) in states.withIndex().drop(1)) {
    if (ended || (state.link != Link.RACK && state.teeth.isNaN())) break
    val before = placements.last()
    val preferred = if (meshes % 2 == 0) -ZIGZAG else ZIGZAG
    placements += placed(state.link, placements, before, state.teeth / 2, preferred).map {
      it.copy(state = index, level = state.level)
    }
    if (state.link != Link.SHAFT) meshes++
    ended = state.link == Link.WORM || state.link == Link.RACK
  }
  return placements
}

/** What a gear of [radius] turned by [before] through [link] draws, [preferred] being the way it would rather go. */
private fun placed(
  link: Link?,
  placements: List<Placement>,
  before: Placement,
  radius: Double,
  preferred: Double,
): List<Placement> =
  when (link) {
    Link.SHAFT -> listOf(before.copy(radius = radius, teeth = (radius * 2).toInt(), shape = Shape.GEAR, beltFrom = null))
    Link.MESH -> listOf(meshed(placements, before, radius, preferred))
    Link.INTERNAL -> listOf(ring(before, radius, preferred))
    Link.BELT -> listOf(belted(placements, before, radius, preferred))
    Link.WORM -> worm(placements, before, radius, preferred)
    Link.RACK -> listOf(rack(before))
    null -> emptyList()
  }

/** Teeth on teeth: it touches [before] and turns the other way, with a tooth of [before] in one of its gaps. */
private fun meshed(placements: List<Placement>, before: Placement, radius: Double, preferred: Double): Placement {
  val teeth = (radius * 2).toInt()
  val direction = clearestDirection(placements, before, radius, preferred)
  val angle = direction + PI - PI / teeth - (before.angle - direction) * before.teeth / teeth
  val distance = before.radius + radius
  return Placement(before.x + distance * cos(direction), before.y + distance * sin(direction), radius, teeth, angle)
}

/**
 * A ring around [before], touching it on the inside. Both turn the same way: where they touch, [before] has a tooth and
 * the ring a gap.
 */
private fun ring(before: Placement, radius: Double, preferred: Double): Placement {
  val teeth = (radius * 2).toInt()
  val distance = radius - before.radius
  // Seen from the ring's center, they touch the other way from where the ring goes
  val touch = preferred + PI
  val angle = touch + PI / teeth + (before.angle - touch) * before.teeth / teeth
  return Placement(
    before.x + distance * cos(preferred),
    before.y + distance * sin(preferred),
    radius,
    teeth,
    angle,
    shape = Shape.RING,
  )
}

/** A pulley or sprocket a belt's width away from [before], turning the same way at the speed its teeth give. */
private fun belted(placements: List<Placement>, before: Placement, radius: Double, preferred: Double): Placement {
  val teeth = (radius * 2).toInt()
  val direction = clearestDirection(placements, before, radius + BELT_GAP / 2, preferred)
  val distance = before.radius + radius + BELT_GAP
  return Placement(
    before.x + distance * cos(direction),
    before.y + distance * sin(direction),
    radius,
    teeth,
    before.angle * before.teeth / teeth,
    beltFrom = placements.lastIndex,
  )
}

/** The worm on the shaft of [before], drawn on top of it, and its wheel beside it, seen from its edge. */
private fun worm(placements: List<Placement>, before: Placement, radius: Double, preferred: Double): List<Placement> {
  val worm = before.copy(radius = WORM_RADIUS, teeth = 1, shape = Shape.WORM, beltFrom = null)
  val direction = clearestDirection(placements, worm, radius, preferred)
  val distance = WORM_RADIUS + radius
  val wheel =
    Placement(
      before.x + distance * cos(direction),
      before.y + distance * sin(direction),
      radius,
      (radius * 2).toInt(),
      0.0,
      shape = Shape.WHEEL,
      direction = direction,
    )
  return listOf(worm, wheel)
}

/** A rack under [before]: it moves along as far as the pitch circle rolls, with a gap under a tooth of [before]. */
private fun rack(before: Placement): Placement {
  // Turning clockwise, the bottom of the gear goes left
  val gap = before.x - before.radius * (before.angle - PI / 2)
  return Placement(before.x, before.y + before.radius, before.radius * RACK_LENGTH, 0, gap, shape = Shape.RACK)
}

/**
 * Which way from [before] a gear of [radius] goes: [preferred], unless it would cover a gear drawn earlier. Then the
 * direction to the right that keeps it the furthest from them.
 */
private fun clearestDirection(placements: List<Placement>, before: Placement, radius: Double, preferred: Double): Double {
  val others = placements.filter { it.x != before.x || it.y != before.y }
  fun clearance(direction: Double): Double {
    val distance = before.radius + radius
    val x = before.x + distance * cos(direction)
    val y = before.y + distance * sin(direction)
    return others.minOfOrNull { hypot(it.x - x, it.y - y) - it.radius - radius } ?: Double.MAX_VALUE
  }
  if (clearance(preferred) >= 0) return preferred
  return (-DIRECTIONS..DIRECTIONS).map { it * PI / 2 / DIRECTIONS }.maxBy { clearance(it) }
}

/** Where the drawing ends on each side, with room for the teeth. */
internal fun Placement.extent(): List<Double> =
  when (shape) {
    Shape.RACK -> listOf(x - radius, x + radius, y - ADDENDUM, y + RACK_DEPTH)
    Shape.WHEEL -> {
      val ends = listOf(-radius, radius).map { x + it * cos(direction) to y + it * sin(direction) }
      listOf(ends.minOf { it.first } - FACE, ends.maxOf { it.first } + FACE, ends.minOf { it.second } - FACE, ends.maxOf { it.second } + FACE)
    }
    Shape.RING -> listOf(x - radius - RIM, x + radius + RIM, y - radius - RIM, y + radius + RIM)
    else -> listOf(x - radius - ADDENDUM, x + radius + ADDENDUM, y - radius - ADDENDUM, y + radius + ADDENDUM)
  }

// Tooth sizes for a module of 1: the tip is 1 above the pitch circle and the root 1.25 below
internal const val ADDENDUM = 1.0
internal const val DEDENDUM = 1.25
/** A worm of about 10 modules across, a common size. */
internal const val WORM_RADIUS = 5.0
/** How thick a worm wheel looks from its edge. */
internal const val FACE = 4.0
/** How far the outside of a ring goes past its teeth. */
internal const val RIM = 3.0
internal const val RACK_DEPTH = 4.0
private const val RACK_LENGTH = 1.6
private const val BELT_GAP = 12.0
private const val DIRECTIONS = 6
private const val ZIGZAG = PI / 9
