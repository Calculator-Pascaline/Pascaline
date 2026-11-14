/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2026 Elshan Agaev
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

package io.github.calculatorpascaline

import android.app.Application
import io.github.calculatorpascaline.core.database.pascalineDatabaseModule
import io.github.calculatorpascaline.core.datastore.dataStoreModule
import io.github.calculatorpascaline.feature.bodymass.bodyMassModule
import io.github.calculatorpascaline.feature.calculator.calculatorModule
import io.github.calculatorpascaline.feature.converter.converterModule
import io.github.calculatorpascaline.feature.datecalculator.dateCalculatorModule
import io.github.calculatorpascaline.feature.glance.converter.converterWidgetModule
import io.github.calculatorpascaline.feature.settings.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.lazyModules
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class PascalineApplication : Application(), KoinStartup {
  override fun onKoinStartup() = koinConfiguration {
    androidContext(this@PascalineApplication)
    lazyModules(
      listOf(
        pascalineDatabaseModule,
        dataStoreModule,
        bodyMassModule,
        calculatorModule,
        converterModule,
        dateCalculatorModule,
        settingsModule,
        converterWidgetModule,
      )
    )
  }
}
