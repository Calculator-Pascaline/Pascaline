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

package com.sadellie.unitto.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window

/**
 * The browser's back button. Each enabled handler adds a step to the browser history, going back
 * takes it and calls the handler added last, like Android calls the innermost one.
 */
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
  val currentOnBack by rememberUpdatedState(onBack)
  DisposableEffect(enabled) {
    if (!enabled) return@DisposableEffect onDispose {}
    val handler = BrowserBack.Handler { currentOnBack() }
    BrowserBack.add(handler)
    onDispose { BrowserBack.remove(handler) }
  }
}

private object BrowserBack {
  class Handler(val onBack: () -> Unit)

  private val handlers = mutableListOf<Handler>()
  // History steps taken by the app itself, for handlers that went away without the back button
  private var ignoredSteps = 0

  init {
    window.addEventListener("popstate", { onPopState() })
  }

  fun add(handler: Handler) {
    handlers += handler
    window.history.pushState(null, "")
  }

  fun remove(handler: Handler) {
    if (!handlers.remove(handler)) return
    // Closed another way (a button, a tap): its step in the history goes too
    ignoredSteps++
    window.history.back()
  }

  private fun onPopState() {
    if (ignoredSteps > 0) {
      ignoredSteps--
      return
    }
    val handler = handlers.removeLastOrNull() ?: return
    handler.onBack()
  }
}
