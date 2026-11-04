/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, R> combineBig(
  flow: Flow<T1>,
  flow2: Flow<T2>,
  flow3: Flow<T3>,
  flow4: Flow<T4>,
  flow5: Flow<T5>,
  flow6: Flow<T6>,
  flow7: Flow<T7>,
  transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> =
  combine(flow, flow2, flow3, flow4, flow5, flow6, flow7) { args ->
    // In the order of the flows
    val values = args.iterator()
    transform(
      values.next() as T1,
      values.next() as T2,
      values.next() as T3,
      values.next() as T4,
      values.next() as T5,
      values.next() as T6,
      values.next() as T7,
    )
  }

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combineBig(
  flow: Flow<T1>,
  flow2: Flow<T2>,
  flow3: Flow<T3>,
  flow4: Flow<T4>,
  flow5: Flow<T5>,
  flow6: Flow<T6>,
  flow7: Flow<T7>,
  flow8: Flow<T8>,
  transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Flow<R> =
  combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { args ->
    // In the order of the flows
    val values = args.iterator()
    transform(
      values.next() as T1,
      values.next() as T2,
      values.next() as T3,
      values.next() as T4,
      values.next() as T5,
      values.next() as T6,
      values.next() as T7,
      values.next() as T8,
    )
  }

fun <T> Flow<T>.stateIn(scope: CoroutineScope, initialValue: T): StateFlow<T> =
  stateIn(scope, SharingStarted.WhileSubscribed(DEFAULT_STATE_IN_STOP_TIME_MS), initialValue)

@Composable
expect fun <T> StateFlow<T>.collectAsStateWithLifecycleKMP(
  initialValue: T = this.value,
  minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
  context: CoroutineContext = EmptyCoroutineContext,
): State<T>

expect val defaultIODispatcher: CoroutineDispatcher

private const val DEFAULT_STATE_IN_STOP_TIME_MS = 5_000L
