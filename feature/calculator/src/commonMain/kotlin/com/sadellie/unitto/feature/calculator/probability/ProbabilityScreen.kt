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

package com.sadellie.unitto.feature.calculator.probability

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.designsystem.icons.symbols.ArrowBack
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.BackHandler
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.textfield.formatExpression
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.probability_back
import unitto.core.common.generated.resources.probability_choose
import unitto.core.common.generated.resources.probability_invalid
import unitto.core.common.generated.resources.probability_next
import unitto.core.common.generated.resources.probability_title

@Composable
internal fun ProbabilityRoute(openDrawer: () -> Unit) {
  val viewModel: ProbabilityViewModel = koinViewModel()
  val format = viewModel.numberFormat.collectAsStateWithLifecycleKMP().value ?: return EmptyScreen()
  val type = viewModel.type
  BackHandler(enabled = type != null) { viewModel.back() }

  ScaffoldWithTopBar(
    title = { Text(stringResource(type?.title ?: Res.string.probability_title)) },
    navigationIcon = {
      if (type == null) {
        DrawerButton(onClick = openDrawer)
      } else {
        IconButton(onClick = { viewModel.back() }) {
          Icon(Symbols.ArrowBack, stringResource(Res.string.probability_back))
        }
      }
    },
    keepNavigationIconOnWideScreens = type != null,
  ) { paddingValues ->
    val modifier = Modifier.padding(paddingValues).fillMaxSize()
    when {
      type == null -> DistributionList(modifier, onClick = viewModel::choose)
      !viewModel.isCalculating -> ParametersStep(modifier, type, viewModel, format)
      else -> viewModel.distribution?.let { CalculationStep(modifier, it, viewModel, format) }
    }
  }
}

/** Every distribution as a card with a picture of its default shape. */
@Composable
private fun DistributionList(modifier: Modifier, onClick: (DistributionType) -> Unit) {
  LazyVerticalGrid(
    modifier = modifier,
    columns = GridCells.Adaptive(LIST_CARD_MIN_WIDTH),
    contentPadding = PaddingValues(SCREEN_PADDING),
    verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
    horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
  ) {
    item(span = { GridItemSpan(maxLineSpan) }) {
      Text(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        text = stringResource(Res.string.probability_choose),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    items(DistributionType.entries) { type ->
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .clip(CARD_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick(type) }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        val preview = type.create(type.previewValues())
        if (preview != null) {
          DistributionGraph(
            modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT),
            distribution = preview,
            shaded = null,
            showLabels = false,
          )
        }
        Text(text = stringResource(type.title), style = MaterialTheme.typography.titleSmall, maxLines = 1)
      }
    }
  }
}

/** Parameters of the picture in the list: defaults, or ones with a clearer shape than a spike. */
@Suppress("MagicNumber")
private fun DistributionType.previewValues(): List<Double> =
  when (this) {
    DistributionType.CHI_SQUARED -> listOf(4.0)
    DistributionType.STUDENT -> listOf(8.0)
    DistributionType.FISHER -> listOf(10.0, 20.0)
    else -> parameters.map { it.default.toNumberOrNull() ?: 0.0 }
  }

/** Second step: parameters with a live picture of the distribution, then on to the calculation. */
@Composable
private fun ParametersStep(
  modifier: Modifier,
  type: DistributionType,
  viewModel: ProbabilityViewModel,
  format: NumberFormat,
) {
  val fractional = format.formatterSymbols.fractional
  val distribution = viewModel.distribution
  val onPreset: (List<String>) -> Unit = { values ->
    values.forEachIndexed { index, value -> viewModel.setParameter(index, value.replace(".", fractional)) }
  }
  TwoPanes(
    modifier = modifier,
    first = {
      if (distribution == null) {
        InvalidCard()
      } else {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(GRAPH_HEIGHT)
              .clip(CARD_SHAPE)
              .background(MaterialTheme.colorScheme.surfaceContainer)
              .padding(12.dp)
        ) {
          DistributionGraph(Modifier.fillMaxSize(), distribution, shaded = null, showLabels = true)
        }
      }
      ParametersCard(type, viewModel, fractional)
      Button(
        modifier = Modifier.fillMaxWidth().height(BUTTON_HEIGHT),
        onClick = viewModel::calculate,
        enabled = distribution != null,
      ) {
        Text(stringResource(Res.string.probability_next), style = MaterialTheme.typography.titleMedium)
      }
    },
    second = {
      if (type == DistributionType.BINOMIAL) BinomialPresets(fractional = fractional, onPick = onPreset)
      if (type == DistributionType.NORMAL) NormalPresets(fractional = fractional, onPick = onPreset)
    },
  )
}

/** Third step: what to know, the answer first, and the graph with its handles. */
@Composable
private fun CalculationStep(
  modifier: Modifier,
  distribution: Distribution,
  viewModel: ProbabilityViewModel,
  format: NumberFormat,
) {
  TwoPanes(
    modifier = modifier,
    first = {
      ResultCard(distribution, viewModel, format)
      GraphCard(distribution, viewModel)
      CalculationSelector(distribution, viewModel)
    },
    second = { MomentCards(viewModel, format.formatterSymbols.fractional) },
  )
}

/** Below each other on phones, side by side on wide screens. */
@Composable
private fun TwoPanes(
  modifier: Modifier,
  first: @Composable () -> Unit,
  second: @Composable () -> Unit,
) {
  BoxWithConstraints(modifier = modifier) {
    val scrolling = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SCREEN_PADDING)
    val spacing = Arrangement.spacedBy(CARD_SPACING * 2)
    if (maxWidth >= WIDE_LAYOUT_WIDTH) {
      Row(modifier = scrolling, horizontalArrangement = spacing) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = spacing) { first() }
        Column(modifier = Modifier.weight(1f), verticalArrangement = spacing) { second() }
      }
    } else {
      Column(modifier = scrolling, verticalArrangement = spacing) {
        first()
        second()
      }
    }
  }
}

@Composable
private fun InvalidCard() {
  Text(
    modifier =
      Modifier.fillMaxWidth()
        .clip(CARD_SHAPE)
        .background(MaterialTheme.colorScheme.errorContainer)
        .padding(16.dp),
    text = stringResource(Res.string.probability_invalid),
    color = MaterialTheme.colorScheme.onErrorContainer,
    style = MaterialTheme.typography.bodyLarge,
  )
}

/** The question with its bounds, then the answer in big. Typing the answer finds the bound. */
@Composable
private fun ResultCard(distribution: Distribution, viewModel: ProbabilityViewModel, format: NumberFormat) {
  val calculation = viewModel.calculation
  val canFindBound = calculation == CalculationType.AT_MOST || calculation == CalculationType.AT_LEAST
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(CARD_SHAPE)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
  ) {
    BoundsEquation(calculation, viewModel.a, viewModel.b, viewModel::updateA, viewModel::updateB)
    EquationSlot(
      value = viewModel.typedProbability ?: viewModel.probability(distribution).format(format, ""),
      onValueChange = viewModel::setProbability,
      placeholder = "p",
      readOnly = !canFindBound,
      size = SlotSize.LARGE,
      onDone = viewModel::finishProbability,
    )
  }
}

/** Slider and typing box for every parameter. */
@Composable
private fun ParametersCard(type: DistributionType, viewModel: ProbabilityViewModel, fractional: String) {
  val values = viewModel.parameters.map { it.toNumberOrNull() ?: 0.0 }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(CARD_SHAPE)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    type.parameters.forEachIndexed { index, parameter ->
      val value = viewModel.parameters.getOrNull(index) ?: return@forEachIndexed
      ParameterRow(parameter, value, values, fractional) { viewModel.setParameter(index, it) }
    }
  }
}

@Composable
private fun ParameterRow(
  parameter: Parameter,
  value: String,
  values: List<Double>,
  fractional: String,
  onValueChange: (String) -> Unit,
) {
  val number = value.toNumberOrNull()
  val range = parameter.slider(values)
  // A typed number outside of the usual range stays on the slider
  val start = min(range.start, number ?: range.start)
  val end = max(max(range.endInclusive, number ?: range.endInclusive), start + parameter.step)
  Column(modifier = Modifier.padding(vertical = 4.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = parameter.symbol,
        modifier = Modifier.padding(end = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = stringResource(parameter.description),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      EquationSlot(
        value = value,
        onValueChange = onValueChange,
        placeholder = parameter.symbol,
        size = SlotSize.SMALL,
        isInvalid = number != null && !parameter.isValid(number),
      )
    }
    Slider(
      value = (number ?: start).coerceIn(start, end).toFloat(),
      onValueChange = { onValueChange(sliderValue(it.toDouble(), parameter.step, fractional)) },
      valueRange = start.toFloat()..end.toFloat(),
    )
  }
}

/** Graph with handles on the bounds: a swipe or a tap moves the closest one. */
@Composable
private fun GraphCard(distribution: Distribution, viewModel: ProbabilityViewModel) {
  val calculation = viewModel.calculation
  val a = viewModel.a.toNumberOrNull()
  val b = viewModel.b.toNumberOrNull()
  val shaded = shadedRange(calculation, a, b)
  val handles = listOfNotNull(a, b.takeIf { calculation.usesB })
  val current by rememberUpdatedState(handles to graphRange(distribution, shaded))
  val visible = distribution.visibleRange()
  fun Offset.value(width: Int): Double {
    val range = current.second
    val value = range.start + (x / width).toDouble().coerceIn(0.0, 1.0) * (range.endInclusive - range.start)
    return value.coerceIn(visible.start, visible.endInclusive)
  }
  fun isUpper(value: Double): Boolean {
    val bounds = current.first
    return bounds.size == 2 && abs(value - bounds[1]) < abs(value - bounds[0])
  }
  var isDraggingUpper by remember { mutableStateOf(false) }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .height(GRAPH_HEIGHT)
        .clip(CARD_SHAPE)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .pointerInput(distribution) {
          detectTapGestures { offset ->
            val value = offset.value(size.width)
            viewModel.dragBound(isUpper(value), value)
          }
        }
        .pointerInput(distribution) {
          detectHorizontalDragGestures(
            onDragStart = { offset -> isDraggingUpper = isUpper(offset.value(size.width)) },
            onHorizontalDrag = { change, _ ->
              change.consume()
              viewModel.dragBound(isDraggingUpper, change.position.value(size.width))
            },
          )
        }
        .padding(horizontal = 12.dp, vertical = 12.dp)
  ) {
    DistributionGraph(Modifier.fillMaxSize(), distribution, shaded, showLabels = true, handles = handles)
  }
}

/** Kind of probability, as connected buttons. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CalculationSelector(distribution: Distribution, viewModel: ProbabilityViewModel) {
  val types = CalculationType.entries.filter { !it.discreteOnly || distribution.isDiscrete }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    types.forEach { type ->
      ToggleButton(
        checked = viewModel.calculation == type,
        onCheckedChange = { viewModel.selectCalculation(type) },
        modifier = Modifier.weight(if (type.usesB) WIDE_BUTTON_WEIGHT else 1f),
        contentPadding = PaddingValues(horizontal = 4.dp),
      ) {
        Text(type.symbol(), style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
      }
    }
  }
}

/** Part of the graph to shade for the calculation, null until its bounds are typed. */
private fun shadedRange(calculation: CalculationType, a: Double?, b: Double?): ClosedFloatingPointRange<Double>? =
  when {
    a == null -> null
    calculation == CalculationType.AT_MOST -> Double.NEGATIVE_INFINITY..a
    calculation == CalculationType.AT_LEAST -> a..Double.POSITIVE_INFINITY
    calculation == CalculationType.EXACTLY -> a..a
    b == null -> null
    else -> a..b
  }

/** Expected value, variance and explanations of the distribution, once its parameters are typed. */
@Composable
private fun MomentCards(viewModel: ProbabilityViewModel, fractional: String) {
  val type = viewModel.type ?: return
  if (type == DistributionType.BINOMIAL) {
    val n = viewModel.parameters.getOrNull(0)?.toNumberOrNull()?.toInt()
    val p = viewModel.parameters.getOrNull(1)?.toNumberOrNull()
    if (n != null && p != null) {
      BinomialMoments(n, p, fractional)
      BinomialExplanation(n, p, fractional)
    }
    return
  }
  val values = viewModel.parameters.mapNotNull { it.toNumberOrNull() }
  if (values.size != type.parameters.size) return
  if (type == DistributionType.NORMAL) {
    NormalMoments(values[0], values[1], fractional)
    NormalRule(values[0], values[1], fractional)
  } else {
    DistributionMoments(type, values, fractional)
  }
}

/** `P(X ≤ [a])`: boxes are text fields, the rest is text. Wraps on narrow screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoundsEquation(
  calculation: CalculationType,
  a: String,
  b: String,
  onA: (String) -> Unit,
  onB: (String) -> Unit,
) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
  ) {
    when (calculation) {
      CalculationType.AT_MOST -> {
        EquationText("P(X ≤")
        EquationSlot(a, onA, "a")
        EquationText(")")
      }
      CalculationType.AT_LEAST -> {
        EquationText("P(X ≥")
        EquationSlot(a, onA, "a")
        EquationText(")")
      }
      CalculationType.EXACTLY -> {
        EquationText("P(X =")
        EquationSlot(a, onA, "a")
        EquationText(")")
      }
      CalculationType.BETWEEN -> {
        EquationText("P(")
        EquationSlot(a, onA, "a")
        EquationText("≤ X ≤")
        EquationSlot(b, onB, "b")
        EquationText(")")
      }
    }
  }
}

@Composable
private fun EquationText(text: String) {
  Box(modifier = Modifier.height(SlotSize.MEDIUM.height), contentAlignment = Alignment.Center) {
    Text(
      text,
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}

/** Sizes of [EquationSlot]: bounds are medium, parameters small, the answer large. */
private enum class SlotSize(val height: Dp) {
  SMALL(40.dp),
  MEDIUM(44.dp),
  LARGE(64.dp),
}

/** Number typed in place, in a rounded box that grows with it. */
@Composable
private fun EquationSlot(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  readOnly: Boolean = false,
  size: SlotSize = SlotSize.MEDIUM,
  isInvalid: Boolean = false,
  onDone: () -> Unit = {},
) {
  val isError = isInvalid || (value.isNotBlank() && value.toNumberOrNull() == null)
  val isAnswer = size == SlotSize.LARGE
  val style = slotTextStyle(size, isError)
  // Box grows with the number: measured text plus padding and room for the cursor
  val measurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val textWidth = with(density) { measurer.measure(value.ifEmpty { placeholder }, style).size.width.toDp() }
  val padding = if (isAnswer) 0.dp else SLOT_PADDING
  val focusManager = LocalFocusManager.current
  var wasFocused by remember { mutableStateOf(false) }
  BasicTextField(
    modifier =
      Modifier.height(size.height)
        .then(if (isAnswer) Modifier.fillMaxWidth() else Modifier.width(maxOf(SLOT_MIN_WIDTH, textWidth + padding * 2 + 4.dp)))
        .onFocusChanged {
          // Leaving the box ends the typing
          if (wasFocused && !it.isFocused) onDone()
          wasFocused = it.isFocused
        },
    value = value,
    onValueChange = onValueChange,
    readOnly = readOnly,
    singleLine = true,
    textStyle = style,
    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    decorationBox = { innerTextField ->
      SlotBox(isAnswer, isError && !isAnswer, padding) {
        if (value.isEmpty()) {
          Text(
            placeholder,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA),
          )
        }
        innerTextField()
      }
    },
  )
}

@Composable
private fun slotTextStyle(size: SlotSize, isError: Boolean): TextStyle {
  val isAnswer = size == SlotSize.LARGE
  val base =
    when (size) {
      SlotSize.SMALL -> MaterialTheme.typography.titleMedium
      SlotSize.MEDIUM -> MaterialTheme.typography.titleLarge
      SlotSize.LARGE -> MaterialTheme.typography.displayMedium
    }
  val color =
    when {
      isError -> MaterialTheme.colorScheme.error
      isAnswer -> MaterialTheme.colorScheme.onPrimaryContainer
      else -> MaterialTheme.colorScheme.primary
    }
  return base.copy(
    color = color,
    fontWeight = FontWeight.Medium,
    textAlign = if (isAnswer) TextAlign.Start else TextAlign.Center,
  )
}

/** Rounded background of a number, the answer stands on the card without one. */
@Composable
private fun SlotBox(isAnswer: Boolean, hasBorder: Boolean, padding: Dp, content: @Composable () -> Unit) {
  Box(
    modifier =
      Modifier.clip(SLOT_SHAPE)
        .background(if (isAnswer) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHighest)
        .border(SLOT_BORDER, if (hasBorder) MaterialTheme.colorScheme.error else Color.Transparent, SLOT_SHAPE)
        .padding(horizontal = padding),
    contentAlignment = if (isAnswer) Alignment.CenterStart else Alignment.Center,
  ) {
    content()
  }
}

/**
 * Density curve of continuous distributions, bars for discrete ones. Values in [shaded] are
 * highlighted.
 */
@Composable
internal fun DistributionGraph(
  modifier: Modifier,
  distribution: Distribution,
  shaded: ClosedRange<Double>?,
  showLabels: Boolean,
  // Without them only the bounds of the shaded area are labelled, for small graphs
  showEdgeLabels: Boolean = true,
  // Values with a handle that can be dragged
  handles: List<Double> = emptyList(),
) {
  val measurer = rememberTextMeasurer()
  val accent = MaterialTheme.colorScheme.primary
  val muted = MaterialTheme.colorScheme.outline
  val axis = MaterialTheme.colorScheme.onSurfaceVariant
  val labelStyle = MaterialTheme.typography.labelMedium.copy(color = axis)
  val boundStyle = labelStyle.copy(color = accent, fontWeight = FontWeight.Bold)
  val handleColor = MaterialTheme.colorScheme.tertiary

  Canvas(modifier = modifier) {
    val labelSpace = if (showLabels) LABEL_SPACE.toPx() else 0f
    val plotHeight = size.height - labelSpace
    val range = graphRange(distribution, shaded)
    val toX = { x: Double -> ((x - range.start) / (range.endInclusive - range.start) * size.width).toFloat() }

    if (distribution.isDiscrete) {
      drawBars(distribution, range, shaded, plotHeight, accent, muted, toX)
    } else {
      drawCurve(distribution, range, shaded, plotHeight, accent, toX)
    }
    drawLine(axis, Offset(0f, plotHeight), Offset(size.width, plotHeight), strokeWidth = 2f)
    handles.filter { it in range }.forEach { value ->
      val radius = HANDLE_RADIUS.toPx()
      drawLine(handleColor, Offset(toX(value), radius), Offset(toX(value), plotHeight), strokeWidth = HANDLE_WIDTH.toPx())
      drawCircle(handleColor, radius, Offset(toX(value).coerceIn(radius, size.width - radius), radius))
    }
    if (!showLabels) return@Canvas

    // Discrete: first and last whole values, not the margin around their bars
    val first = if (distribution.isDiscrete) ceil(range.start) else range.start
    val last = if (distribution.isDiscrete) floor(range.endInclusive) else range.endInclusive
    if (showEdgeLabels) {
      drawLabel(measurer, first.label(), toX(first), plotHeight, labelStyle)
      drawLabel(measurer, last.label(), toX(last), plotHeight, labelStyle)
    }
    shaded?.start?.takeIf { it.isFinite() && it in range }?.let {
      drawLabel(measurer, it.label(), toX(it), plotHeight, boundStyle)
    }
    shaded?.endInclusive?.takeIf { it.isFinite() && it in range && it != shaded.start }?.let {
      drawLabel(measurer, it.label(), toX(it), plotHeight, boundStyle)
    }
  }
}

/** Visible values, wider when the bounds are a bit outside of them. */
private fun graphRange(distribution: Distribution, shaded: ClosedRange<Double>?): ClosedRange<Double> {
  val visible = distribution.visibleRange()
  val width = visible.endInclusive - visible.start
  var start = visible.start
  var end = visible.endInclusive
  listOfNotNull(shaded?.start, shaded?.endInclusive)
    .filter { it.isFinite() && it >= visible.start - width && it <= visible.endInclusive + width }
    .forEach {
      start = min(start, it)
      end = max(end, it)
    }
  start = max(start, distribution.support.start)
  end = min(end, distribution.support.endInclusive)
  if (distribution.isDiscrete) {
    start = floor(start) - DISCRETE_MARGIN
    end = ceil(end) + DISCRETE_MARGIN
  }
  return start..end
}

private fun DrawScope.drawCurve(
  distribution: Distribution,
  range: ClosedRange<Double>,
  shaded: ClosedRange<Double>?,
  plotHeight: Float,
  color: Color,
  toX: (Double) -> Float,
) {
  val xs = (0..CURVE_SAMPLES).map { range.start + (range.endInclusive - range.start) * it / CURVE_SAMPLES }
  val ys = xs.map { distribution.density(it) }
  val maxY = densityScale(ys)
  val toY = { y: Double -> plotHeight - (min(y, maxY) / maxY * plotHeight * CURVE_FILL).toFloat() }

  if (shaded != null) {
    val area = Path()
    var started = false
    xs.zip(ys).filter { (x, _) -> x in shaded }.forEach { (x, y) ->
      if (!started) {
        area.moveTo(toX(x), plotHeight)
        started = true
      }
      area.lineTo(toX(x), toY(y))
    }
    if (started) {
      area.lineTo(toX(min(shaded.endInclusive, range.endInclusive)), plotHeight)
      area.close()
      drawPath(area, color.copy(alpha = SHADE_ALPHA))
    }
  }
  val curve = Path()
  xs.zip(ys).forEachIndexed { index, (x, y) ->
    if (index == 0) curve.moveTo(toX(x), toY(y)) else curve.lineTo(toX(x), toY(y))
  }
  drawPath(curve, color, style = Stroke(width = CURVE_WIDTH.toPx()))
}

private fun DrawScope.drawBars(
  distribution: Distribution,
  range: ClosedRange<Double>,
  shaded: ClosedRange<Double>?,
  plotHeight: Float,
  accent: Color,
  muted: Color,
  toX: (Double) -> Float,
) {
  val first = ceil(range.start).toLong()
  val last = floor(range.endInclusive).toLong()
  val step = max(1L, (last - first + 1) / MAX_BARS)
  val values = (first..last step step).map { it.toDouble() }
  val heights = values.map { distribution.density(it) }
  val maxY = heights.maxOrNull()?.takeIf { it > 0 } ?: return
  val barWidth = max(1f, (toX(1.0) - toX(0.0)) * step * BAR_FILL)
  values.zip(heights).forEach { (k, y) ->
    val height = (y / maxY * plotHeight * CURVE_FILL).toFloat()
    // Without a calculation every bar is highlighted
    val isShaded = shaded == null || (k >= ceil(shaded.start) && k <= floor(shaded.endInclusive))
    drawRect(
      color = if (isShaded) accent else muted.copy(alpha = MUTED_ALPHA),
      topLeft = Offset(toX(k) - barWidth / 2, plotHeight - height),
      size = Size(barWidth, height),
    )
  }
}

/** Highest density to show: spikes like χ² near 0 are cut so the rest stays readable. */
private fun densityScale(ys: List<Double>): Double {
  val finite = ys.filter { it.isFinite() && it > 0 }.sorted()
  if (finite.isEmpty()) return 1.0
  val top = finite.last()
  val high = finite[(finite.size * SPIKE_PERCENTILE).toInt().coerceAtMost(finite.size - 1)]
  return if (top > high * SPIKE_RATIO) high * SPIKE_RATIO else top
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, x: Float, top: Float, style: TextStyle) {
  val layout = measurer.measure(text, style)
  val left = (x - layout.size.width / 2).coerceIn(0f, size.width - layout.size.width)
  drawText(layout, topLeft = Offset(left, top + LABEL_GAP.toPx()))
}

/** Short label for the graph: 1.96, −3. */
private fun Double.label(): String = rounded(LABEL_DECIMALS)

private fun Double.format(format: NumberFormat, fallback: String): String {
  if (!isFinite()) return fallback
  return KBigDecimal.valueOf(this)
    .toFormattedString(max(format.precision, MIN_PROBABILITY_DECIMALS), format.outputFormat)
    .replace("-", Token.Operator.MINUS)
    .formatExpression(format.formatterSymbols)
}

/** Short name for the selector: `X ≤ a`. */
private fun CalculationType.symbol(): String =
  when (this) {
    CalculationType.AT_MOST -> "X ≤ a"
    CalculationType.BETWEEN -> "a ≤ X ≤ b"
    CalculationType.AT_LEAST -> "X ≥ a"
    CalculationType.EXACTLY -> "X = a"
  }

private val CARD_SPACING = 8.dp
private val SLOT_MIN_WIDTH = 56.dp
private val SLOT_PADDING = 12.dp
private val SLOT_SHAPE = RoundedCornerShape(14.dp)
private val SLOT_BORDER = 2.dp
private const val PLACEHOLDER_ALPHA = 0.5f
private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val GRAPH_HEIGHT = 220.dp
private val PREVIEW_HEIGHT = 56.dp
private val BUTTON_HEIGHT = 56.dp
private val LIST_CARD_MIN_WIDTH = 150.dp
private val SCREEN_PADDING = 12.dp
private val WIDE_LAYOUT_WIDTH = 720.dp
private const val WIDE_BUTTON_WEIGHT = 1.5f
private val HANDLE_RADIUS = 8.dp
private val HANDLE_WIDTH = 2.dp
private val LABEL_SPACE = 22.dp
private val LABEL_GAP = 4.dp
private val CURVE_WIDTH = 2.5.dp
private const val CURVE_SAMPLES = 240
private const val CURVE_FILL = 0.92f
private const val SHADE_ALPHA = 0.35f
private const val MUTED_ALPHA = 0.6f
private const val BAR_FILL = 0.8f
private const val MAX_BARS = 200L
private const val DISCRETE_MARGIN = 0.5
private const val SPIKE_PERCENTILE = 0.9
private const val SPIKE_RATIO = 3.0
private const val LABEL_DECIMALS = 2
private const val MIN_PROBABILITY_DECIMALS = 4
