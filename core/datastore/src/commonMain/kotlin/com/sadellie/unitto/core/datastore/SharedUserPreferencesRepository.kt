/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2026 Elshan Agaev
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

package com.sadellie.unitto.core.datastore

import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.datastore.UserPreferencesRepository.Defaults
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.UnitsListSorting
import com.sadellie.unitto.core.navigation.graphRoutes
import io.github.sadellie.themmo.core.MonetMode
import io.github.sadellie.themmo.core.ThemingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Saved settings, read by their key ([PrefKeys]). Null when not saved yet. */
interface PreferenceValues {
  fun boolean(key: String): Boolean?

  fun int(key: String): Int?

  fun long(key: String): Long?

  fun string(key: String): String?
}

/** Settings being changed, saved together. */
interface PreferenceEditor : PreferenceValues {
  fun set(key: String, value: Boolean)

  fun set(key: String, value: Int)

  fun set(key: String, value: Long)

  fun set(key: String, value: String)
}

/**
 * Settings of the Android and the web app: how they are read, their defaults and how they are
 * changed. The platform only says where they are saved ([values], [edit]).
 */
abstract class SharedUserPreferencesRepository : UserPreferencesRepository {
  /** The saved settings, again when one changes. */
  protected abstract val values: Flow<PreferenceValues>

  /** Changes settings and saves them. */
  protected abstract suspend fun edit(change: PreferenceEditor.() -> Unit)

  /** Vibrations, the screen kept on and tools: not everywhere. */
  protected open fun PreferenceValues.enableVibrations(): Boolean =
    boolean(PrefKeys.ENABLE_VIBRATIONS_PREF_KEY) ?: Defaults.enableVibrations

  protected open fun PreferenceValues.enableKeepScreenOn(): Boolean =
    boolean(PrefKeys.ENABLE_KEEP_SCREEN_ON_PREF_KEY) ?: Defaults.enableKeepScreenOn

  protected open fun PreferenceValues.enableToolsExperiment(): Boolean =
    boolean(PrefKeys.ENABLE_TOOLS_EXPERIMENT_PREF_KEY) ?: Defaults.enableToolsExperiment

  override val appPrefs: Flow<AppPreferences>
    get() =
      values.map { preferences ->
        AppPreferences(
          themingMode = preferences.themingMode(),
          enableDynamicTheme = preferences.enableDynamicTheme(),
          enableAmoledTheme = preferences.enableAmoledTheme(),
          customColor = preferences.customColor(),
          monetMode = preferences.monetMode(),
          enableToolsExperiment = preferences.enableToolsExperiment(),
          enableVibrations = preferences.enableVibrations(),
          enableKeepScreenOn = preferences.enableKeepScreenOn(),
        )
      }

  override val generalPrefs: Flow<GeneralPreferences>
    get() =
      values.map { preferences ->
        GeneralPreferences(
          lastReadChangelog = preferences.lastReadChangelog(),
          enableVibrations = preferences.enableVibrations(),
          enableKeepScreenOn = preferences.enableKeepScreenOn(),
        )
      }

  override val calculatorPrefs: Flow<CalculatorPreferences>
    get() =
      values.map { preferences ->
        CalculatorPreferences(
          radianMode = preferences.radianMode(),
          formatterSymbols = preferences.formatterSymbols(),
          partialHistoryView = preferences.partialHistoryView(),
          steppedPartialHistoryView = preferences.steppedPartialHistoryView(),
          initialPartialHistoryView = preferences.initialPartialHistoryView(),
          openHistoryViewButton = preferences.openHistoryViewButton(),
          precision = preferences.digitsPrecision(),
          outputFormat = Defaults.outputFormat,
          additionalButtons = preferences.additionalButtons(),
          inverseMode = preferences.inverseMode(),
          fractionalOutput = preferences.fractionalOutput(),
          constantCalculation = preferences.constantCalculation(),
        )
      }

  override val converterPrefs: Flow<ConverterPreferences>
    get() =
      values.map { preferences ->
        ConverterPreferences(
          formatterSymbols = preferences.formatterSymbols(),
          precision = preferences.digitsPrecision(),
          outputFormat = Defaults.outputFormat,
          formatTime = preferences.unitConverterFormatTime(),
          sorting = preferences.unitConverterSorting(),
          shownUnitGroups = preferences.shownUnitGroups(),
          favoritesOnly = preferences.unitConverterFavoritesOnly(),
          latestLeftSideUnit = preferences.latestLeftSide(),
          latestRightSideUnit = preferences.latestRightSide(),
          showIcons = preferences.unitConverterShowIcons(),
        )
      }

  override val formattingPrefs: Flow<FormattingPreferences>
    get() =
      values.map { preferences ->
        FormattingPreferences(
          digitsPrecision = preferences.digitsPrecision(),
          formatterSymbols = preferences.formatterSymbols(),
          outputFormat = Defaults.outputFormat,
        )
      }

  override val unitGroupsPrefs: Flow<UnitGroupsPreferences>
    get() = values.map { UnitGroupsPreferences(shownUnitGroups = it.shownUnitGroups()) }

  override val addSubtractPrefs: Flow<AddSubtractPreferences>
    get() = values.map { AddSubtractPreferences(formatterSymbols = it.formatterSymbols()) }

  override val graphFunctions: Flow<String>
    get() = values.map { it.string(PrefKeys.GRAPH_FUNCTIONS_PREF_KEY) ?: "" }

  override fun appData(key: String): Flow<String> = values.map { it.string(key) ?: "" }

  override val bodyMassPrefs: Flow<BodyMassPreferences>
    get() = values.map { BodyMassPreferences(formatterSymbols = it.formatterSymbols()) }

  override val aboutPrefs: Flow<AboutPreferences>
    get() = values.map { AboutPreferences(enableToolsExperiment = it.enableToolsExperiment()) }

  override suspend fun updateDigitsPrecision(precision: Int) =
    edit { set(PrefKeys.DIGITS_PRECISION_PREF_KEY, precision) }

  override suspend fun updateFormatterSymbols(grouping: String, fractional: String, indian: Boolean) {
    // Grouping and fractional symbols are always different
    if (grouping == fractional) return
    edit {
      set(PrefKeys.FORMATTER_GROUPING_PREF_KEY, grouping)
      set(PrefKeys.FORMATTER_FRACTIONAL_PREF_KEY, fractional)
      set(PrefKeys.FORMATTER_INDIAN_PREF_KEY, indian)
    }
  }

  override suspend fun updateLatestPairOfUnits(unitFrom: String, unitTo: String) = edit {
    set(PrefKeys.LATEST_LEFT_SIDE_PREF_KEY, unitFrom)
    set(PrefKeys.LATEST_RIGHT_SIDE_PREF_KEY, unitTo)
  }

  override suspend fun updateThemingMode(themingMode: ThemingMode) =
    edit { set(PrefKeys.THEMING_MODE_PREF_KEY, themingMode.name) }

  override suspend fun updateDynamicTheme(enabled: Boolean) =
    edit { set(PrefKeys.ENABLE_DYNAMIC_THEME_PREF_KEY, enabled) }

  override suspend fun updateAmoledTheme(enabled: Boolean) =
    edit { set(PrefKeys.ENABLE_AMOLED_THEME_PREF_KEY, enabled) }

  override suspend fun updateCustomColor(color: Long) =
    edit { set(PrefKeys.CUSTOM_COLOR_PREF_KEY, color) }

  override suspend fun updateMonetMode(monetMode: MonetMode) =
    edit { set(PrefKeys.MONET_MODE_PREF_KEY, monetMode.name) }

  override suspend fun updateShownUnitGroups(shownUnitGroups: List<UnitGroup>) =
    edit { set(PrefKeys.SHOWN_UNIT_GROUPS_PREF_KEY, shownUnitGroups.packToString()) }

  override suspend fun addShownUnitGroup(unitGroup: UnitGroup) = edit {
    set(PrefKeys.SHOWN_UNIT_GROUPS_PREF_KEY, shownUnitGroups().plus(unitGroup).packToString())
  }

  override suspend fun removeShownUnitGroup(unitGroup: UnitGroup) = edit {
    set(PrefKeys.SHOWN_UNIT_GROUPS_PREF_KEY, shownUnitGroups().minus(unitGroup).packToString())
  }

  override suspend fun updateLastReadChangelog(value: String) =
    edit { set(PrefKeys.LAST_READ_CHANGELOG_PREF_KEY, value) }

  override suspend fun updateVibrations(enabled: Boolean) =
    edit { set(PrefKeys.ENABLE_VIBRATIONS_PREF_KEY, enabled) }

  override suspend fun updateEnableKeepScreenOn(enabled: Boolean) =
    edit { set(PrefKeys.ENABLE_KEEP_SCREEN_ON_PREF_KEY, enabled) }

  override suspend fun updateToolsExperiment(enabled: Boolean) =
    edit { set(PrefKeys.ENABLE_TOOLS_EXPERIMENT_PREF_KEY, enabled) }

  override suspend fun updateGraphFunctions(value: String) =
    edit { set(PrefKeys.GRAPH_FUNCTIONS_PREF_KEY, value) }

  override suspend fun updateAppData(key: String, value: String) = edit { set(key, value) }

  override suspend fun updateRadianMode(radianMode: Boolean) =
    edit { set(PrefKeys.RADIAN_MODE_PREF_KEY, radianMode) }

  override suspend fun updateUnitConverterFavoritesOnly(enabled: Boolean) =
    edit { set(PrefKeys.UNIT_CONVERTER_FAVORITES_ONLY_PREF_KEY, enabled) }

  override suspend fun updateUnitConverterFormatTime(enabled: Boolean) =
    edit { set(PrefKeys.UNIT_CONVERTER_FORMAT_TIME_PREF_KEY, enabled) }

  override suspend fun updateUnitConverterSorting(sorting: UnitsListSorting) =
    edit { set(PrefKeys.UNIT_CONVERTER_SORTING_PREF_KEY, sorting.name) }

  override suspend fun updateUnitConverterShowIcons(enabled: Boolean) =
    edit { set(PrefKeys.UNIT_CONVERTER_SHOW_ICONS_PREF_KEY, enabled) }

  override suspend fun updatePartialHistoryView(enabled: Boolean) =
    edit { set(PrefKeys.PARTIAL_HISTORY_VIEW_PREF_KEY, enabled) }

  override suspend fun updateSteppedPartialHistoryView(enabled: Boolean) =
    edit { set(PrefKeys.STEPPED_PARTIAL_HISTORY_VIEW_PREF_KEY, enabled) }

  override suspend fun updateInitialPartialHistoryView(enabled: Boolean) =
    edit { set(PrefKeys.INITIAL_PARTIAL_HISTORY_VIEW_PREF_KEY, enabled) }

  override suspend fun updateOpenHistoryViewButton(enabled: Boolean) =
    edit { set(PrefKeys.OPEN_HISTORY_VIEW_BUTTON_PREF_KEY, enabled) }

  override suspend fun updateFractionalOutput(enabled: Boolean) =
    edit { set(PrefKeys.FRACTIONAL_OUTPUT_PREF_KEY, enabled) }

  override suspend fun updateConstantCalculation(enabled: Boolean) =
    edit { set(PrefKeys.CONSTANT_CALCULATION_PREF_KEY, enabled) }

  override suspend fun updateAdditionalButtons(enabled: Boolean) =
    edit { set(PrefKeys.ADDITIONAL_BUTTONS_PREF_KEY, enabled) }

  override suspend fun updateInverseMode(enabled: Boolean) =
    edit { set(PrefKeys.INVERSE_MODE_PREF_KEY, enabled) }

  private fun PreferenceValues.enableDynamicTheme() =
    boolean(PrefKeys.ENABLE_DYNAMIC_THEME_PREF_KEY) ?: Defaults.enableDynamicTheme

  private fun PreferenceValues.themingMode() =
    string(PrefKeys.THEMING_MODE_PREF_KEY)?.letTryOrNull { ThemingMode.valueOf(it) }
      ?: Defaults.themingMode

  private fun PreferenceValues.enableAmoledTheme() =
    boolean(PrefKeys.ENABLE_AMOLED_THEME_PREF_KEY) ?: Defaults.enableAmoledTheme

  private fun PreferenceValues.customColor() = long(PrefKeys.CUSTOM_COLOR_PREF_KEY) ?: Defaults.customColor

  private fun PreferenceValues.monetMode() =
    string(PrefKeys.MONET_MODE_PREF_KEY)?.letTryOrNull { MonetMode.valueOf(it) } ?: Defaults.monetMode

  private fun PreferenceValues.lastReadChangelog() =
    string(PrefKeys.LAST_READ_CHANGELOG_PREF_KEY) ?: Defaults.lastReadChangelog

  private fun PreferenceValues.radianMode() = boolean(PrefKeys.RADIAN_MODE_PREF_KEY) ?: Defaults.radianMode

  private fun PreferenceValues.formatterSymbols(): FormatterSymbols {
    var grouping = string(PrefKeys.FORMATTER_GROUPING_PREF_KEY)
    var fractional = string(PrefKeys.FORMATTER_FRACTIONAL_PREF_KEY)
    if (grouping == null || fractional == null) {
      // formatter symbols must fallback together
      val defaultFormatterSymbols = Defaults.formatterSymbols
      grouping = defaultFormatterSymbols.grouping
      fractional = defaultFormatterSymbols.fractional
    }
    val indian = boolean(PrefKeys.FORMATTER_INDIAN_PREF_KEY) ?: Defaults.formatterSymbols.indian
    return FormatterSymbols(grouping, fractional, indian)
  }

  private fun PreferenceValues.partialHistoryView() =
    boolean(PrefKeys.PARTIAL_HISTORY_VIEW_PREF_KEY) ?: Defaults.partialHistoryView

  private fun PreferenceValues.steppedPartialHistoryView() =
    boolean(PrefKeys.STEPPED_PARTIAL_HISTORY_VIEW_PREF_KEY) ?: Defaults.steppedPartialHistoryView

  private fun PreferenceValues.initialPartialHistoryView() =
    boolean(PrefKeys.INITIAL_PARTIAL_HISTORY_VIEW_PREF_KEY) ?: Defaults.initialPartialHistoryView

  private fun PreferenceValues.openHistoryViewButton() =
    boolean(PrefKeys.OPEN_HISTORY_VIEW_BUTTON_PREF_KEY) ?: Defaults.openHistoryViewButton

  private fun PreferenceValues.digitsPrecision() =
    int(PrefKeys.DIGITS_PRECISION_PREF_KEY) ?: Defaults.digitsPrecision

  private fun PreferenceValues.unitConverterFormatTime() =
    boolean(PrefKeys.UNIT_CONVERTER_FORMAT_TIME_PREF_KEY) ?: Defaults.unitConverterFormatTime

  private fun PreferenceValues.unitConverterSorting() =
    string(PrefKeys.UNIT_CONVERTER_SORTING_PREF_KEY)?.letTryOrNull { UnitsListSorting.valueOf(it) }
      ?: Defaults.unitConverterSorting

  private fun PreferenceValues.shownUnitGroups(): List<UnitGroup> =
    string(PrefKeys.SHOWN_UNIT_GROUPS_PREF_KEY)?.letTryOrNull { list ->
      if (list.isEmpty()) return@letTryOrNull listOf()
      list
        .split(",")
        // Skip removed groups (currency)
        .mapNotNull { name -> UnitGroup.entries.firstOrNull { it.name == name } }
    } ?: Defaults.shownUnitGroups

  private fun PreferenceValues.unitConverterFavoritesOnly() =
    boolean(PrefKeys.UNIT_CONVERTER_FAVORITES_ONLY_PREF_KEY) ?: Defaults.unitConverterFavoritesOnly

  private fun PreferenceValues.unitConverterShowIcons() =
    boolean(PrefKeys.UNIT_CONVERTER_SHOW_ICONS_PREF_KEY) ?: Defaults.unitConverterShowIcons

  private fun PreferenceValues.latestLeftSide() =
    string(PrefKeys.LATEST_LEFT_SIDE_PREF_KEY) ?: Defaults.latestLeftSide

  private fun PreferenceValues.latestRightSide() =
    string(PrefKeys.LATEST_RIGHT_SIDE_PREF_KEY) ?: Defaults.latestRightSide

  private fun PreferenceValues.additionalButtons() =
    boolean(PrefKeys.ADDITIONAL_BUTTONS_PREF_KEY) ?: Defaults.additionButtons

  private fun PreferenceValues.fractionalOutput() =
    boolean(PrefKeys.FRACTIONAL_OUTPUT_PREF_KEY) ?: Defaults.fractionalOutput

  private fun PreferenceValues.constantCalculation() =
    boolean(PrefKeys.CONSTANT_CALCULATION_PREF_KEY) ?: Defaults.constantCalculation

  private fun PreferenceValues.inverseMode() = boolean(PrefKeys.INVERSE_MODE_PREF_KEY) ?: Defaults.inverseMode
}
