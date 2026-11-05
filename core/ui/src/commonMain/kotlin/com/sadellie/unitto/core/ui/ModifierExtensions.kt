/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.core.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.sadellie.unitto.core.designsystem.LocalHapticFeedbackManager
import kotlinx.coroutines.withTimeoutOrNull

/**
 * @param isPressed Pressed state from [trackKeyPress], it comes without the delay of clicks in
 *   scrollable containers. Pressed state of [interactionSource] when null.
 */
fun Modifier.squashable(
  onClick: () -> Unit = {},
  onLongClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource,
  cornerRadiusRange: IntRange,
  role: Role = Role.Button,
  animationSpec: AnimationSpec<Int> = tween(),
  isPressed: Boolean? = null,
) = composed {
  val isClickPressed by interactionSource.collectIsPressedAsState()
  val isKeyPressed = isPressed ?: isClickPressed
  val cornerRadius: Int by
    animateIntAsState(
      targetValue = if (isKeyPressed && LocalKeyAnimation.current) cornerRadiusRange.first else cornerRadiusRange.last,
      animationSpec = animationSpec,
      label = "Squashed animation",
    )

  this.clip(RoundedCornerShape(cornerRadius))
    .haptics(isKeyPressed)
    .combinedClickable(
      onClick = onClick,
      onLongClick = onLongClick,
      interactionSource = interactionSource,
      indication = ripple(),
      role = role,
      enabled = enabled,
    )
}

fun Modifier.squashable(
  onClick: () -> Unit = {},
  onLongClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource,
  cornerRadiusRange: ClosedRange<Dp>,
  role: Role = Role.Button,
  animationSpec: AnimationSpec<Dp> = tween(),
) = composed {
  val isPressed by interactionSource.collectIsPressedAsState()
  val cornerRadius: Dp by
    animateDpAsState(
      targetValue = if (isPressed) cornerRadiusRange.start else cornerRadiusRange.endInclusive,
      animationSpec = animationSpec,
      label = "Squashed animation",
    )

  this.clip(RoundedCornerShape(cornerRadius))
    .haptics(isPressed)
    .combinedClickable(
      onClick = onClick,
      onLongClick = onLongClick,
      interactionSource = interactionSource,
      indication = ripple(),
      role = role,
      enabled = enabled,
    )
}

/**
 * Vibration for keys that can be swiped over, on pages or in lists: only for real presses, see
 * [KeyPress.presses]. [keyPress] is updated by [trackKeyPress].
 */
fun Modifier.keyHaptics(keyPress: KeyPress) = composed {
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedbackManager.current
  LaunchedEffect(keyPress.presses) { if (keyPress.presses > 0) haptics.vibrateKeyboardPress(scope) }
  this
}

/** Whether a finger is on a key, see [trackKeyPress]. */
@Stable
class KeyPress {
  var isPressed by mutableStateOf(false)
    internal set

  /**
   * Touches that are not swipes. A number, as a quick tap can be over before the next frame, which
   * then would never see it [isPressed].
   */
  var presses by mutableIntStateOf(0)
    internal set
}

@Composable fun rememberKeyPress(): KeyPress = remember { KeyPress() }

/**
 * Key is pressed as soon as a finger touches it. Clicks in scrollable containers (like pagers) are
 * pressed only after a delay, in case it is a swipe: quick taps were too short for key animations.
 * Touches are not consumed, clicks and swipes work as before. A swipe releases the key.
 *
 * It is a real press when the finger lifts, or stays still for a moment: a swipe moves right away.
 * The last pass sees what clicks and swipes did with each touch.
 */
fun Modifier.trackKeyPress(keyPress: KeyPress) =
  pointerInput(keyPress) {
    awaitEachGesture {
      awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
      keyPress.isPressed = true
      var isLifted: Boolean? = null
      withTimeoutOrNull(SWIPE_WAIT_MILLIS) { isLifted = awaitFingerLifted() }
      if (isLifted != false) keyPress.presses++
      if (isLifted == null) awaitFingerLifted()
      keyPress.isPressed = false
    }
  }

/** True when the finger lifts from the key, false when a swipe takes it or it leaves the key. */
private suspend fun AwaitPointerEventScope.awaitFingerLifted(): Boolean {
  while (true) {
    val change = awaitPointerEvent(PointerEventPass.Final).changes.first()
    if (change.changedToUpIgnoreConsumed()) return true
    if (change.isConsumed || change.isOutOfBounds(size, extendedTouchPadding)) return false
  }
}

private const val SWIPE_WAIT_MILLIS = 100L

internal expect fun Modifier.haptics(isPressed: Boolean): Modifier
