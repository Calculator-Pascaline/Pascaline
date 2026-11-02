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

package com.sadellie.unitto.core.common

/**
 * Text saved in the browser (localStorage), kept after the page is closed. Without storage (private
 * windows of some browsers) nothing is saved and nothing breaks.
 */
object WebStorage {
  fun get(key: String): String? = storageGet(PREFIX + key)

  fun set(key: String, value: String) = storageSet(PREFIX + key, value)

  /** Fields saved as one text: each field is its length, a colon and the field. */
  fun encode(fields: List<String>): String = fields.joinToString("") { "${it.length}:$it" }

  /** Fields of [encode], empty list for broken text. */
  fun decode(text: String): List<String> {
    val fields = mutableListOf<String>()
    var index = 0
    while (index < text.length) {
      val colon = text.indexOf(':', index)
      if (colon == -1) return emptyList()
      val length = text.substring(index, colon).toIntOrNull() ?: return emptyList()
      val end = colon + 1 + length
      if (end > text.length) return emptyList()
      fields += text.substring(colon + 1, end)
      index = end
    }
    return fields
  }

  private const val PREFIX = "pascaline."
}

@JsFun("(key) => { try { return localStorage.getItem(key); } catch (e) { return null; } }")
private external fun storageGet(key: String): String?

@JsFun("(key, value) => { try { localStorage.setItem(key, value); } catch (e) {} }")
private external fun storageSet(key: String, value: String)
