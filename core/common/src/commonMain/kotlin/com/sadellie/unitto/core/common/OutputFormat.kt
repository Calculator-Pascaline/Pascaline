/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025 Elshan Agaev
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

/** Output format here means whether or not use engineering notation */
object OutputFormat {
  // Never use engineering notation
  const val PLAIN = 0

  // Plain numbers, E notation when the number is too big or too small to read: 1.2E+12, 5E-9.
  // The app always uses this one
  const val ALLOW_ENGINEERING = 1

  // App will try it's best to use engineering notation
  const val FORCE_ENGINEERING = 2
}
