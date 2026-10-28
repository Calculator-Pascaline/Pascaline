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

package com.sadellie.unitto.feature.calculator.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.textfield.formatExpression
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.data_clear
import unitto.core.common.generated.resources.data_delete_row

/**
 * Lists of numbers side by side, like the data pages of NumWorks Statistics and Regression. There
 * is always an empty row at the end: typing in it adds a row.
 *
 * @param rows Text of every cell, row by row. Every row has one cell per header.
 * @param onCellChange Row index can be `rows.size` for the empty row.
 */
@Composable
internal fun DataTable(
  modifier: Modifier,
  headers: List<String>,
  rows: List<List<String>>,
  onCellChange: (row: Int, column: Int, value: String) -> Unit,
  onDeleteRow: (Int) -> Unit,
  onClear: () -> Unit,
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      headers.forEach { header ->
        Text(
          modifier = Modifier.weight(1f).padding(vertical = 12.dp),
          text = header,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
      }
      // Room of the delete buttons
      Box(Modifier.size(DELETE_WIDTH))
    }
    HorizontalDivider()
    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
      items(rows.size + 1) { index ->
        val cells = rows.getOrNull(index) ?: List(headers.size) { "" }
        Row(modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
          cells.forEachIndexed { column, text ->
            NumberCell(
              modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
              value = text,
              onValueChange = { onCellChange(index, column, it) },
            )
          }
          if (index < rows.size) {
            // Not focusable: the Next key of the keyboard goes from cell to cell
            IconButton(
              modifier = Modifier.size(DELETE_WIDTH).focusProperties { canFocus = false },
              onClick = { onDeleteRow(index) },
            ) {
              Icon(Symbols.Delete, stringResource(Res.string.data_delete_row))
            }
          } else {
            Box(Modifier.size(DELETE_WIDTH))
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }
      if (rows.isNotEmpty()) {
        item {
          TextButton(modifier = Modifier.padding(8.dp), onClick = onClear) {
            Text(stringResource(Res.string.data_clear))
          }
        }
      }
    }
  }
}

/** Number typed with the decimal keyboard. Red when it is not a number. */
@Composable
private fun NumberCell(modifier: Modifier, value: String, onValueChange: (String) -> Unit) {
  val isError = value.isNotBlank() && value.toNumberOrNull() == null
  val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
  BasicTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    singleLine = true,
    textStyle = MaterialTheme.typography.bodyLarge.copy(color = color, textAlign = TextAlign.Center),
    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
    decorationBox = { innerTextField ->
      Box(
        modifier =
          Modifier.clip(CELL_SHAPE)
            .background(if (isError) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        innerTextField()
      }
    },
  )
}

/** How the calculator shows numbers: precision, scientific or not, decimal separator. */
internal class NumberFormat(
  private val formatterSymbols: FormatterSymbols,
  private val precision: Int,
  private val outputFormat: Int,
) {
  /** `−2.5`, or [undefined] when not a number. */
  fun format(value: Double, undefined: String = "—"): String {
    if (!value.isFinite()) return undefined
    return KBigDecimal.valueOf(value)
      .toFormattedString(precision, outputFormat)
      .replace("-", Token.Operator.MINUS)
      .formatExpression(formatterSymbols)
  }
}

/** Cells of a column as numbers, null for empty or wrong ones. */
internal fun List<List<String>>.column(index: Int): List<Double?> = map { it.getOrNull(index)?.toNumberOrNull() }

private val ROW_HEIGHT = 52.dp
private val DELETE_WIDTH = 48.dp
private val CELL_SHAPE = RoundedCornerShape(8.dp)
