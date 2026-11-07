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

package com.sadellie.unitto.feature.calculator.elements

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.statistics.StatLine
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.elements_actinide
import unitto.core.common.generated.resources.elements_alkali_metal
import unitto.core.common.generated.resources.elements_alkaline_earth_metal
import unitto.core.common.generated.resources.elements_atomic_number
import unitto.core.common.generated.resources.elements_configuration
import unitto.core.common.generated.resources.elements_electronegativity
import unitto.core.common.generated.resources.elements_group
import unitto.core.common.generated.resources.elements_halogen
import unitto.core.common.generated.resources.elements_lanthanide
import unitto.core.common.generated.resources.elements_mass
import unitto.core.common.generated.resources.elements_metalloid
import unitto.core.common.generated.resources.elements_noble_gas
import unitto.core.common.generated.resources.elements_nonmetal
import unitto.core.common.generated.resources.elements_period
import unitto.core.common.generated.resources.elements_post_transition_metal
import unitto.core.common.generated.resources.elements_shells
import unitto.core.common.generated.resources.elements_title
import unitto.core.common.generated.resources.elements_transition_metal
import unitto.core.common.generated.resources.elements_unknown

/** Like the Elements app of NumWorks: the periodic table, tap an element to read about it. */
@Composable
internal fun ElementsRoute(openDrawer: () -> Unit) {
  var selected by rememberSaveable { mutableIntStateOf(1) }
  val element = ELEMENTS[selected - 1]

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.elements_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val height = maxHeight
      if (maxWidth >= WIDE_LAYOUT_WIDTH) {
        // The table on the left, the chosen element on the right
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Column(modifier = Modifier.weight(TABLE_WEIGHT).fillMaxHeight().verticalScroll(rememberScrollState())) {
            PeriodicTable(
              modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
              selected = selected,
              onClick = { selected = it },
              fitHeight = height - LEGEND_SPACE,
            )
            FamilyLegend()
          }
          ElementPanel(element, Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp))
        }
      } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
          ElementCard(element, Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
          PeriodicTable(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            selected = selected,
            onClick = { selected = it },
          )
          ElementDetails(element)
          HorizontalDivider()
          FamilyLegend()
        }
      }
    }
  }
}

/** The chosen element on wide screens: its atom in big, then everything about it. */
@Composable
private fun ElementPanel(element: Element, modifier: Modifier) {
  val color by animateColorAsState(element.family.color, label = "Element color")
  Column(
    modifier = modifier.clip(TILE_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer).verticalScroll(rememberScrollState()),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().clip(TILE_SHAPE).background(color).padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      AtomView(
        modifier = Modifier.size(PANEL_ATOM_SIZE),
        shells = element.shells,
        nucleus = CELL_TEXT,
        line = CELL_TEXT.copy(alpha = ATOM_LINE_ALPHA),
        electron = CELL_TEXT,
      )
      AnimatedContent(
        targetState = element,
        transitionSpec = { (fadeIn() + slideInVertically { it / NAME_RISE }) togetherWith fadeOut() },
        label = "Element name",
      ) { shown ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(shown.number.toString(), color = CELL_TEXT, style = MaterialTheme.typography.titleMedium)
          Text(shown.symbol, color = CELL_TEXT, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
          Text(shown.name, color = CELL_TEXT, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
          Text(stringResource(shown.family.title), color = CELL_TEXT, style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
    Column(modifier = Modifier.padding(vertical = 8.dp)) { ElementDetails(element) }
  }
}

/** The element in big, in the color of its family, with its atom going around. */
@Composable
private fun ElementCard(element: Element, modifier: Modifier) {
  val color by animateColorAsState(element.family.color, label = "Element color")
  Row(
    modifier = modifier.clip(TILE_SHAPE).background(color).padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    AtomView(
      modifier = Modifier.size(ATOM_SIZE),
      shells = element.shells,
      nucleus = CELL_TEXT,
      line = CELL_TEXT.copy(alpha = ATOM_LINE_ALPHA),
      electron = CELL_TEXT,
    )
    AnimatedContent(
      targetState = element,
      modifier = Modifier.weight(1f),
      transitionSpec = { (fadeIn() + slideInVertically { it / NAME_RISE }) togetherWith fadeOut() },
      label = "Element name",
    ) { shown ->
      Column {
        Text(shown.number.toString(), color = CELL_TEXT, style = MaterialTheme.typography.titleMedium)
        Text(
          shown.symbol,
          color = CELL_TEXT,
          style = MaterialTheme.typography.displayMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(shown.name, color = CELL_TEXT, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(stringResource(shown.family.title), color = CELL_TEXT, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
      }
    }
  }
}

/** 18 columns, as wide as the screen. Lanthanides and actinides under it. */
@Composable
private fun PeriodicTable(modifier: Modifier, selected: Int, onClick: (Int) -> Unit, fitHeight: Dp = Dp.Infinity) {
  BoxWithConstraints(modifier = modifier) {
    val cellWidth = minOf(maxWidth / COLUMNS, fitHeight / (ROWS * CELL_ASPECT))
    val start = (maxWidth - cellWidth * COLUMNS) / 2
    val cellHeight = cellWidth * CELL_ASPECT
    val fontSize = with(LocalDensity.current) { (cellWidth * SYMBOL_SIZE).toSp() }
    Box(modifier = Modifier.fillMaxWidth().height(cellHeight * ROWS)) {
      ELEMENTS.forEach { element ->
        val (row, column) = element.tablePosition()
        val isSelected = element.number == selected
        Box(
          modifier =
            Modifier.offset(x = start + cellWidth * column, y = cellHeight * row)
              .size(cellWidth, cellHeight)
              .padding(CELL_GAP)
              .clip(CELL_SHAPE)
              .background(element.family.color)
              .border(if (isSelected) SELECTED_BORDER else 0.dp, MaterialTheme.colorScheme.onSurface, CELL_SHAPE)
              .clickable { onClick(element.number) },
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = element.symbol,
            color = CELL_TEXT,
            fontSize = fontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun ElementDetails(element: Element) {
  val mass = KBigDecimal.valueOf(element.mass).toFormattedString(MASS_DECIMALS, OutputFormat.PLAIN)
  StatLine(stringResource(Res.string.elements_atomic_number), element.number.toString())
  StatLine(stringResource(Res.string.elements_mass), if (element.isMassNumber) "[$mass] g/mol" else "$mass g/mol")
  StatLine(stringResource(Res.string.elements_group), element.group?.toString() ?: "—")
  StatLine(stringResource(Res.string.elements_period), element.period.toString())
  StatLine(stringResource(Res.string.elements_configuration), element.configuration)
  StatLine(stringResource(Res.string.elements_shells), element.shells.joinToString(" · "))
  StatLine(stringResource(Res.string.elements_electronegativity), element.electronegativity?.toString() ?: "—")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FamilyLegend() {
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ElementFamily.entries.forEach { family ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(LEGEND_DOT).clip(CELL_SHAPE).background(family.color))
        Text(modifier = Modifier.padding(start = 6.dp), text = stringResource(family.title), style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

/** Light colors, readable with dark text in light and dark themes. */
private val ElementFamily.color: Color
  get() =
    when (this) {
      ElementFamily.ALKALI_METAL -> Color(0xFFFFA8A8)
      ElementFamily.ALKALINE_EARTH_METAL -> Color(0xFFFFCC99)
      ElementFamily.TRANSITION_METAL -> Color(0xFFFFE699)
      ElementFamily.POST_TRANSITION_METAL -> Color(0xFFC7E6A3)
      ElementFamily.LANTHANIDE -> Color(0xFFFFB8E1)
      ElementFamily.ACTINIDE -> Color(0xFFE0B8FF)
      ElementFamily.METALLOID -> Color(0xFF9EE0CF)
      ElementFamily.NONMETAL -> Color(0xFFA8D4FF)
      ElementFamily.HALOGEN -> Color(0xFFB8F0FF)
      ElementFamily.NOBLE_GAS -> Color(0xFFC9C2FF)
      ElementFamily.UNKNOWN -> Color(0xFFDDDDDD)
    }

private val ElementFamily.title: StringResource
  get() =
    when (this) {
      ElementFamily.ALKALI_METAL -> Res.string.elements_alkali_metal
      ElementFamily.ALKALINE_EARTH_METAL -> Res.string.elements_alkaline_earth_metal
      ElementFamily.TRANSITION_METAL -> Res.string.elements_transition_metal
      ElementFamily.POST_TRANSITION_METAL -> Res.string.elements_post_transition_metal
      ElementFamily.LANTHANIDE -> Res.string.elements_lanthanide
      ElementFamily.ACTINIDE -> Res.string.elements_actinide
      ElementFamily.METALLOID -> Res.string.elements_metalloid
      ElementFamily.NONMETAL -> Res.string.elements_nonmetal
      ElementFamily.HALOGEN -> Res.string.elements_halogen
      ElementFamily.NOBLE_GAS -> Res.string.elements_noble_gas
      ElementFamily.UNKNOWN -> Res.string.elements_unknown
    }

private const val COLUMNS = 18
/** 7 periods, an empty row, lanthanides and actinides */
private const val ROWS = 10
private const val CELL_ASPECT = 1.25f
private const val SYMBOL_SIZE = 0.42f
private const val MASS_DECIMALS = 4
private val CELL_TEXT = Color(0xFF1B1B1F)
private val CELL_GAP = 1.dp
private val CELL_SHAPE = RoundedCornerShape(3.dp)
private val SELECTED_BORDER = 2.dp
private val ATOM_SIZE = 148.dp
private val TILE_SHAPE = RoundedCornerShape(24.dp)
private const val ATOM_LINE_ALPHA = 0.35f
private const val NAME_RISE = 4
private val LEGEND_DOT = 14.dp
private val WIDE_LAYOUT_WIDTH = 840.dp
private val LEGEND_SPACE = 110.dp
private val PANEL_ATOM_SIZE = 220.dp
private const val TABLE_WEIGHT = 1.9f
