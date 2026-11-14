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

package io.github.calculatorpascaline.core.data.converter

import io.github.calculatorpascaline.core.common.KBigDecimal
import io.github.calculatorpascaline.core.database.UnitsDaoInMemory
import io.github.calculatorpascaline.core.model.converter.UnitGroup
import io.github.calculatorpascaline.core.model.converter.UnitsListSorting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Hours are typed as hours and minutes: 08:25 is 8 h and 25 min. */
class UnitRepositoryConvertHourMinute {
  private val unitConverterRepo = UnitConverterRepositoryImpl(UnitsRepository(UnitsDaoInMemory()))

  private suspend fun convert(to: String, hours: String, minutes: String, formatTime: Boolean = false) =
    unitConverterRepo.convert(
      unitFromId = UnitID.hour,
      unitToId = to,
      value1 = hours,
      value2 = minutes,
      formatTime = formatTime,
    )

  private fun assertValue(expected: String, result: ConverterResult) {
    val value = (result as ConverterResult.Default).value
    assertEquals("$expected != $value", 0, value.compareTo(KBigDecimal(expected)))
  }

  @Test
  fun hoursAndMinutesToMinutes() = runTest {
    assertValue("505", convert(UnitID.minute, "8", "25"))
    assertValue("30360", convert(UnitID.second, "8", "26"))
  }

  @Test
  fun minutesAlone() = runTest {
    assertValue("90", convert(UnitID.minute, "1", "30"))
    assertValue("45", convert(UnitID.minute, "0", "45"))
    assertValue("120", convert(UnitID.minute, "2", "0"))
  }

  @Test
  fun hoursAndMinutesToHours() = runTest {
    assertValue("1.5", convert(UnitID.hour, "1", "30"))
  }

  @Test
  fun hoursAndMinutesFormatted() = runTest {
    val result = convert(UnitID.second, "8", "25", formatTime = true) as ConverterResult.Time
    assertEquals(0, result.hour.compareTo(KBigDecimal("8")))
    assertEquals(0, result.minute.compareTo(KBigDecimal("25")))
  }

  @Test
  fun listOfUnitsUsesMinutesToo() = runTest {
    val units =
      unitConverterRepo
        .filterUnitsAndBatchConvert(
          query = "",
          unitGroup = UnitGroup.TIME,
          favoritesOnly = false,
          sorting = UnitsListSorting.USAGE,
          unitFromId = UnitID.hour,
          input1 = "8",
          input2 = "25",
        )
        .getValue(UnitGroup.TIME)

    assertValue("505", units.first { it.basicUnit.id == UnitID.minute }.conversion!!)
  }
}
