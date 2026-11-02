/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2026 Elshan Agaev
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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/**
 * Keypad made of [KeypadFlowScope.KeypadRow]s. Rows are equally tall, keys of a row share its width
 * by weight. A pressed key gets wider and pushes other keys of its row aside.
 *
 * @param iconHeight Default height factor of icons, see [KeyboardButtonToken].
 */
@Composable
fun KeypadFlow(
  modifier: Modifier,
  iconHeight: Float = 1f,
  content: @Composable KeypadFlowScope.() -> Unit,
) {
  SubcomposeLayout(modifier) { constraints ->
    val maxHeight = constraints.maxHeight
    val maxWidth = constraints.maxWidth

    val verticalSpacingPx =
      if (maxHeight == Constraints.Infinity) {
        0
      } else {
        (maxHeight * KEYPAD_SPACING_FRACTION).roundToInt()
      }
    val horizontalSpacingPx =
      if (maxWidth == Constraints.Infinity) {
        0
      } else {
        (maxWidth * KEYPAD_SPACING_FRACTION).roundToInt()
      }

    val measurables =
      subcompose(KeypadSlots.Rows) {
        val scope = remember(iconHeight) { KeypadFlowScopeImpl(iconHeight = iconHeight) }
        CompositionLocalProvider(LocalKeypadSpacingPx provides horizontalSpacingPx) {
          scope.content()
        }
      }

    val count = measurables.size
    val totalSpacing = verticalSpacingPx * (count - 1).coerceAtLeast(0)

    // each child will have same height
    val childHeight =
      if (count > 0 && maxHeight != Constraints.Infinity) {
        ((maxHeight - totalSpacing) / count).coerceAtLeast(0)
      } else {
        0
      }

    val childConstraints =
      if (maxHeight == Constraints.Infinity) {
        constraints
      } else {
        constraints.copy(minHeight = childHeight, maxHeight = childHeight)
      }

    val placeables = measurables.map { it.measure(childConstraints) }

    val widestChild = placeables.maxOfOrNull { it.width } ?: 0
    val width = widestChild.coerceIn(constraints.minWidth, maxWidth)
    val height =
      if (maxHeight == Constraints.Infinity) {
        placeables.sumOf { it.height } + totalSpacing
      } else maxHeight

    layout(width, height) {
      var y = 0
      placeables.forEach { p ->
        p.placeRelative(0, y)
        y += p.height + verticalSpacingPx
      }
    }
  }
}

/**
 * Weight of a key in a row: [defaultWeight] and more while pressed. Used by keys of [KeypadFlow] and
 * by keys in other rows that should behave the same. [isPressed] comes from [trackKeyPress].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun animatePressedWeight(isPressed: Boolean, defaultWeight: Float = 1f): State<Float> {
  return animateFloatAsState(
    targetValue = if (isPressed) PRESSED_WEIGHT_FACTOR * defaultWeight else defaultWeight,
    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
    label = "Pressed key weight",
  )
}

private enum class KeypadSlots {
  Rows
}

private val LocalKeypadSpacingPx = compositionLocalOf { 0 }

internal class KeypadFlowScopeImpl(override val iconHeight: Float) : KeypadFlowScope {

  @Composable
  override fun KeypadRow(content: @Composable RowScope.() -> Unit) {
    val spacingPx = LocalKeypadSpacingPx.current
    // Gaps are inside of keys (half on each side), row is wider by one gap so that outer keys touch
    // the edges. Key with weight 2 is then as wide as 2 keys and the gap between them.
    Row(
      modifier =
        Modifier.fillMaxWidth().layout { measurable, constraints ->
          val rowWidth = constraints.maxWidth + spacingPx
          val placeable =
            measurable.measure(constraints.copy(minWidth = rowWidth, maxWidth = rowWidth))
          layout(constraints.maxWidth, placeable.height) { placeable.place(-spacingPx / 2, 0) }
        },
      content = content,
    )
  }

  @Composable
  override fun RowScope.ButtonTransparent(
    button: KeypadButton.KeypadButtonAdd,
    onClick: (String) -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = { onClick(button.token) },
      onLongClick = null,
      colors = buttonTransparentColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonTransparent(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = onClick,
      onLongClick = null,
      colors = buttonTransparentColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonTertiary(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = onClick,
      onLongClick = null,
      colors = buttonTertiaryColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonFilled(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = onClick,
      onLongClick = null,
      colors = buttonFilledColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonFilled(
    button: KeypadButton.KeypadButtonAdd,
    onClick: (String) -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = { onClick(button.token) },
      onLongClick = null,
      colors = buttonFilledColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonLight(
    button: KeypadButton.KeypadButtonAdd,
    onClick: (String) -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = { onClick(button.token) },
      onLongClick = null,
      colors = buttonLightColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonLight(
    button: KeypadButton.KeypadButtonSimple,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = onClick,
      onLongClick = onLongClick,
      colors = buttonLightColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonFilledPrimary(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean,
    height: Float,
    weight: Float,
  ) =
    BasicKeyboardButton(
      contentHeight = height,
      enabled = enabled,
      defaultWeight = weight,
      onClick = onClick,
      onLongClick = null,
      colors = buttonFilledPrimaryColors(),
      icon = button.icon,
      contentDescription = button.description?.let { stringResource(it) },
    )

  @Composable
  override fun RowScope.ButtonText(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    weight: Float,
    trailingContent: (@Composable () -> Unit)?,
  ) =
    BasicKeyboardButton(
      enabled = enabled,
      defaultWeight = weight,
      onClick = onClick,
      onLongClick = null,
      colors = buttonLightColors(),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = label,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
        )
        if (trailingContent != null) {
          Spacer(Modifier.width(6.dp))
          trailingContent()
        }
      }
    }
}

@Composable
private fun buttonLightColors() =
  ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  )

@Composable
private fun buttonFilledColors() =
  ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  )

// The accent color: orange = key
@Composable
private fun buttonFilledPrimaryColors() =
  ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary,
  )

@Composable
private fun buttonTertiaryColors() =
  ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
  )

@Composable
private fun buttonTransparentColors() =
  ButtonDefaults.textButtonColors(
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  )

@Composable
private fun RowScope.BasicKeyboardButton(
  contentHeight: Float,
  enabled: Boolean,
  defaultWeight: Float,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  colors: ButtonColors,
  icon: ImageVector,
  contentDescription: String?,
) =
  BasicKeyboardButton(
    enabled = enabled,
    defaultWeight = defaultWeight,
    onClick = onClick,
    onLongClick = onLongClick,
    colors = colors,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      modifier =
        Modifier.aspectRatio(1f) // All icons are square (24x24)
          .fillMaxWidth()
          .graphicsLayer {
            scaleX = contentHeight
            scaleY = contentHeight
          },
      tint = LocalContentColor.current,
    )
  }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.BasicKeyboardButton(
  enabled: Boolean,
  defaultWeight: Float,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  colors: ButtonColors,
  content: @Composable () -> Unit,
) {
  val containerColor =
    remember(colors, enabled) {
      if (enabled) colors.containerColor else colors.disabledContainerColor
    }
  val contentColor =
    remember(colors, enabled) { if (enabled) colors.contentColor else colors.disabledContentColor }

  val interactionSource = remember { MutableInteractionSource() }
  // Pressed right away, also on pages that are swiped
  val keyPress = rememberKeyPress()
  val weight: Float by animatePressedWeight(keyPress.isPressed, defaultWeight)
  val spacing = with(LocalDensity.current) { LocalKeypadSpacingPx.current.toDp() }

  Box(
    modifier =
      Modifier.weight(weight)
        .fillMaxHeight()
        .padding(horizontal = spacing / 2)
        .trackKeyPress(keyPress)
        .squashable(
          interactionSource = interactionSource,
          onClick = { onClick() },
          onLongClick = onLongClick,
          // Rounded squares, a bit rounder while pressed
          cornerRadiusRange = 24..36,
          animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
          enabled = enabled,
          isPressed = keyPress.isPressed,
        )
        .background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
  }
}

interface KeypadFlowScope {
  val iconHeight: Float

  @Composable fun KeypadRow(content: @Composable RowScope.() -> Unit)

  @Composable
  fun RowScope.ButtonTransparent(
    button: KeypadButton.KeypadButtonAdd,
    onClick: (String) -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonTransparent(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonTertiary(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonFilled(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonFilled(
    button: KeypadButton.KeypadButtonAdd,
    onClick: (String) -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonLight(
    button: KeypadButton.KeypadButtonAdd,
    onClick: (String) -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonLight(
    button: KeypadButton.KeypadButtonSimple,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  @Composable
  fun RowScope.ButtonFilledPrimary(
    button: KeypadButton.KeypadButtonSimple,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Float = iconHeight,
    weight: Float = 1f,
  )

  /** Light key with a text [label], like `sin` or `unit`. [trailingContent] goes after the label. */
  @Composable
  fun RowScope.ButtonText(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    weight: Float = 1f,
    trailingContent: (@Composable () -> Unit)? = null,
  )
}

object KeyboardButtonToken {

  /** Height factor for icons. Mostly for main button in portrait mode. */
  const val ICON_HEIGHT_TALL = 1.1f

  /** Height factor for icons. Mostly for additional button in portrait mode */
  const val ICON_HEIGHT_TALL_SECONDARY = 1.6f

  /** Height factor for icons. Mostly for main button in landscape mode. */
  const val ICON_HEIGHT_SHORT = 1.3f

  /** Height factor for icons. Mostly for additional button in landscape mode. */
  const val ICON_HEIGHT_SHORT_SECONDARY = 1.1f
}

/** How many times wider a key gets while it is pressed. */
private const val PRESSED_WEIGHT_FACTOR = 1.5f
/** Space between keys: this part of the keypad width (between columns) or height (between rows) */
const val KEYPAD_SPACING_FRACTION = 0.02f
