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

package com.sadellie.unitto.feature.calculator.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.UnitMenuItem
import com.sadellie.unitto.core.designsystem.icons.symbols.ArrowBack
import com.sadellie.unitto.core.designsystem.icons.symbols.ChevronRight
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.calculator_units_and_constants
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.sadellie.unitto.core.ui.keyHaptics

/**
 * Function key: [label] is shown, [token] is inserted. [inverse] is used instead when inverse switch
 * is on: cos ==> acos.
 */
private class FunctionKey(val label: String, val token: String, val inverse: FunctionKey? = null)

/** Not a key, switches trigonometric keys to their inverses. */
private val INVERSE_SWITCH = FunctionKey("inv", "")

/**
 * Function keys on pages that are swiped horizontally. Keys fill the page: fewer rows are taller,
 * fewer keys in a row are wider. Dots under the keys show current page.
 *
 * @param rowHeight Height of one row when page has [PAGE_ROWS] rows.
 */
@Composable
internal fun FunctionRows(modifier: Modifier, rowHeight: Dp, onAddTokenClick: (String) -> Unit) {
  val pagerState = rememberPagerState(pageCount = { functionPages.size })
  var inverse by rememberSaveable { mutableStateOf(false) }
  Column(modifier = modifier) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxWidth().height(rowHeight * PAGE_ROWS),
      pageSpacing = KEY_SPACING,
    ) { page ->
      Column(modifier = Modifier.fillMaxSize()) {
        functionPages[page].forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = KEY_SPACING / 2),
            horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
          ) {
            row.forEach { key ->
              val keyModifier = Modifier.weight(1f).fillMaxHeight()
              if (key === INVERSE_SWITCH) {
                InverseSwitch(keyModifier, inverse) { inverse = it }
              } else {
                FunctionKeyButton(
                  modifier = keyModifier,
                  key = key.inverse?.takeIf { inverse } ?: key,
                  onClick = onAddTokenClick,
                )
              }
            }
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth().height(DOTS_HEIGHT),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(functionPages.size) { index ->
        val color =
          if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DOT_ALPHA)
        Box(
          modifier =
            Modifier.padding(horizontal = 3.dp).size(DOT_SIZE).clip(CircleShape).background(color)
        )
      }
    }
  }
}

@Composable
private fun FunctionKeyButton(modifier: Modifier, key: FunctionKey, onClick: (String) -> Unit) {
  // Vibrates like the icon keys
  val interactionSource = remember { MutableInteractionSource() }
  Surface(
    modifier = modifier.keyHaptics(interactionSource),
    onClick = { onClick(key.token) },
    interactionSource = interactionSource,
    shape = KEY_SHAPE,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = key.label,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun InverseSwitch(modifier: Modifier, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  val interactionSource = remember { MutableInteractionSource() }
  Surface(
    modifier = modifier.keyHaptics(interactionSource),
    checked = checked,
    onCheckedChange = onCheckedChange,
    interactionSource = interactionSource,
    shape = KEY_SHAPE,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = INVERSE_SWITCH.label, style = MaterialTheme.typography.titleLarge)
      Spacer(Modifier.width(6.dp))
      Switch(checked = checked, onCheckedChange = null)
    }
  }
}

private fun function(name: String) = FunctionKey(name, "$name(")

/** cos, and acos when inverse switch is on. */
private fun trig(name: String, inverseName: String) =
  FunctionKey(name, "$name(", inverse = function(inverseName))

private val functionPages =
  listOf(
    // Exponents and roots
    listOf(
      listOf(
        FunctionKey("eˣ", Token.Layout.EXP),
        FunctionKey(Token.Func.LN, Token.Func.LN_BRACKET),
        FunctionKey(Token.Func.LOG, Token.Layout.LOG_10),
        FunctionKey("×10ˣ", Token.Operator.MULTIPLY + Token.Layout.POWER_10),
      ),
      listOf(
        FunctionKey(Token.Operator.SQRT, Token.Layout.SQRT),
        FunctionKey("x²", Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START + "2" + Token.Layout.END),
      ),
    ),
    // Trigonometry, inverse functions with the switch
    listOf(
      listOf(
        trig(Token.Func.COS, Token.Func.ARCOS),
        trig(Token.Func.SIN, Token.Func.ARSIN),
        trig(Token.Func.TAN, Token.Func.ACTAN),
        FunctionKey(Token.Const.PI, Token.Const.PI),
      ),
      listOf(
        trig(Token.Func.COSH, Token.Func.ARCOSH),
        trig(Token.Func.SINH, Token.Func.ARSINH),
        trig(Token.Func.TANH, Token.Func.ARTANH),
        INVERSE_SWITCH,
      ),
    ),
    // Calculus. Integral without bounds is indefinite
    listOf(
      listOf(
        FunctionKey("∫ □ dx", Token.Layout.INTEGRAL),
        FunctionKey("d/dx(□)", Token.Layout.DERIVATIVE),
      ),
      listOf(
        FunctionKey(Token.Calculus.SUM + " □", Token.Layout.SUM),
        FunctionKey(Token.Calculus.PRODUCT + " □", Token.Layout.PRODUCT),
        FunctionKey(Token.Const.INFINITY, Token.Const.INFINITY),
      ),
    ),
  )

/** Units and constants, same menus as on NumWorks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitsSheet(onDismiss: () -> Unit, onUnitClick: (String) -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val path = remember { mutableStateListOf<UnitMenuItem.Category>() }
  val items = path.lastOrNull()?.children ?: PhysicalUnits.menu

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (path.isNotEmpty()) {
        IconButton(onClick = { path.removeAt(path.lastIndex) }) {
          Icon(Symbols.ArrowBack, contentDescription = null)
        }
      } else {
        Spacer(Modifier.width(16.dp))
      }
      Text(
        text = path.lastOrNull()?.name ?: stringResource(Res.string.calculator_units_and_constants),
        style = MaterialTheme.typography.titleLarge,
      )
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
      items(items) { item ->
        when (item) {
          is UnitMenuItem.Category ->
            MenuRow(
              title = item.name,
              onClick = { path.add(item) },
              trailing = {
                Icon(Symbols.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
              },
            )
          is UnitMenuItem.Leaf ->
            MenuRow(
              title = item.unit.symbol,
              subtitle = item.unit.name,
              onClick = { onUnitClick(item.unit.token) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
      }
    }
  }
}

@Composable
private fun MenuRow(
  title: String,
  onClick: () -> Unit,
  subtitle: String? = null,
  trailing: @Composable () -> Unit = {},
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      modifier = Modifier.weight(1f),
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = if (subtitle != null) FontWeight.Bold else FontWeight.Normal,
    )
    if (subtitle != null) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    trailing()
  }
}

private val KEY_SPACING = 6.dp
internal const val PAGE_ROWS = 4
private const val DOT_ALPHA = 0.3f
private val DOT_SIZE = 6.dp
internal val DOTS_HEIGHT = 14.dp
private const val DIVIDER_ALPHA = 0.5f

/** Same corners as the other keys. */
internal val KEY_SHAPE = RoundedCornerShape(percent = 24)
