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

  /** Everything the web app saved, as JSON: the web app's backup file. */
  fun exportAll(): String = storageExport(PREFIX)

  /** Replaces everything with [exportAll]'s JSON, false (and nothing changed) for another file. */
  fun importAll(json: String): Boolean = storageImport(PREFIX, json)

  private const val PREFIX = "pascaline."
}

@JsFun("(key) => { try { return localStorage.getItem(key); } catch (e) { return null; } }")
private external fun storageGet(key: String): String?

@JsFun("(key, value) => { try { localStorage.setItem(key, value); } catch (e) {} }")
private external fun storageSet(key: String, value: String)

@JsFun(
  """(prefix) => {
  const data = {};
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key.startsWith(prefix)) data[key.substring(prefix.length)] = localStorage.getItem(key);
    }
  } catch (e) {}
  return JSON.stringify({ app: 'pascaline-web', version: 1, data: data });
}"""
)
private external fun storageExport(prefix: String): String

@JsFun(
  """(prefix, json) => {
  try {
    const backup = JSON.parse(json);
    if (!backup || backup.app !== 'pascaline-web' || typeof backup.data !== 'object') return false;
    for (const key of Object.keys(localStorage)) if (key.startsWith(prefix)) localStorage.removeItem(key);
    for (const [key, value] of Object.entries(backup.data)) {
      if (typeof value === 'string') localStorage.setItem(prefix + key, value);
    }
    return true;
  } catch (e) { return false; }
}"""
)
private external fun storageImport(prefix: String, json: String): Boolean
