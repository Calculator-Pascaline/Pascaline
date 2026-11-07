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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.sadellie.unitto.core.common.PhysicalUnit
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.UnitMenuItem
import com.sadellie.unitto.core.common.physicalUnitNames
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.designsystem.icons.symbols.ActivityZone
import com.sadellie.unitto.core.designsystem.icons.symbols.ActivityZoneFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Airwave
import com.sadellie.unitto.core.designsystem.icons.symbols.Atom
import com.sadellie.unitto.core.designsystem.icons.symbols.Close
import com.sadellie.unitto.core.designsystem.icons.symbols.Compress
import com.sadellie.unitto.core.designsystem.icons.symbols.DeviceThermostat
import com.sadellie.unitto.core.designsystem.icons.symbols.DevicesOther
import com.sadellie.unitto.core.designsystem.icons.symbols.DiagonalLine
import com.sadellie.unitto.core.designsystem.icons.symbols.ElectricBolt
import com.sadellie.unitto.core.designsystem.icons.symbols.ElectricBoltFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Energy
import com.sadellie.unitto.core.designsystem.icons.symbols.FitnessCenter
import com.sadellie.unitto.core.designsystem.icons.symbols.Language
import com.sadellie.unitto.core.designsystem.icons.symbols.LineAxis
import com.sadellie.unitto.core.designsystem.icons.symbols.Power
import com.sadellie.unitto.core.designsystem.icons.symbols.PowerFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Schedule
import com.sadellie.unitto.core.designsystem.icons.symbols.ScheduleFill
import com.sadellie.unitto.core.designsystem.icons.symbols.SplitscreenBottom
import com.sadellie.unitto.core.designsystem.icons.symbols.Straighten
import com.sadellie.unitto.core.designsystem.icons.symbols.StraightenFill
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.icons.symbols.WaterMedium
import com.sadellie.unitto.core.designsystem.icons.symbols.Weight
import com.sadellie.unitto.core.designsystem.icons.symbols.WeightFill
import com.sadellie.unitto.core.designsystem.icons.symbols._123
import com.sadellie.unitto.core.ui.KeyPress
import com.sadellie.unitto.core.ui.LocalKeyAnimation
import com.sadellie.unitto.core.ui.animatePressedWeight
import com.sadellie.unitto.core.ui.keyHaptics
import com.sadellie.unitto.core.ui.rememberKeyPress
import com.sadellie.unitto.core.ui.trackKeyPress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.calculator_units_and_constants
import unitto.core.common.generated.resources.common_close
import unitto.core.common.generated.resources.keyboard_degree
import unitto.core.common.generated.resources.keyboard_radian

/**
 * Function key: [label] is shown, [token] is inserted. [inverse] is used instead when inverse switch
 * is on: cos ==> acos.
 */
private class FunctionKey(
  val label: String,
  val token: String,
  val inverse: FunctionKey? = null,
  val width: Float = 1f,
)

/** Not a key, switches trigonometric keys to their inverses. */
private val INVERSE_SWITCH = FunctionKey("inv", "", width = SWITCH_WIDTH)

/** Not a key, switches the angle mode. */
private val ANGLE_SWITCH = FunctionKey("", "", width = SWITCH_WIDTH)

/**
 * Function keys on pages that are swiped horizontally. Keys fill the page: fewer rows are taller,
 * fewer keys in a row are wider. Dots under the keys show current page.
 *
 * @param rowHeight Height of one row when page has [PAGE_ROWS] rows.
 * @param firstKeys Label and token of keys for the first pages, like terms of sequences.
 * @param showCalculus `false` hides the page of integrals, derivatives and sums.
 * @param spacing Space between keys of a row, [rowGap] between rows.
 */
@Composable
internal fun FunctionRows(
  modifier: Modifier,
  rowHeight: Dp,
  onAddTokenClick: (String) -> Unit,
  firstKeys: List<Pair<String, String>> = emptyList(),
  showCalculus: Boolean = true,
  spacing: Dp = KEY_SPACING,
  rowGap: Dp = KEY_SPACING,
) {
  val functionPages =
    remember(firstKeys, showCalculus) {
      val first =
        firstKeys.chunked(KEYS_PER_PAGE).map { page ->
          page.chunked(KEYS_PER_ROW).map { row -> row.map { (label, token) -> FunctionKey(label, token) } }
        }
      first + listOfNotNull(EXPONENTS_PAGE, TRIGONOMETRY_PAGE, CALCULUS_PAGE.takeIf { showCalculus })
    }
  val pagerState = rememberPagerState(pageCount = { functionPages.size })
  var inverse by rememberSaveable { mutableStateOf(false) }
  Column(modifier = modifier) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxWidth().height(rowHeight * PAGE_ROWS),
      pageSpacing = spacing,
    ) { page ->
      Column(modifier = Modifier.fillMaxSize()) {
        functionPages[page].forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = rowGap / 2),
            horizontalArrangement = Arrangement.spacedBy(spacing),
          ) {
            row.forEach { key ->
              // Wider and squashed while pressed, like keys of the keypad. Pressed right away:
              // clicks on pages that are swiped come too late for that
              val keyPress = rememberKeyPress()
              val growth by animatePressedWeight(keyPress.isPressed, key.width)
              val keyModifier = Modifier.weight(growth).fillMaxHeight().trackKeyPress(keyPress)
              if (key === INVERSE_SWITCH) {
                KeySwitch(keyModifier, keyPress, INVERSE_SWITCH.label, inverse) { inverse = it }
              } else if (key === ANGLE_SWITCH) {
                AngleSwitch(keyModifier, keyPress)
              } else {
                FunctionKeyButton(
                  modifier = keyModifier,
                  keyPress = keyPress,
                  key = key.inverse?.takeIf { inverse } ?: key,
                  onClick = onAddTokenClick,
                )
              }
            }
          }
        }
      }
    }
    PageDots(modifier = Modifier.fillMaxWidth().height(DOTS_HEIGHT), pagerState = pagerState)
  }
}

/**
 * Dots under pages, current one is colored. Clicking a dot opens its page: mouse can't swipe pages
 * in the web app.
 */
@Composable
internal fun PageDots(modifier: Modifier, pagerState: PagerState) {
  val scope = rememberCoroutineScope()
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(pagerState.pageCount) { index ->
      val color =
        if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DOT_ALPHA)
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .width(DOT_TARGET_WIDTH)
            .clip(CircleShape)
            .clickable { scope.launch { pagerState.animateScrollToPage(index) } },
        contentAlignment = Alignment.Center,
      ) {
        Box(Modifier.size(DOT_SIZE).clip(CircleShape).background(color))
      }
    }
  }
}

@Composable
private fun FunctionKeyButton(
  modifier: Modifier,
  keyPress: KeyPress,
  key: FunctionKey,
  onClick: (String) -> Unit,
) {
  // Vibrates like the icon keys
  Surface(
    modifier = modifier.keyHaptics(keyPress),
    onClick = { onClick(key.token) },
    shape = squashedKeyShape(keyPress.isPressed),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      val hasSlots = SLOT in key.label
      Text(
        text = if (hasSlots) key.label.withSlotKeys() else key.label.withSuperscript(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        inlineContent = if (hasSlots) slotKeyContent() else emptyMap(),
      )
    }
  }
}

/** Switch for the angle mode of sin, cos and tan and of every tool: on is rad, off is deg. */
@Composable
private fun AngleSwitch(modifier: Modifier, keyPress: KeyPress) {
  val userPrefsRepository = koinInject<UserPreferencesRepository>()
  val radianMode by
    remember(userPrefsRepository) { userPrefsRepository.calculatorPrefs.map { it.radianMode } }
      .collectAsState(initial = true)
  val scope = rememberCoroutineScope()
  val name = stringResource(if (radianMode) Res.string.keyboard_radian else Res.string.keyboard_degree)
  KeySwitch(
    modifier = modifier.semantics { contentDescription = name },
    keyPress = keyPress,
    label = if (radianMode) ANGLE_LABEL_RADIANS else ANGLE_LABEL_DEGREES,
    checked = radianMode,
    onCheckedChange = { scope.launch { userPrefsRepository.updateRadianMode(it) } },
  )
}

/** A key that stays on or off, like the inverse functions and the angle mode. */
@Composable
private fun KeySwitch(
  modifier: Modifier,
  keyPress: KeyPress,
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Surface(
    modifier = modifier.keyHaptics(keyPress),
    checked = checked,
    onCheckedChange = onCheckedChange,
    shape = squashedKeyShape(keyPress.isPressed),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = label, style = MaterialTheme.typography.titleLarge, maxLines = 1)
      Spacer(Modifier.width(6.dp))
      Switch(checked = checked, onCheckedChange = null)
    }
  }
}

/**
 * Label with what is after [SUPERSCRIPT] small and raised: `e^x` ==> eˣ. Drawn as a real
 * superscript, the ˣ character is missing from fonts on the web.
 */
private fun String.withSuperscript(): AnnotatedString {
  val (base, exponent) = split(SUPERSCRIPT, limit = 2).let { it.first() to it.getOrNull(1) }
  return buildAnnotatedString {
    append(base)
    if (exponent != null) {
      // Digits are taller than letters like e: the exponent goes higher after them
      val shift = if (base.lastOrNull()?.isDigit() == true) SUPERSCRIPT_SHIFT_DIGIT else SUPERSCRIPT_SHIFT
      withStyle(SpanStyle(fontSize = SUPERSCRIPT_SIZE.em, baselineShift = BaselineShift(shift))) {
        append(exponent)
      }
    }
  }
}

/** Empty places to fill in, drawn as the same soft keys as in the expression. */
private fun String.withSlotKeys(): AnnotatedString = buildAnnotatedString {
  split(SLOT).forEachIndexed { index, part ->
    if (index > 0) appendInlineContent(SLOT_KEY, SLOT.toString())
    append(part)
  }
}

@Composable
private fun slotKeyContent(): Map<String, InlineTextContent> {
  val color = LocalContentColor.current.copy(alpha = SLOT_ALPHA)
  return mapOf(
    SLOT_KEY to
      InlineTextContent(Placeholder(SLOT_WIDTH.em, SLOT_HEIGHT.em, PlaceholderVerticalAlign.TextCenter)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(SLOT_CORNER)).background(color))
      }
  )
}

private const val SLOT = '□'
private const val SLOT_KEY = "slot"
private const val SLOT_ALPHA = 0.2f
private const val SLOT_WIDTH = 0.75f
private const val SLOT_HEIGHT = 0.9f
private const val SLOT_CORNER = 30
private const val SUPERSCRIPT = '^'
private const val SUPERSCRIPT_SIZE = 0.75f
/** About as high as ² on the x² key, Superscript (0.5) is too low */
private const val SUPERSCRIPT_SHIFT = 0.7f
private const val SUPERSCRIPT_SHIFT_DIGIT = 1f

private fun function(name: String) = FunctionKey(name, "$name(")

/** cos, and acos when inverse switch is on. */
private fun trig(name: String, inverseName: String) =
  FunctionKey(name, "$name(", inverse = function(inverseName))

// Exponents and roots
private val EXPONENTS_PAGE =
  listOf(
    listOf(
      FunctionKey("e${SUPERSCRIPT}x", Token.Layout.EXP),
      FunctionKey(Token.Func.LN, Token.Func.LN_BRACKET),
      FunctionKey(Token.Func.LOG, Token.Layout.LOG_10),
      FunctionKey("×10${SUPERSCRIPT}x", Token.Operator.MULTIPLY + Token.Layout.POWER_10),
    ),
    listOf(
      FunctionKey(Token.Operator.SQRT, Token.Layout.SQRT),
      FunctionKey("x²", Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START + "2" + Token.Layout.END),
    ),
  )

// Trigonometry, inverse functions with the switch
private val TRIGONOMETRY_PAGE =
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
      ANGLE_SWITCH,
    ),
  )

// Calculus. Integral without bounds is indefinite
private val CALCULUS_PAGE =
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
  )

/**
 * Units and constants, same menus as on NumWorks: every category is a tile, across the top of a
 * screen wider than tall and down the side otherwise, and the units of the chosen one are shown
 * next to the tiles, grouped under the names of its sub-menus.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun UnitsSheet(onDismiss: () -> Unit, onUnitClick: (String) -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val categories = remember { PhysicalUnits.menu.filterIsInstance<UnitMenuItem.Category>() }
  // Opens on the category it was left on, read before the first frame to not flash another one
  val userPrefsRepository = koinInject<UserPreferencesRepository>()
  val savedCategory =
    remember { userPrefsRepository.appData(UNITS_CATEGORY_KEY) }.collectAsState(null).value ?: return
  val selectedIndex = (savedCategory.toIntOrNull() ?: 0).coerceIn(categories.indices)
  val selected = categories[selectedIndex]

  val scope = rememberCoroutineScope()
  // Hides with the sheet's animation, then closes
  val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }
  // Always the whole screen, also wider than a sheet usually is: room for every unit. Closed with
  // its X (or back), not by sliding it down
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    sheetMaxWidth = Dp.Unspecified,
    sheetGesturesEnabled = false,
    dragHandle = null,
    // The whole page takes the chosen category's color
    containerColor = animateColorAsState(categoryPageColor(selected.name), label = "Units page").value,
  ) {
    // In landscape the navigation bar is on a side, and would cover the X
    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
          .padding(top = 8.dp)
    ) {
      Row(
        modifier = Modifier.padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(Res.string.calculator_units_and_constants),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = close) {
          Icon(Symbols.Close, contentDescription = stringResource(Res.string.common_close))
        }
      }
      val tiles: @Composable (Boolean) -> Unit = { isHorizontal ->
        categories.forEachIndexed { index, category ->
          CategoryTile(
            category = category,
            selected = index == selectedIndex,
            isHorizontal = isHorizontal,
            onClick = { scope.launch { userPrefsRepository.updateAppData(UNITS_CATEGORY_KEY, index.toString()) } },
          )
        }
      }
      // By the shape of the screen: categories across the top when it is wider than tall (a
      // laptop), down the left edge otherwise (a phone). They scroll apart from the units
      BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        if (maxWidth > maxHeight) {
          Column(modifier = Modifier.fillMaxSize()) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              tiles(true)
            }
            CategoryUnits(selected, Modifier.padding(start = 16.dp), onUnitClick)
          }
        } else {
          Row(modifier = Modifier.fillMaxSize()) {
            Column(
              modifier =
                Modifier.fillMaxHeight()
                  .verticalScroll(rememberScrollState())
                  .padding(start = 16.dp, end = 12.dp, bottom = 24.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              tiles(false)
            }
            Column(modifier = Modifier.weight(1f)) {
              CategoryName(categories, selectedIndex)
              CategoryUnits(selected, Modifier, onUnitClick)
            }
          }
        }
      }
    }
  }
}

/** Name of the chosen category above its units, sliding the way the selection moved. */
@Composable
private fun CategoryName(categories: List<UnitMenuItem.Category>, selectedIndex: Int) {
  AnimatedContent(
    targetState = selectedIndex,
    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
    transitionSpec = {
      val direction = if (targetState > initialState) 1 else -1
      val slide = spring<IntOffset>(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
      (slideInVertically(slide) { direction * it / 2 } + fadeIn(tween(CATEGORY_FADE_IN_MS))) togetherWith
        (slideOutVertically(slide) { -direction * it / 2 } + fadeOut(tween(CATEGORY_FADE_OUT_MS)))
    },
    contentAlignment = Alignment.CenterStart,
    label = "Category name",
  ) { index ->
    val category = categories[index]
    Text(
      text = menuName(category.name),
      style = MaterialTheme.typography.titleLarge,
      color = categoryColor(category.name),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/** Units of the chosen category, scrolling on their own and fading from one category to another. */
@Composable
private fun CategoryUnits(
  selected: UnitMenuItem.Category,
  modifier: Modifier,
  onUnitClick: (String) -> Unit,
) {
  AnimatedContent(
    targetState = selected,
    modifier = modifier.fillMaxHeight(),
    transitionSpec = {
      fadeIn(tween(CATEGORY_FADE_IN_MS)) togetherWith fadeOut(tween(CATEGORY_FADE_OUT_MS))
    },
    label = "Category units",
  ) { category ->
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(end = 16.dp, bottom = 24.dp)
    ) {
      unitSections(category).forEach { (title, units) ->
        UnitSection(
          title = title,
          units = units,
          color = categoryColor(category.name),
          chipColor = categoryChipColor(category.name),
          onUnitClick = onUnitClick,
        )
      }
    }
  }
}

/**
 * Name of a category, unit or constant of the units menu in the app's language. The menu's names
 * are in English: they stay the keys, an unknown one is shown as it is.
 */
@Composable
private fun menuName(name: String): String = physicalUnitNames[name]?.let { stringResource(it) } ?: name

/** Between the names of a sub-menu and its own sub-menu: "Energy · Joule". */
private const val SECTION_SEPARATOR = " · "

/**
 * Units of [category] in the menu's order: its own units, then each sub-menu's under its name.
 * Deeper sub-menus are named after both: "Energy · Joule".
 */
private fun unitSections(category: UnitMenuItem.Category): List<Pair<String, List<PhysicalUnit>>> {
  val sections = mutableListOf<Pair<String, List<PhysicalUnit>>>()
  fun walk(menu: UnitMenuItem.Category, title: String) {
    val units = menu.children.filterIsInstance<UnitMenuItem.Leaf>().map { it.unit }
    if (units.isNotEmpty()) sections += title to units
    menu.children.filterIsInstance<UnitMenuItem.Category>().forEach { child ->
      // A first level of sub-menus is named alone, Length rather than Length and angle · Length
      walk(child, if (menu === category) child.name else title + SECTION_SEPARATOR + child.name)
    }
  }
  walk(category, category.name)
  return sections
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitSection(
  title: String,
  units: List<PhysicalUnit>,
  color: Color,
  chipColor: Color,
  onUnitClick: (String) -> Unit,
) {
  Row(
    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    categoryIcon(title.substringAfterLast(SECTION_SEPARATOR))?.let { icon ->
      Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(18.dp),
      )
      Spacer(Modifier.width(8.dp))
    }
    Text(
      text = title.split(SECTION_SEPARATOR).map { menuName(it) }.joinToString(SECTION_SEPARATOR),
      style = MaterialTheme.typography.labelLarge,
      color = color,
    )
  }
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    units.forEach { unit -> UnitChip(unit = unit, color = chipColor, onClick = { onUnitClick(unit.token) }) }
  }
}

/**
 * Category of the units menu as a colored tile, like an app icon: soft tint with the icon in the
 * category's color. Chosen, it fills with that color. When the tiles are [isHorizontal] it also
 * grows to show its name; otherwise [CategoryName] shows it and the tiles stay where they are.
 */
@Composable
private fun CategoryTile(
  category: UnitMenuItem.Category,
  selected: Boolean,
  isHorizontal: Boolean,
  onClick: () -> Unit,
) {
  val color = categoryColor(category.name)
  val name = menuName(category.name)
  val background by
    animateColorAsState(if (selected) color else color.copy(alpha = CATEGORY_TINT_ALPHA), label = "Tile")
  val content by animateColorAsState(if (selected) Color.White else color, label = "Tile icon")
  val iconScale by
    animateFloatAsState(
      if (selected) CATEGORY_ICON_SELECTED_SCALE else 1f,
      spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
      label = "Tile icon scale",
    )
  // The tile that just grew may stick out of the list
  val bringIntoView = remember { BringIntoViewRequester() }
  LaunchedEffect(selected) {
    if (selected) {
      delay(CATEGORY_GROW_MS)
      bringIntoView.bringIntoView()
    }
  }
  val icon: @Composable () -> Unit = {
    Box(modifier = Modifier.size(CATEGORY_TILE_SIZE), contentAlignment = Alignment.Center) {
      categoryTileIcon(category.name)?.let {
        Icon(
          it,
          contentDescription = null,
          modifier = Modifier.size(CATEGORY_TILE_ICON_SIZE).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
        )
      }
    }
  }
  val keyPress = rememberKeyPress()
  Surface(
    onClick = onClick,
    modifier =
      Modifier.bringIntoViewRequester(bringIntoView)
        .semantics { contentDescription = name }
        .trackKeyPress(keyPress)
        .keyHaptics(keyPress),
    shape = RoundedCornerShape(CATEGORY_TILE_CORNER),
    color = background,
    contentColor = content,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      icon()
      AnimatedVisibility(
        visible = selected && isHorizontal,
        enter = expandHorizontally(tileSpring()) + fadeIn(),
        exit = shrinkHorizontally(tileSpring()) + fadeOut(),
      ) {
        Text(
          text = name,
          modifier = Modifier.padding(end = 18.dp),
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

private fun tileSpring() = spring<IntSize>(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)

/** Own color of each category, the same in light and dark themes: strong enough on both. */
private fun categoryColor(name: String): Color =
  when (name) {
    "Length and angle" -> Color(color = 0xFF5C6BC0)
    "Time and frequency" -> Color(color = 0xFF00897B)
    "Area and volume" -> Color(color = 0xFF039BE5)
    "Mass" -> Color(color = 0xFFEF6C00)
    "Electricity" -> Color(color = 0xFFF9A825)
    "Force and pressure" -> Color(color = 0xFFE53935)
    "Energy and power" -> Color(color = 0xFF43A047)
    "Temperature" -> Color(color = 0xFFD81B60)
    "Other units" -> Color(color = 0xFF8E24AA)
    "Constants" -> Color(color = 0xFF546E7A)
    else -> Color(color = 0xFF5C6BC0)
  }

/** Background of the menu: the sheet's color with a light wash of the category's, fading over. */
@Composable
private fun categoryPageColor(name: String): Color {
  val target =
    categoryColor(name).copy(alpha = CATEGORY_PAGE_ALPHA).compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
  return animateColorAsState(target, label = "Units page").value
}

/** Units on that page: a stronger wash, so they stand out from it. */
@Composable
private fun categoryChipColor(name: String): Color {
  val target =
    categoryColor(name).copy(alpha = CATEGORY_CHIP_ALPHA).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
  return animateColorAsState(target, label = "Unit chips").value
}

/** Filled icons on the tiles, they read better at this size than the outlined ones. */
private fun categoryTileIcon(name: String): ImageVector? =
  when (name) {
    "Length and angle" -> Symbols.StraightenFill
    "Time and frequency" -> Symbols.ScheduleFill
    "Area and volume" -> Symbols.ActivityZoneFill
    "Mass" -> Symbols.WeightFill
    "Electricity" -> Symbols.ElectricBoltFill
    "Energy and power" -> Symbols.PowerFill
    else -> categoryIcon(name)
  }

/** Symbol to insert, with its name below: hplanck alone doesn't say it's Planck's constant. */
@Composable
private fun UnitChip(unit: PhysicalUnit, color: Color, onClick: () -> Unit) {
  val keyPress = rememberKeyPress()
  Surface(
    onClick = onClick,
    modifier = Modifier.trackKeyPress(keyPress).keyHaptics(keyPress),
    shape = RoundedCornerShape(12.dp),
    color = color,
    contentColor = MaterialTheme.colorScheme.onSurface,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(text = unit.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
      Text(
        text = menuName(unit.name),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

/**
 * Icon in front of a category of the units menu, like the unit converter's groups. The menu's
 * names are fixed (not translated), null for a name that has none.
 */
private fun categoryIcon(name: String): ImageVector? = CATEGORY_ICONS[name]

private val CATEGORY_ICONS: Map<String, ImageVector> by lazy {
  mapOf(
    "Length and angle" to Symbols.Straighten,
    "Length" to Symbols.Straighten,
    "Angle" to Symbols.DiagonalLine,
    // Inches, ounces, gallons: the units of other countries
    "Imperial" to Symbols.Language,
    "Time and frequency" to Symbols.Schedule,
    "Time" to Symbols.Schedule,
    "Frequency" to Symbols.Airwave,
    "Area and volume" to Symbols.ActivityZone,
    "Area" to Symbols.ActivityZone,
    "Volume" to Symbols.WaterMedium,
    "Mass" to Symbols.Weight,
    "Electricity" to Symbols.ElectricBolt,
    "Current" to Symbols.ElectricBolt,
    "Voltage" to Symbols.ElectricBoltFill,
    // Zigzag, like a resistor
    "Resistance" to Symbols.LineAxis,
    // Two plates, like a capacitor
    "Capacitance" to Symbols.SplitscreenBottom,
    "Other units" to Symbols.DevicesOther,
    "Force and pressure" to Symbols.FitnessCenter,
    "Force" to Symbols.FitnessCenter,
    "Pressure" to Symbols.Compress,
    "Energy and power" to Symbols.Energy,
    "Energy" to Symbols.Energy,
    "Joule" to Symbols.Energy,
    "Electronvolt" to Symbols.Atom,
    "Watt-hour" to Symbols.Power,
    "Power" to Symbols.Power,
    "Temperature" to Symbols.DeviceThermostat,
    "Amount of substance" to Symbols.Atom,
    "Constants" to Symbols._123,
  )
}

private const val CATEGORY_ICON_SELECTED_SCALE = 1.15f
private const val UNITS_CATEGORY_KEY = "UNITS_CATEGORY"
private const val CATEGORY_GROW_MS = 250L
private const val CATEGORY_FADE_IN_MS = 220
private const val CATEGORY_FADE_OUT_MS = 90
private val CATEGORY_TILE_SIZE = 52.dp
private val CATEGORY_TILE_CORNER = 18.dp
private val CATEGORY_TILE_ICON_SIZE = 28.dp
private const val CATEGORY_TINT_ALPHA = 0.16f
private const val CATEGORY_PAGE_ALPHA = 0.08f
private const val CATEGORY_CHIP_ALPHA = 0.16f
private val KEY_SPACING = 6.dp
internal const val PAGE_ROWS = 4
private const val KEYS_PER_ROW = 4
private const val KEYS_PER_PAGE = 8
private const val DOT_ALPHA = 0.3f
private val DOT_SIZE = 6.dp
/** Width that can be clicked around a dot */
private val DOT_TARGET_WIDTH = 16.dp
internal val DOTS_HEIGHT = 14.dp

/** Same corners as the other keys. */
/** Corners of keys like on the keypad: a bit less round while pressed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun squashedKeyShape(isPressed: Boolean): RoundedCornerShape {
  val corner by
    animateIntAsState(
      targetValue = if (isPressed && LocalKeyAnimation.current) KEY_PRESSED_CORNER else KEY_CORNER,
      animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
      label = "Squashed key",
    )
  return RoundedCornerShape(percent = corner)
}

private const val SWITCH_WIDTH = 1.45f
private const val ANGLE_LABEL_DEGREES = "deg"
private const val ANGLE_LABEL_RADIANS = "rad"
private const val KEY_CORNER = 36
private const val KEY_PRESSED_CORNER = 24
