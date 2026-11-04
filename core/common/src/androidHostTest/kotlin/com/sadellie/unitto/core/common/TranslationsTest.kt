/*
 * Unitto is a calculator for Android
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

package com.sadellie.unitto.core.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.w3c.dom.Element

/**
 * Checks every translation against English. A missing or extra placeholder makes the string
 * format wrong or crash, and a name that English doesn't have is never shown.
 */
class TranslationsTest {
  private val resources = File("src/commonMain/composeResources")
  private val placeholder = Regex("""%\d+\$[sd]|%[sd]""")

  private fun strings(folder: File): Map<String, String> {
    val document =
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(folder, "strings.xml"))
    val nodes = document.getElementsByTagName("string")
    return (0 until nodes.length)
      .map { nodes.item(it) as Element }
      .associate { it.getAttribute("name") to it.textContent }
  }

  private val english = strings(File(resources, "values"))
  private val translations =
    resources
      .listFiles { file -> file.name.startsWith("values-") && File(file, "strings.xml").exists() }!!
      .sortedBy { it.name }
      .associate { it.name to strings(it) }

  @Test
  fun translationsFound() {
    assertTrue(translations.size >= 10, "found ${translations.keys}")
  }

  @Test
  fun sameNamesAsEnglish() {
    val unknown =
      translations.flatMap { (folder, strings) -> (strings.keys - english.keys).map { "$folder/$it" } }
    assertEquals(emptyList(), unknown)
  }

  @Test
  fun samePlaceholdersAsEnglish() {
    val wrong =
      translations.flatMap { (folder, strings) ->
        strings.mapNotNull { (name, text) ->
          val expected = english[name] ?: return@mapNotNull null
          val want = placeholder.findAll(expected).map { it.value }.sorted().toList()
          val got = placeholder.findAll(text).map { it.value }.sorted().toList()
          if (want == got) null else "$folder/$name: $got instead of $want"
        }
      }
    assertEquals(emptyList(), wrong)
  }
}
