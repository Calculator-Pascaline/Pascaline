/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package io.github.calculatorpascaline.feature.settings.language

import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.locale_cs
import pascaline.core.common.generated.resources.locale_de
import pascaline.core.common.generated.resources.locale_en
import pascaline.core.common.generated.resources.locale_en_rGB
import pascaline.core.common.generated.resources.locale_es
import pascaline.core.common.generated.resources.locale_fr
import pascaline.core.common.generated.resources.locale_hi
import pascaline.core.common.generated.resources.locale_hu
import pascaline.core.common.generated.resources.locale_in
import pascaline.core.common.generated.resources.locale_it
import pascaline.core.common.generated.resources.locale_nl
import pascaline.core.common.generated.resources.locale_pt_rBR
import pascaline.core.common.generated.resources.locale_ru
import pascaline.core.common.generated.resources.locale_sk
import pascaline.core.common.generated.resources.locale_tr
import pascaline.core.common.generated.resources.locale_zh_rCN
import pascaline.core.common.generated.resources.settings_system

internal val languages by lazy {
  listOf(
    "" to Res.string.settings_system,
    "en" to Res.string.locale_en,
    "en-GB" to Res.string.locale_en_rGB,
    "cs" to Res.string.locale_cs,
    "de" to Res.string.locale_de,
    "es" to Res.string.locale_es,
    "fr" to Res.string.locale_fr,
    "hi" to Res.string.locale_hi,
    "hu" to Res.string.locale_hu,
    "id" to Res.string.locale_in,
    "it" to Res.string.locale_it,
    "nl" to Res.string.locale_nl,
    "pt-BR" to Res.string.locale_pt_rBR,
    "ru" to Res.string.locale_ru,
    "sk" to Res.string.locale_sk,
    "tr" to Res.string.locale_tr,
    "zh-CN" to Res.string.locale_zh_rCN,
  )
}
