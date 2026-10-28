/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

package com.sadellie.unitto

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.uri.toKmpUri
import com.sadellie.unitto.core.datastore.PrefKeys
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.designsystem.HapticFeedbackManagerImpl
import com.sadellie.unitto.core.designsystem.LocalHapticFeedbackManager
import com.sadellie.unitto.core.designsystem.LocalWindowSize
import com.sadellie.unitto.core.designsystem.theme.LocalNumberTypography
import com.sadellie.unitto.core.designsystem.theme.numberTypographyUnitto
import com.sadellie.unitto.core.navigation.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class MainActivity : AppCompatActivity(), KoinComponent {

  @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val uri = intent.data
    val deepLinkRoute = uri?.let { Route.extractRouteFromDeeplink(uri.toKmpUri()) }
    val userPrefsRepository by inject<UserPreferencesRepository>()

    setContent {
      val prefs = userPrefsRepository.appPrefs.collectAsStateWithLifecycle(null).value

      LaunchedEffect(prefs?.enableKeepScreenOn) {
        val enableKeepScreenOn = prefs?.enableKeepScreenOn ?: return@LaunchedEffect
        if (enableKeepScreenOn) {
          this@MainActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          this@MainActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }
      // Woken up while the app was open, it shows again without unlocking. Leaving it still asks to
      val showOnLockScreen =
        remember { userPrefsRepository.appData(PrefKeys.SHOW_ON_LOCK_SCREEN_PREF_KEY) }
          .collectAsStateWithLifecycle(null)
          .value
      LaunchedEffect(showOnLockScreen) {
        if (showOnLockScreen != null) showWhenLocked(showOnLockScreen != "false")
      }
      val view = LocalView.current
      val hapticFeedbackManager =
        remember(prefs?.enableVibrations) {
          HapticFeedbackManagerImpl(view, prefs?.enableVibrations ?: false)
        }

      val numberTypography = numberTypographyUnitto()
      CompositionLocalProvider(
        LocalNumberTypography provides numberTypography,
        LocalWindowSize provides calculateWindowSizeClass(this@MainActivity),
        LocalHapticFeedbackManager provides hapticFeedbackManager,
      ) {
        App(deepLinkRoute, prefs)
      }
    }
  }

  private fun showWhenLocked(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(enabled)
    } else {
      @Suppress("DEPRECATION")
      if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
      else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }
  }
}
