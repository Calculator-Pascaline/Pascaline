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

package com.sadellie.unitto.feature.settings.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import com.sadellie.unitto.core.designsystem.ExpressivePreview
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.ListItemExpressive
import com.sadellie.unitto.core.ui.ListItemExpressiveDefaults
import com.sadellie.unitto.core.ui.NavigateUpButton
import com.sadellie.unitto.core.ui.ScaffoldWithLargeTopBar
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.settings_language

@Composable
internal fun LanguageRoute(navigateUp: () -> Unit) {
  LanguageScreen(navigateUp = navigateUp)
}

@Composable
private fun LanguageScreen(navigateUp: () -> Unit) {
  val currentLangKey = remember { AppCompatDelegate.getApplicationLocales().toLanguageTags() }

  fun changeLanguage(langKey: String) {
    val selectedLocale =
      if (langKey == "") {
        LocaleListCompat.getEmptyLocaleList()
      } else {
        LocaleListCompat.forLanguageTags(langKey)
      }

    AppCompatDelegate.setApplicationLocales(selectedLocale)
    navigateUp()
  }

  ScaffoldWithLargeTopBar(
    title = stringResource(Res.string.settings_language),
    navigationIcon = { NavigateUpButton(navigateUp) },
  ) { padding ->
    LazyColumn(
      modifier = Modifier.padding(horizontal = Sizes.large),
      contentPadding = padding,
      verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
    ) {
      languages.forEachIndexed { index, (key, res) ->
        item(key = key, contentType = ContentType.ITEM) {
          ListItemExpressive(
            onClick = { changeLanguage(key) },
            headlineContent = { Text(stringResource(res)) },
            leadingContent = {
              RadioButton(selected = currentLangKey == key, onClick = { changeLanguage(key) })
            },
            shape = ListItemExpressiveDefaults.listedShaped(index, languages.size),
          )
        }
      }
    }
  }
}

private enum class ContentType {
  ITEM,
}

@Preview
@Composable
fun LanguageScreenPreview() = ExpressivePreview { LanguageScreen(navigateUp = {}) }
