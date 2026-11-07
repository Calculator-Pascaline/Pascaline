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

package com.sadellie.unitto.feature.calculator.physics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.physics_invalid

/** Seconds since this was first shown. Read it while drawing, so that only the drawing is redone. */
@Composable
internal fun rememberClock(): State<Double> {
  val seconds = remember { mutableDoubleStateOf(0.0) }
  LaunchedEffect(Unit) {
    val start = withFrameNanos { it }
    while (true) withFrameNanos { seconds.doubleValue = (it - start) / NANOS_PER_SECOND }
  }
  return seconds
}

/** Calls [onFrame] with the seconds since the last frame, for as long as [key] stays. */
@Composable
internal fun OnEveryFrame(key: Any, onFrame: (Double) -> Unit) {
  LaunchedEffect(key) {
    var last = withFrameNanos { it }
    while (true) {
      withFrameNanos { now ->
        onFrame((now - last) / NANOS_PER_SECOND)
        last = now
      }
    }
  }
}

/** The decimal separator the user chose. */
@Composable
internal fun rememberFractional(): String {
  val preferences = koinInject<UserPreferencesRepository>()
  val fractional by
    remember(preferences) { preferences.calculatorPrefs.map { it.formatterSymbols.fractional } }
      .collectAsState(initial = ".")
  return fractional
}

internal fun numberFormat(fractional: String): (Double) -> String = { value -> value.rounded(DECIMALS).replace(".", fractional) }

internal fun String.number() = toNumberOrNull() ?: Double.NaN

/** What moves on top of what can be changed, side by side with how it works on wide screens. */
@Composable
internal fun PhysicsScreen(
  title: StringResource,
  openDrawer: () -> Unit,
  live: @Composable () -> Unit,
  settings: @Composable () -> Unit,
  explanation: @Composable () -> Unit,
) {
  ScaffoldWithTopBar(
    title = { Text(stringResource(title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val spacing = Arrangement.spacedBy(12.dp)
      if (maxWidth >= WIDE_LAYOUT_WIDTH) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = spacing) {
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = spacing) {
            live()
            settings()
          }
          Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = spacing) {
            explanation()
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
          verticalArrangement = spacing,
        ) {
          live()
          settings()
          explanation()
        }
      }
    }
  }
}

/** Card without a title, for what moves. */
@Composable
internal fun LiveCard(content: @Composable () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(LIVE_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    content()
  }
}

@Composable
internal fun InvalidValues() {
  Text(stringResource(Res.string.physics_invalid), color = MaterialTheme.colorScheme.error)
}

private const val NANOS_PER_SECOND = 1e9
private const val DECIMALS = 2
private val LIVE_SHAPE = RoundedCornerShape(24.dp)
private val WIDE_LAYOUT_WIDTH = 720.dp
