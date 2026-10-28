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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KBigDecimalMath
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isExpression
import com.sadellie.unitto.core.common.isGreaterThan
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.data.calculator.CalculatorHistoryRepository
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import com.sadellie.unitto.core.ui.textfield.addBracket
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.sadellie.unitto.core.ui.textfield.getTextFieldState
import com.sadellie.unitto.core.ui.textfield.observe
import com.sadellie.unitto.core.ui.textfield.placeCursorAtTheEnd
import io.github.sadellie.evaluatto.Expression
import io.github.sadellie.evaluatto.ExpressionException
import io.github.sadellie.evaluatto.ExpressionResult
import io.github.sadellie.evaluatto.UnitValue
import io.github.sadellie.evaluatto.integerInfo
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
private val SCIENTIFIC_MAX = KBigDecimal("1E9")
private val SCIENTIFIC_MIN = KBigDecimal("1E-6")

/** 5.5×10¹⁶ (layout slots) when value is too big or too small to read, plain number otherwise. */
private fun KBigDecimal.toScientific(precision: Int, outputFormat: Int): String {
  val magnitude = abs()
  val isPlain = magnitude.compareTo(KBigDecimal.ZERO) == 0 || (magnitude >= SCIENTIFIC_MIN && magnitude < SCIENTIFIC_MAX)
  if (isPlain) return toFormattedString(precision, outputFormat).replace("-", Token.Operator.MINUS)

  val digits = magnitude.stripTrailingZeros().toPlainString()
  val exponent =
    if (magnitude >= KBigDecimal.ONE) {
      digits.substringBefore(".").length - 1
    } else {
      -(digits.substringAfter(".").indexOfFirst { it != '0' } + 1)
    }
  val mantissa = multiply(KBigDecimal("1E${-exponent}"))
  return mantissa.toFormattedString(precision, OutputFormat.PLAIN).replace("-", Token.Operator.MINUS) +
    Token.Operator.MULTIPLY +
    "10" +
    Token.Operator.POWER +
    Token.Layout.SUPERSCRIPT_START +
    exponent.toString().replace("-", Token.Operator.MINUS) +
    Token.Layout.END
}
private const val DEGREES_IN_HALF_TURN = 180.0

internal class CalculatorViewModel(
  private val userPrefsRepository: UserPreferencesRepository,
  private val calculatorHistoryRepository: CalculatorHistoryRepository,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
  private var _calculationJob: Job? = null
  private val _inputKey = "CALCULATOR_INPUT"
  private val _input = savedStateHandle.getTextFieldState(_inputKey)
  private val _result = MutableStateFlow<CalculationResult>(CalculationResult.Empty)
  private val _equalClicked = MutableStateFlow(false)
  private val _prefs = userPrefsRepository.calculatorPrefs.stateIn(viewModelScope, null)

  val uiState: StateFlow<CalculatorUIState> =
    combine(_result, _prefs, calculatorHistoryRepository.historyFlow) { result, prefs, history ->
        prefs ?: return@combine CalculatorUIState.Loading

        return@combine CalculatorUIState.Ready(
          input = _input,
          output = result,
          radianMode = prefs.radianMode,
          precision = prefs.precision,
          outputFormat = prefs.outputFormat,
          formatterSymbols = prefs.formatterSymbols,
          history = history,
          middleZero = prefs.middleZero,
          acButton = prefs.acButton,
          additionalButtons = prefs.additionalButtons,
          inverseMode = prefs.inverseMode,
          partialHistoryView = prefs.partialHistoryView,
          steppedPartialHistoryView = prefs.steppedPartialHistoryView,
          initialPartialHistoryView = prefs.initialPartialHistoryView,
          openHistoryViewButton = prefs.openHistoryViewButton,
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
    when {
      // Equal was clicked and user tries to type a digit or dot
      _equalClicked.value && tokens in Token.Digit.allWithDot -> _input.clearText()
      // Equal was clicked and user tries to add operator or something
      _equalClicked.value -> _input.placeCursorAtTheEnd()
    }
    _input.addTokens(tokens)
    _equalClicked.update { false }
  }

  fun addBracket() {
    if (_equalClicked.value) {
      // Cursor is set to 0 when equal is clicked
      _input.placeCursorAtTheEnd()
    }
    _input.addBracket()
    _equalClicked.update { false }
  }

  fun deleteTokens() {
    if (_equalClicked.value) {
      _input.clearText()
    } else {
      _input.deleteTokens()
    }
    _equalClicked.update { false }
  }

  fun clearInput() {
    _input.clearText()
    _equalClicked.update { false }
  }

  fun updateRadianMode(newValue: Boolean) =
    viewModelScope.launch {
      userPrefsRepository.updateRadianMode(newValue)
      _equalClicked.update { false }
      calculateInput()
    }

  fun updateAdditionalButtons(newValue: Boolean) =
    viewModelScope.launch { userPrefsRepository.updateAdditionalButtons(newValue) }

  fun updateInverseMode(newValue: Boolean) =
    viewModelScope.launch { userPrefsRepository.updateInverseMode(newValue) }

  fun updateInitialPartialHistoryView(newValue: Boolean) =
    viewModelScope.launch { userPrefsRepository.updateInitialPartialHistoryView(newValue) }

  fun clearHistory() = viewModelScope.launch { calculatorHistoryRepository.clear() }

  fun deleteHistoryItem(item: CalculatorHistoryItem) =
    viewModelScope.launch { calculatorHistoryRepository.delete(item.id) }

  /**
   * Adds calculation to history (errors too, with empty result) and clears input, like NumWorks.
   * Result is saved as lines: result, then fraction and roots if there are any.
   */
  fun onEqualClick() =
    viewModelScope.launch {
      val prefs = _prefs.value ?: return@launch
      val inputValue = _input.text.toString()
      if (inputValue.isBlank()) return@launch

      val evaluated =
        try {
          evaluate(inputValue, prefs.radianMode, prefs.precision, prefs.outputFormat)
        } catch (e: Exception) {
          null
        }

      val lines = mutableListOf<String>()
      if (evaluated != null) {
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
        lines += if (exact != null) APPROXIMATELY + approximation else approximation

        // Complex numbers and expressions with x can't be fractions
        if (prefs.fractionalOutput && evaluated is ExpressionResult.Real) {
          val fractional =
            try {
              // Different rounding mode to properly calculate fractional
              calculate(inputValue, prefs.radianMode, KRoundingMode.DOWN).toFractionalString()
            } catch (e: Exception) {
              ""
            }
          if (fractional.isNotEmpty()) lines += fractional.toFractionSlots()
        }

      }

      val historyResult = HistoryResult(lines = lines, menu = evaluated?.menuSections() ?: emptyList())
      calculatorHistoryRepository.add(
        expression = inputValue.replace(EMPTY_INTEGRAL_BOUNDS, Token.Calculus.INTEGRAL),
        result = historyResult.encode(),
      )

      _equalClicked.update { false }
      _input.clearText()
      _result.update { CalculationResult.Empty }
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
        val newResult =
          try {
            val evaluated =
              evaluate(
                input = _input.text.toString(),
                radianMode = prefs.radianMode,
                precision = prefs.precision,
                outputFormat = prefs.outputFormat,
              )
            CalculationResult.Success(evaluated.format(prefs.precision, prefs.outputFormat))
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
      Expression(input, radianMode, roundingMode).calculate().also {
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
      is ExpressionResult.Real if si != null || otherTemperatures.isNotEmpty() -> {
        val prefs = _prefs.value ?: return emptyList()
        fun UnitValue.line() = value.toScientific(prefs.precision, prefs.outputFormat) + unit
        buildList {
          if (otherTemperatures.isNotEmpty()) {
            add(MenuSection.OTHER_TEMPERATURES to otherTemperatures.map { it.line() })
          }
          // 300 K in SI is 300 K again
          val siValue = si?.takeIf { otherTemperatures.isEmpty() || it.unit != unit }
          if (siValue != null) {
            val value = siValue.value.toScientific(prefs.precision, prefs.outputFormat)
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
      if (exact == null) return listOf(APPROXIMATELY + decimal + unit)
      if (exact == decimal) return listOf(exact + unit)
      return listOf(exact + unit, APPROXIMATELY + decimal + unit)
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
      Expression(input, radianMode, KRoundingMode.HALF_EVEN).evaluate(precision, outputFormat).also {
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
      is ExpressionResult.Real -> value.toFormattedString(precision, outputFormat) + (unit ?: "")
      is ExpressionResult.Complex -> formatComplex(real, imaginary, precision, outputFormat)
      is ExpressionResult.Symbolic -> expression
    }

  /** `a+bi`, `bi` or `a` when imaginary part is too small to be shown. */
  private fun formatComplex(
    real: KBigDecimal,
    imaginary: KBigDecimal,
    precision: Int,
    outputFormat: Int,
  ): String {
    val realText = real.toFormattedString(precision, outputFormat)
    val imaginaryText = imaginary.abs().toFormattedString(precision, outputFormat)
    if (imaginaryText == "0") return realText
    val imaginaryPart = if (imaginaryText == "1") Token.Const.I else imaginaryText + Token.Const.I
    val isNegative = imaginary < KBigDecimal.ZERO
    if (realText == "0") return (if (isNegative) "-" else "") + imaginaryPart
    return realText + (if (isNegative) "-" else Token.Operator.PLUS) + imaginaryPart
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
