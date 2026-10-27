/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

object Config {
  const val STORE_LINK = "https://play.google.com/store/apps/details?id=com.sadellie.unitto"
  const val TRANSLATE_LINK = "https://github.com/sadellie/unitto"
  const val WEB_LINK = "https://sadellie.github.io/unitto/app"
  const val TERMS_LINK = "https://sadellie.github.io/unitto/terms"
  const val PRIVACY_LINK = "https://sadellie.github.io/unitto/privacy"
  const val SOURCE_CODE_LINK = "https://github.com/Calculator-Pascaline/Pascaline"
  const val AUTHOR_LINK = "https://sadellie.github.io"
  val VERSION_CODE
    get() = BuildKonfig.VERSION_CODE

  val VERSION_NAME
    get() = BuildKonfig.VERSION_NAME
}
