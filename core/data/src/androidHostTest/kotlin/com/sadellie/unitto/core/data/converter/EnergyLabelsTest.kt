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

package com.sadellie.unitto.core.data.converter

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Labels of the gigajoule (10⁹ J) and the megajoule (10⁶ J) used to be swapped in most languages:
 * 1 "GJ" was 1 000 000 J.
 */
class EnergyLabelsTest {
  private val resources = File("../common/src/commonMain/composeResources")

  private fun String.string(name: String): String? =
    Regex("""<string name="$name">([^<]*)</string>""").find(this)?.groupValues?.get(1)

  @Test
  fun gigajouleAndMegajoule() {
    val languages = resources.listFiles { file -> file.name.startsWith("values") }.orEmpty()
    assertTrue(languages.isNotEmpty(), "No strings in ${resources.absolutePath}")
    languages.forEach { language ->
      val strings = File(language, "strings.xml").readText()
      val gigaShort = strings.string("unit_gigajoule_short") ?: return@forEach
      val megaShort = strings.string("unit_megajoule_short") ?: return@forEach
      assertTrue(gigaShort.first() in "GГ", "${language.name}: gigajoule is $gigaShort")
      assertTrue(megaShort.first() in "MМ", "${language.name}: megajoule is $megaShort")
      val giga = strings.string("unit_gigajoule").orEmpty().lowercase()
      assertTrue(MEGA.none { giga.startsWith(it) }, "${language.name}: gigajoule is $giga")
    }
  }

  private companion object {
    val MEGA = listOf("meg", "még", "мег")
  }
}
