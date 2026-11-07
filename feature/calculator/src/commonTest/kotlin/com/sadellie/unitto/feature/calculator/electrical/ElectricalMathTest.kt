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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElectricalMathTest {
  private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-6) =
    assertTrue(abs(expected - actual) <= tolerance * maxOf(1.0, abs(expected)), "expected $expected, got $actual")

  private val plain: (Double) -> String = { value -> ((value * 100).toLong() / 100.0).toString().removeSuffix(".0") }

  @Test
  fun engineeringPicksThePrefix() {
    assertEquals("4.7 kΩ", engineering(4700.0, "Ω", plain))
    assertEquals("22 µF", engineering(22e-6, "F", plain))
    assertEquals("0 A", engineering(0.0, "A", plain))
    assertEquals("—", engineering(Double.NaN, "A", plain))
  }

  @Test
  fun rcFilterIsThreeDecibelsDownAtTheCorner() {
    val low = Filter(FilterKind.LOW_PASS, FilterBuild.RC, 1000.0, 1e-7, 0.0)
    assertClose(1591.549, low.cornerFrequency, 1e-5)
    assertClose(-3.0103, low.decibels(low.cornerFrequency), 1e-4)
    assertClose(-45.0, low.phase(low.cornerFrequency))
    assertClose(1.0, low.gain(1e-3), 1e-5)
    val high = Filter(FilterKind.HIGH_PASS, FilterBuild.RC, 1000.0, 1e-7, 0.0)
    assertClose(45.0, high.phase(high.cornerFrequency))
    assertTrue(high.gain(1.0) < 0.01)
  }

  @Test
  fun rlcFiltersMeetAtTheMiddle() {
    val pass = Filter(FilterKind.BAND_PASS, FilterBuild.RLC, 100.0, 1e-6, 1e-2)
    val stop = Filter(FilterKind.BAND_STOP, FilterBuild.RLC, 100.0, 1e-6, 1e-2)
    assertClose(1.0, pass.gain(pass.cornerFrequency))
    assertClose(0.0, stop.gain(stop.cornerFrequency))
    assertClose(1.0, pass.quality ?: 0.0)
    assertClose(pass.cornerFrequency, pass.bandwidth)
  }

  @Test
  fun rlFilterTurnsAtROverL() {
    val low = Filter(FilterKind.LOW_PASS, FilterBuild.RL, 100.0, 0.0, 0.01)
    assertClose(1591.549, low.cornerFrequency, 1e-5)
    assertClose(-3.0103, low.decibels(low.cornerFrequency), 1e-4)
    val high = Filter(FilterKind.HIGH_PASS, FilterBuild.RL, 100.0, 0.0, 0.01)
    assertClose(45.0, high.phase(high.cornerFrequency))
  }

  @Test
  fun secondOrderCutsTwiceAsFast() {
    val rlc = Filter(FilterKind.LOW_PASS, FilterBuild.RLC, 100.0, 1e-6, 1e-2)
    // 40 dB less ten times higher, and Q times more at f₀
    assertClose(-40.0, rlc.decibels(rlc.cornerFrequency * 10), 0.01)
    assertClose(rlc.quality ?: 0.0, rlc.gain(rlc.cornerFrequency))
    assertClose(-90.0, rlc.phase(rlc.cornerFrequency))
    val first = Filter(FilterKind.LOW_PASS, FilterBuild.RC, 1000.0, 1e-7, 0.0)
    assertClose(-20.0, first.decibels(first.cornerFrequency * 100) - first.decibels(first.cornerFrequency * 10), 0.01)
  }

  @Test
  fun twoRcStagesLoadEachOther() {
    val low = Filter(FilterKind.LOW_PASS, FilterBuild.TWO_RC, 1000.0, 1e-7, 0.0)
    assertClose(1.0 / 3, low.gain(low.cornerFrequency))
    val high = Filter(FilterKind.HIGH_PASS, FilterBuild.TWO_RC, 1000.0, 1e-7, 0.0)
    assertClose(1.0, high.gain(high.cornerFrequency * 1000), 1e-5)
    assertTrue(high.gain(high.cornerFrequency / 10) < 0.011)
  }

  @Test
  fun bandFiltersOnlyComeAsRlc() {
    assertFalse(Filter(FilterKind.BAND_PASS, FilterBuild.RC, 100.0, 1e-6, 0.0).isValid)
    assertFalse(Filter(FilterKind.LOW_PASS, FilterBuild.RL, 100.0, 1e-6, 0.0).isValid)
    assertTrue(Filter(FilterKind.LOW_PASS, FilterBuild.RL, 100.0, 0.0, 1e-3).isValid)
  }

  @Test
  fun colorCodeReadsBands() {
    val code = ColorCode(listOf(BandColor.YELLOW, BandColor.VIOLET), BandColor.RED, BandColor.GOLD)
    assertClose(4700.0, code.resistance)
    assertClose(4465.0, code.lowest)
    assertClose(4935.0, code.highest)
    val small = ColorCode(listOf(BandColor.BROWN, BandColor.BLACK, BandColor.BLACK), BandColor.SILVER, BandColor.BROWN)
    assertClose(1.0, small.resistance)
  }

  @Test
  fun colorCodeGoesBackAndForth() {
    listOf(4700.0, 10.0, 0.47, 1e6, 330.0).forEach { ohms ->
      val code = assertNotNull(colorCode(ohms, 2, BandColor.GOLD), "$ohms")
      assertClose(ohms, code.resistance)
    }
    assertClose(4700.0, assertNotNull(colorCode(4700.0, 3, BandColor.BROWN)).resistance)
    assertNull(colorCode(0.0, 2, BandColor.GOLD))
    assertNull(colorCode(0.001, 2, BandColor.GOLD))
  }

  @Test
  fun standardValuesAreFound() {
    assertClose(4700.0, ResistorSeries.E12.nearest(4500.0))
    assertClose(5100.0, ResistorSeries.E24.nearest(5000.0))
    assertClose(1.0, ResistorSeries.E12.nearest(0.99))
  }

  @Test
  fun capacitorCharges() {
    assertClose(0.0, chargedShare(0.0, 1.0))
    assertClose(0.632, chargedShare(1.0, 1.0), 1e-3)
    assertClose(0.993, chargedShare(5.0, 1.0), 1e-3)
  }

  @Test
  fun timerStaysBetweenItsLimits() {
    val timer = Timer555(1000.0, 10000.0, 1e-6)
    assertClose(0.5238, timer.duty, 1e-3)
    assertClose(1 / timer.period, timer.frequency)
    assertTrue(timer.isHigh(0.0))
    assertFalse(timer.isHigh(timer.highTime * 1.01))
    assertClose(1.0 / 3, timer.capacitorAt(0.0))
    assertClose(2.0 / 3, timer.capacitorAt(timer.highTime * 0.99999), 1e-4)
    assertClose(1.0 / 3, timer.capacitorAt(timer.period * 0.99999), 1e-4)
  }

  @Test
  fun gatesFollowTheirTables() {
    val inputs = listOf(false to false, false to true, true to false, true to true)
    fun table(gate: Gate) = inputs.map { (a, b) -> gate.output(a, b) }
    assertEquals(listOf(false, false, false, true), table(Gate.AND))
    assertEquals(listOf(false, true, true, true), table(Gate.OR))
    assertEquals(listOf(false, true, true, false), table(Gate.XOR))
    assertEquals(listOf(true, true, false, false), table(Gate.NOT))
    assertEquals(listOf(true, true, true, false), table(Gate.NAND))
    assertEquals(listOf(true, false, false, false), table(Gate.NOR))
    assertEquals(listOf(true, false, false, true), table(Gate.XNOR))
  }

  @Test
  fun voltageGainInDecibels() {
    assertClose(20.0, voltageDecibels(10.0))
    assertClose(-6.0206, voltageDecibels(0.5), 1e-4)
  }
}
