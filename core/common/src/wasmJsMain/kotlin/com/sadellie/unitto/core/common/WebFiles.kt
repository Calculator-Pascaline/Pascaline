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

/** Files of the web app: the browser saves them in its downloads and opens them from a picker. */
object WebFiles {
  /** Saves [text] as [fileName] in the browser's downloads. */
  fun save(fileName: String, mimeType: String, text: String) = saveFile(fileName, mimeType, text)

  /**
   * Asks for a file of type [accept] (like ".json") and gives its text to [onText]. Nothing happens
   * when the picker is closed.
   */
  fun open(accept: String, onText: (String) -> Unit) = openFile(accept, onText)
}

@JsFun(
  """(name, type, text) => {
  const url = URL.createObjectURL(new Blob([text], { type: type }));
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}"""
)
private external fun saveFile(name: String, type: String, text: String)

@JsFun(
  """(accept, onText) => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = accept;
  input.onchange = () => {
    const file = input.files && input.files[0];
    if (file) file.text().then(text => onText(text));
  };
  input.click();
}"""
)
private external fun openFile(accept: String, onText: (String) -> Unit)
