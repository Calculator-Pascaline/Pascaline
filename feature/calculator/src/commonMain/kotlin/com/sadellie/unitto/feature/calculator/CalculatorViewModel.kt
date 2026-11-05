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

package com.sadellie.unitto.feature.calculator

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KBigDecimalMath
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isExpression
import com.sadellie.unitto.core.common.isGreaterThan
import com.sadellie.unitto.core.common.isScientific
import com.sadellie.unitto.core.common.scientificMantissa
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.data.calculator.CalculatorHistoryRepository
import com.sadellie.unitto.core.datastore.CalculatorPreferences
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import com.sadellie.unitto.core.ui.textfield.ANGLE_BOX_FUNCTIONS
import com.sadellie.unitto.core.ui.textfield.addBracket
import com.sadellie.unitto.core.ui.textfield.addFunctionWithAngleBox
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.chooseAngleUnit
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.sadellie.unitto.core.ui.textfield.getTextFieldState
import com.sadellie.unitto.core.ui.textfield.observe
import com.sadellie.unitto.core.ui.textfield.placeCursorAtTheEnd
import io.github.sadellie.evaluatto.ExactFraction
import io.github.sadellie.evaluatto.Expression
import io.github.sadellie.evaluatto.ExpressionException
import io.github.sadellie.evaluatto.ExpressionResult
import io.github.sadellie.evaluatto.UnitValue
import io.github.sadellie.evaluatto.integerInfo
import io.github.sadellie.evaluatto.isShownExactly
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val COMPLEX_INFO_PRECISION = 50

/** Indefinite integral is entered with empty bounds, history shows it without them: ∫cos(x)dx */
private const val EMPTY_INTEGRAL_BOUNDS =
  Token.Calculus.INTEGRAL +
    Token.Layout.SUBSCRIPT_START +
    Token.Layout.END +
    Token.Layout.SUPERSCRIPT_START +
    Token.Layout.END
/** Value as [toScientific] shows it: rounded to [precision] digits, of the mantissa for powers of ten. */
private fun KBigDecimal.shownValue(precision: Int): KBigDecimal {
  if (!isScientific()) return KBigDecimal(toFormattedString(precision, OutputFormat.PLAIN))
  val (mantissa, exponent) = scientificMantissa()
  return KBigDecimal(mantissa.toFormattedString(precision, OutputFormat.PLAIN)).multiply(KBigDecimal("1E$exponent"))
}

/** ≈ when the shown value is rounded: 20÷4500 ==> ≈0.004, 55! ==> ≈1.27×10⁷³, but 1÷8 ==> 0.125. */
private fun KBigDecimal.roundingSign(precision: Int, fraction: ExactFraction? = null): String =
  if (isShownExactly(this, fraction, shownValue(precision))) "" else APPROXIMATELY

/** 5.5×10¹⁶ (layout slots) when value is too big or too small to read, plain number otherwise. */
private fun KBigDecimal.toScientific(precision: Int, outputFormat: Int): String {
  if (!isScientific()) return toFormattedString(precision, outputFormat).replace("-", Token.Operator.MINUS)

  val (mantissa, exponent) = scientificMantissa()
  return mantissa.toFormattedString(precision, OutputFormat.PLAIN).replace("-", Token.Operator.MINUS) +
    Token.Operator.MULTIPLY +
    "10" +
    Token.Operator.POWER +
    Token.Layout.SUPERSCRIPT_START +
    exponent.toString().replace("-", Token.Operator.MINUS) +
    Token.Layout.END
}
private const val DEGREES_IN_HALF_TURN = 180.0

/** Tokens of what calculates many points. */
private val CALCULUS_TOKENS = listOf(Token.Calculus.INTEGRAL, Token.Calculus.SUM, Token.Calculus.PRODUCT)

/** What a calculated result depends on. */
private data class EvaluationKey(
  val input: String,
  val radianMode: Boolean,
  val precision: Int,
  val outputFormat: Int,
)

internal class CalculatorViewModel(
  private val userPrefsRepository: UserPreferencesRepository,
  private val calculatorHistoryRepository: CalculatorHistoryRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private var _calculationJob: Job? = null
  private var _equalJob: Job? = null
  /** Result of the preview, equal doesn't calculate it again. */
  private var _lastEvaluated: Pair<EvaluationKey, ExpressionResult>? = null
  private val _isCalculating = MutableStateFlow(false)
  private val _inputKey = "CALCULATOR_INPUT"
  private val _input = savedStateHandle.getTextFieldState(_inputKey)
  private val _result = MutableStateFlow<CalculationResult>(CalculationResult.Empty)
  private val _equalClicked = MutableStateFlow(false)
  private val _prefs = userPrefsRepository.calculatorPrefs.stateIn(viewModelScope, null)
  /** Calculation that equal will change instead of adding a new one. */
  private val _editing = MutableStateFlow<CalculatorHistoryItem?>(null)
  /** Results of history in the input, equal finds out where they ended up. */
  private val _pendingReuses = mutableListOf<PendingReuse>()

  val uiState: StateFlow<CalculatorUIState> =
    combine(
        _result,
        _prefs,
        calculatorHistoryRepository.historyFlow,
        _editing,
        _isCalculating,
      ) { result, prefs, history, editing, isCalculating ->
        prefs ?: return@combine CalculatorUIState.Loading

        return@combine CalculatorUIState.Ready(
          input = _input,
          output = result,
          radianMode = prefs.radianMode,
          precision = prefs.precision,
          outputFormat = prefs.outputFormat,
          formatterSymbols = prefs.formatterSymbols,
          history = history,
          additionalButtons = prefs.additionalButtons,
          inverseMode = prefs.inverseMode,
          partialHistoryView = prefs.partialHistoryView,
          steppedPartialHistoryView = prefs.steppedPartialHistoryView,
          initialPartialHistoryView = prefs.initialPartialHistoryView,
          openHistoryViewButton = prefs.openHistoryViewButton,
          isEditing = editing != null,
          isCalculating = isCalculating,
        )
      }
      .stateIn(viewModelScope, CalculatorUIState.Loading)

  suspend fun observeInput() {
    _input.observe().collectLatest {
      // Do not process input if equal was clicked to keep fractional output
      if (_equalClicked.value) {
        _equalClicked.update { false }
        return@collectLatest
      }
      savedStateHandle[_inputKey] = it.toString()
      calculateInput()
    }
  }

  fun addTokens(tokens: String) {
    if (_isCalculating.value) return
    when {
      // Equal was clicked and user tries to type a digit or dot
      _equalClicked.value && tokens in Token.Digit.allWithDot -> _input.clearText()
      // Equal was clicked and user tries to add operator or something
      _equalClicked.value -> _input.placeCursorAtTheEnd()
    }
    // Operator on empty input works on previous result: 12 in history, "+" ==> 12+
    if (_input.text.isEmpty()) {
      val last = (uiState.value as? CalculatorUIState.Ready)?.history?.firstOrNull()
      val operand = last?.let { HistoryResult.decode(it.result).operandFor(tokens) }
      if (last != null && operand != null) {
        _input.addTokens(operand)
        rememberReuse(last, operand, position = 0)
      }
    }
    if (tokens in ANGLE_BOX_FUNCTIONS) {
      // sin(|deg): angle unit box in the angle mode, can be switched by tapping it
      _input.addFunctionWithAngleBox(tokens, radians = _prefs.value?.radianMode ?: true)
    } else {
      _input.addTokens(tokens)
      _input.chooseAngleUnit(tokens)
    }
    _equalClicked.update { false }
  }

  /** Result tapped in history: changes when its calculation is edited. */
  fun addHistoryResult(item: CalculatorHistoryItem, result: String) {
    if (_isCalculating.value) return
    addTokens(result)
    rememberReuse(item, result, position = _input.selection.max - result.length)
  }

  // A calculation can only build on earlier ones, otherwise editing it would never end
  private fun rememberReuse(source: CalculatorHistoryItem, text: String, position: Int) {
    val editing = _editing.value
    if (editing == null || source.timestamp < editing.timestamp) {
      _pendingReuses += PendingReuse(source.id, text, position)
    }
  }

  fun addBracket() {
    if (_isCalculating.value) return
    if (_equalClicked.value) {
      // Cursor is set to 0 when equal is clicked
      _input.placeCursorAtTheEnd()
    }
    _input.addBracket()
    _equalClicked.update { false }
  }

  fun deleteTokens() {
    if (_isCalculating.value) return
    if (_equalClicked.value) {
      _input.clearText()
    } else {
      _input.deleteTokens()
    }
    _equalClicked.update { false }
  }

  /** Also stops a calculation that takes too long. */
  fun clearInput() {
    _equalJob?.cancel()
    _input.clearText()
    _pendingReuses.clear()
    _equalClicked.update { false }
  }

  /** Puts [item] in the input, equal then changes it instead of adding a new calculation. */
  fun editHistoryItem(item: CalculatorHistoryItem) {
    if (_isCalculating.value) return
    _pendingReuses.clear()
    item.reused
      .filter { it.end <= item.expression.length }
      .forEach { _pendingReuses += PendingReuse(it.sourceId, item.expression.substring(it.start, it.end), it.start) }
    _input.setTextAndPlaceCursorAtEnd(item.expression)
    _editing.update { item }
    _equalClicked.update { false }
  }

  fun cancelEdit() {
    _editing.update { null }
    clearInput()
  }

  fun updateInitialPartialHistoryView(newValue: Boolean) =
    viewModelScope.launch { userPrefsRepository.updateInitialPartialHistoryView(newValue) }

  fun clearHistory() = viewModelScope.launch { calculatorHistoryRepository.clear() }

  fun deleteHistoryItem(item: CalculatorHistoryItem) =
    viewModelScope.launch { calculatorHistoryRepository.delete(item.id) }

  /**
   * Adds calculation to history (errors too, with empty result) and clears input, like NumWorks.
   * Result is saved as lines: result, then fraction and roots if there are any. A calculation that
   * is being edited changes in place, with everything that reused its result.
   */
  fun onEqualClick() {
    if (_isCalculating.value) return
    _equalJob =
      viewModelScope.launch {
        _isCalculating.update { true }
        try {
          saveInput()
        } finally {
          _isCalculating.update { false }
        }
      }
  }

  private suspend fun saveInput() {
    val prefs = _prefs.value ?: return
    var inputValue = _input.text.toString()
    // Equal again repeats last operation on the result: 2+3=5, = ==> 5+3=8
    val editing = _editing.value
    if (inputValue.isBlank() && prefs.constantCalculation && editing == null) {
      inputValue = repeatLastOperation().orEmpty()
    }
    if (inputValue.isBlank()) return

    // The preview of this input may be calculating it right now
    _calculationJob?.join()
    val expression = inputValue.replace(EMPTY_INTEGRAL_BOUNDS, Token.Calculus.INTEGRAL)
    val reused = locateReused(expression, _pendingReuses)
    val result = historyResult(inputValue, prefs)
    // An edit is finished even if AC is pressed, or only some calculations would follow it
    withContext(NonCancellable) {
      // It can be deleted while it is edited
      val isInHistory = calculatorHistoryRepository.historyFlow.first().any { it.id == editing?.id }
      if (editing != null && isInHistory) {
        calculatorHistoryRepository.update(editing.id, expression, result, reused)
        updateCalculationsAfter(editing.id, prefs)
      } else {
        calculatorHistoryRepository.add(expression, result, reused)
      }
    }

    _editing.update { null }
    _pendingReuses.clear()
    _equalClicked.update { false }
    _input.clearText()
    _result.update { CalculationResult.Empty }
  }

  /** [HistoryResult] of [input] ready to be saved, one without lines for an error. */
  private suspend fun historyResult(input: String, prefs: CalculatorPreferences): String {
    val key = EvaluationKey(input, prefs.radianMode, prefs.precision, prefs.outputFormat)
    val evaluated =
      try {
        _lastEvaluated?.takeIf { it.first == key }?.second
          ?: evaluate(input, prefs.radianMode, prefs.precision, prefs.outputFormat)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        null
      }
    val lines = if (evaluated == null) emptyList() else resultLines(evaluated, input, prefs)
    return HistoryResult(lines = lines, menu = evaluated?.menuSections() ?: emptyList()).encode()
  }

  /**
   * Oldest first, so that a calculation that reused one that reused [editedId] gets the new result.
   * What reused an error keeps its old value: there is nothing to put in its place.
   */
  private suspend fun updateCalculationsAfter(editedId: Int, prefs: CalculatorPreferences) {
    val history = calculatorHistoryRepository.historyFlow.first().sortedBy { it.timestamp }
    val results = history.associate { it.id to HistoryResult.decode(it.result).insertable }.toMutableMap()
    val changed = mutableSetOf(editedId)
    history.forEach { item ->
      val operands = changed.mapNotNull { id -> results[id]?.let { id to it } }.toMap()
      val rewritten = rewriteReused(item.expression, item.reused, operands) ?: return@forEach
      val result = historyResult(rewritten.expression, prefs)
      calculatorHistoryRepository.update(item.id, rewritten.expression, result, rewritten.reused)
      results[item.id] = HistoryResult.decode(result).insertable
      changed += item.id
    }
  }

  /** Lines of a result: exact value with square roots, approximation, then the fraction. */
  private suspend fun resultLines(
    evaluated: ExpressionResult,
    inputValue: String,
    prefs: CalculatorPreferences,
  ): List<String> {
    val lines = mutableListOf<String>()
    // Exact value with square roots first, then approximation: √5i, ≈2.236i
    val exact =
      when (evaluated) {
        is ExpressionResult.Real -> evaluated.exact
        is ExpressionResult.Complex -> evaluated.exact
        is ExpressionResult.Symbolic -> null
      }
    if (exact != null) lines += exact
    val approximation =
      evaluated
        .format(prefs.precision, prefs.outputFormat)
        // replace minus symbols since it is not recognized by evaluatto
        .replace("-", Token.Operator.MINUS)
    lines += if (exact != null) APPROXIMATELY + approximation.removePrefix(APPROXIMATELY) else approximation
    // Complex numbers and expressions with x can't be fractions
    if (prefs.fractionalOutput && evaluated is ExpressionResult.Real) {
      val fractional = fraction(evaluated, inputValue, prefs)
      if (fractional.isNotEmpty()) lines += fractional.toFractionSlots()
    }
    return lines
  }

  /** `1 2⁄3`, empty when the result is not a fraction. */
  private suspend fun fraction(evaluated: ExpressionResult.Real, inputValue: String, prefs: CalculatorPreferences): String {
    // Exact fraction when the result is rational: 1÷3×3 is 1, not 0.999… as 1⁄1
    val exactFraction = evaluated.fraction
    return when {
      exactFraction != null -> exactFraction.toFractionalString()
      // Integrals and series take too long to be calculated twice
      CALCULUS_TOKENS.any { it in inputValue } -> evaluated.value.toFractionalString()
      else ->
        try {
          // Different rounding mode to properly calculate fractional
          calculate(inputValue, prefs.radianMode, KRoundingMode.DOWN).toFractionalString()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          ""
        }
    }
  }

  /** Previous result with the operation of previous expression: 5+3 for 2+3=5. */
  private fun repeatLastOperation(): String? {
    val last = (uiState.value as? CalculatorUIState.Ready)?.history?.firstOrNull() ?: return null
    val operation = last.expression.repeatableOperation() ?: return null
    val operand = HistoryResult.decode(last.result).operandFor(operation.take(1)) ?: return null
    rememberReuse(last, operand, position = 0)
    return operand + operation
  }

  private fun calculateInput() {
    _calculationJob?.cancel()
    _calculationJob =
      viewModelScope.launch {
        if (!_input.text.toString().isExpression()) {
          _result.update { CalculationResult.Empty }
          return@launch
        }

        val prefs = _prefs.value ?: return@launch
        val input = _input.text.toString()
        val newResult =
          try {
            val evaluated =
              evaluate(
                input = input,
                radianMode = prefs.radianMode,
                precision = prefs.precision,
                outputFormat = prefs.outputFormat,
              )
            _lastEvaluated =
              EvaluationKey(input, prefs.radianMode, prefs.precision, prefs.outputFormat) to evaluated
            CalculationResult.Success(evaluated.format(prefs.precision, prefs.outputFormat))
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            CalculationResult.Empty
          }
        _result.update { newResult }
      }
  }

  private suspend fun calculate(
    input: String,
    radianMode: Boolean,
    roundingMode: KRoundingMode = KRoundingMode.HALF_EVEN,
  ): KBigDecimal =
    withContext(Dispatchers.Default) {
      Expression(input, radianMode, roundingMode, onProgress = { ensureActive() }).calculate().also {
        if (it.isGreaterThan(maxCalculationResult)) throw ExpressionException.TooBig()
      }
    }

  /**
   * "…" menu: factorization, roots and discriminant for polynomials. Other forms and prime factors
   * for whole numbers.
   */
  private fun ExpressionResult.menuSections(): List<Pair<MenuSection, List<String>>> =
    when (this) {
      is ExpressionResult.Symbolic ->
        listOf(
          MenuSection.FACTORIZATION to listOfNotNull(factorization),
          MenuSection.ROOTS to roots,
          MenuSection.DISCRIMINANT to listOfNotNull(discriminant),
        )
      is ExpressionResult.Real if si != null || otherUnits.isNotEmpty() -> {
        val prefs = _prefs.value ?: return emptyList()
        fun UnitValue.line() =
          value.roundingSign(prefs.precision) + value.toScientific(prefs.precision, prefs.outputFormat) + unit
        buildList {
          if (otherUnits.isNotEmpty()) {
            add(MenuSection.OTHER_UNITS to otherUnits.map { it.line() })
          }
          // 300 K in SI is 300 K again, nothing new to show
          val siValue = si?.takeIf { it.unit != unit || baseUnits != null }
          if (siValue != null) {
            val value =
              siValue.value.roundingSign(prefs.precision) +
                siValue.value.toScientific(prefs.precision, prefs.outputFormat)
            add(MenuSection.SI_UNITS to listOfNotNull(value + siValue.unit, baseUnits?.let { value + it }))
          }
        }
      }
      is ExpressionResult.Real -> {
        val info = if (unit == null) integerInfo(value) else null
        if (info == null) {
          emptyList()
        } else {
          listOf(
            MenuSection.DECIMAL to listOf(info.decimal),
            MenuSection.HEXADECIMAL to listOf(info.hexadecimal),
            MenuSection.BINARY to listOf(info.binary),
            MenuSection.PRIME_FACTORS to listOfNotNull(info.primeFactors),
          )
        }
      }
      is ExpressionResult.Complex -> complexSections(this)
    }

  /**
   * Complex plane, parts, length and angle of a complex number. Exact values (like √5) go first,
   * then approximation.
   */
  private fun complexSections(result: ExpressionResult.Complex): List<Pair<MenuSection, List<String>>> {
    val prefs = _prefs.value ?: return emptyList()
    val real = result.real
    val imaginary = result.imaginary
    val parts = result.parts
    fun KBigDecimal.text() =
      toFormattedString(prefs.precision, prefs.outputFormat).replace("-", Token.Operator.MINUS)

    /** Exact value, then ≈ decimal when they are not the same. Only ≈ decimal without exact. */
    fun exactAndApproximate(exact: String?, value: KBigDecimal, unit: String = ""): List<String> {
      val decimal = value.text()
      return when (exact) {
        null -> listOf(APPROXIMATELY + decimal + unit)
        decimal -> listOf(exact + unit)
        else -> listOf(exact + unit, APPROXIMATELY + decimal + unit)
      }
    }

    val mathContext = KMathContext(COMPLEX_INFO_PRECISION, KRoundingMode.HALF_EVEN)
    val modulus = KBigDecimalMath.sqrt(real.multiply(real) + imaginary.multiply(imaginary), mathContext)
    val reDouble = real.toPlainString().toDouble()
    val imDouble = imaginary.toPlainString().toDouble()
    val angle = atan2(imDouble, reDouble)
    val degrees = KBigDecimal.valueOf(angle * DEGREES_IN_HALF_TURN / PI)
    return listOf(
      MenuSection.COMPLEX_PLANE to listOf("$reDouble,$imDouble"),
      MenuSection.REAL_PART to exactAndApproximate(parts?.real, real),
      MenuSection.IMAGINARY_PART to exactAndApproximate(parts?.imaginary, imaginary),
      MenuSection.MODULUS to exactAndApproximate(parts?.modulus, modulus),
      MenuSection.ARGUMENT to
        exactAndApproximate(parts?.argument, KBigDecimal.valueOf(angle), " rad") +
          // 45°, but ≈65.905°
          listOf((if (parts?.argument == null) APPROXIMATELY else "") + degrees.text() + "°"),
    )
  }

  /** Like [calculate], but also gives complex numbers and expressions with x. */
  private suspend fun evaluate(
    input: String,
    radianMode: Boolean,
    precision: Int,
    outputFormat: Int,
  ): ExpressionResult =
    withContext(Dispatchers.Default) {
      Expression(input, radianMode, KRoundingMode.HALF_EVEN, onProgress = { ensureActive() })
        .evaluate(precision, outputFormat)
        .also {
        val isTooBig =
          when (it) {
            is ExpressionResult.Real -> it.value.abs().isGreaterThan(maxCalculationResult)
            is ExpressionResult.Complex ->
              it.real.abs().isGreaterThan(maxCalculationResult) ||
                it.imaginary.abs().isGreaterThan(maxCalculationResult)
            is ExpressionResult.Symbolic -> false
          }
        if (isTooBig) throw ExpressionException.TooBig()
      }
    }

  private fun ExpressionResult.format(precision: Int, outputFormat: Int): String =
    when (this) {
      // ≈ when digits are lost: 20÷4500 ==> ≈0.004, 55! ==> ≈1.27×10⁷³
      is ExpressionResult.Real ->
        value.roundingSign(precision, fraction) + value.toScientific(precision, outputFormat) + (unit ?: "")
      is ExpressionResult.Complex -> {
        val isRounded =
          real.roundingSign(precision, fractions?.first).isNotEmpty() ||
            imaginary.roundingSign(precision, fractions?.second).isNotEmpty()
        (if (isRounded) APPROXIMATELY else "") + formatComplex(real, imaginary, precision, outputFormat)
      }
      is ExpressionResult.Symbolic -> expression
    }

  /** `a+bi`, `bi` or `a` when imaginary part is too small to be shown. */
  private fun formatComplex(
    real: KBigDecimal,
    imaginary: KBigDecimal,
    precision: Int,
    outputFormat: Int,
  ): String {
    val realText = real.toScientific(precision, outputFormat)
    val imaginaryText = imaginary.abs().toScientific(precision, outputFormat)
    val imaginaryPart = if (imaginaryText == "1") Token.Const.I else imaginaryText + Token.Const.I
    val isNegative = imaginary < KBigDecimal.ZERO
    return when {
      imaginaryText == "0" -> realText
      realText == "0" -> (if (isNegative) "-" else "") + imaginaryPart
      else -> realText + (if (isNegative) "-" else Token.Operator.PLUS) + imaginaryPart
    }
  }

  /** `1 2⁄3` ==> `1⟨2⟩⟨3⟩` to show it with a horizontal bar. */
  private fun String.toFractionSlots(): String {
    val (whole, fraction) = if (" " in this) split(" ", limit = 2) else listOf("", this)
    val (numerator, denominator) = fraction.split(Token.DisplayOnly.FRACTION, limit = 2)
    val sign = if (numerator.startsWith("-") || whole.startsWith("-")) Token.Operator.MINUS else ""
    return sign +
      whole.removePrefix("-") +
      Token.Layout.NUMERATOR_START +
      numerator.removePrefix("-") +
      Token.Layout.END +
      Token.Layout.DENOMINATOR_START +
      denominator +
      Token.Layout.END
  }

  private val maxCalculationResult = KBigDecimal.valueOf(Double.MAX_VALUE)

  override fun onCleared() {
    viewModelScope.cancel()
    super.onCleared()
  }
}
