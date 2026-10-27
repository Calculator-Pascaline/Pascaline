/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2026 Elshan Agaev
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

package com.sadellie.unitto.feature.settings.about

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.Config
import com.sadellie.unitto.core.designsystem.icons.symbols.Code
import com.sadellie.unitto.core.designsystem.icons.symbols.Copyright
import com.sadellie.unitto.core.designsystem.icons.symbols.Help
import com.sadellie.unitto.core.designsystem.icons.symbols.Info
import com.sadellie.unitto.core.designsystem.icons.symbols.Policy
import com.sadellie.unitto.core.designsystem.icons.symbols.PrivacyTip
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.ListItemExpressive
import com.sadellie.unitto.core.ui.ListItemExpressiveDefaults
import com.sadellie.unitto.core.ui.NavigateUpButton
import com.sadellie.unitto.core.ui.ScaffoldWithLargeTopBar
import com.sadellie.unitto.core.ui.rememberLinkOpener
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.settings_about_unitto
import unitto.core.common.generated.resources.settings_author
import unitto.core.common.generated.resources.settings_note
import unitto.core.common.generated.resources.settings_privacy_policy
import unitto.core.common.generated.resources.settings_terms_and_conditions
import unitto.core.common.generated.resources.settings_third_party_licenses
import unitto.core.common.generated.resources.settings_version_name
import unitto.core.common.generated.resources.settings_view_source_code

@Composable
internal fun AboutRoute(
  navigateUpAction: () -> Unit,
  navigateToThirdParty: () -> Unit,
) {
  AboutScreen(navigateUpAction = navigateUpAction, navigateToThirdParty = navigateToThirdParty)
}

@Composable
private fun AboutScreen(
  navigateUpAction: () -> Unit,
  navigateToThirdParty: () -> Unit,
) {
  ScaffoldWithLargeTopBar(
    title = stringResource(Res.string.settings_about_unitto),
    navigationIcon = { NavigateUpButton(navigateUpAction) },
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(padding)
          .padding(start = Sizes.large, end = Sizes.large, bottom = Sizes.large),
      verticalArrangement = ListItemExpressiveDefaults.ListArrangement,
    ) {
      val linkOpener = rememberLinkOpener()
      AuthorBlock(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        onClick = { linkOpener.launch(Config.AUTHOR_LINK) },
      )
      ListItemExpressive(
        icon = Symbols.PrivacyTip,
        headlineText = stringResource(Res.string.settings_terms_and_conditions),
        onClick = { linkOpener.launch(Config.TERMS_LINK) },
        shape = ListItemExpressiveDefaults.firstShape,
      )
      ListItemExpressive(
        icon = Symbols.Policy,
        headlineText = stringResource(Res.string.settings_privacy_policy),
        onClick = { linkOpener.launch(Config.PRIVACY_LINK) },
        shape = ListItemExpressiveDefaults.middleShape,
      )
      ListItemExpressive(
        icon = Symbols.Code,
        headlineText = stringResource(Res.string.settings_view_source_code),
        onClick = { linkOpener.launch(Config.SOURCE_CODE_LINK) },
        shape = ListItemExpressiveDefaults.middleShape,
      )
      ListItemExpressive(
        icon = Symbols.Copyright,
        headlineText = stringResource(Res.string.settings_third_party_licenses),
        onClick = { navigateToThirdParty() },
        shape = ListItemExpressiveDefaults.middleShape,
      )
      ListItemExpressive(
        icon = Symbols.Info,
        headlineText = stringResource(Res.string.settings_version_name),
        supportingText = "${Config.VERSION_NAME} (${Config.VERSION_CODE})",
        onClick = {},
        shape = ListItemExpressiveDefaults.lastShape,
      )
    }
  }
}

@Composable
private fun AuthorBlock(onClick: () -> Unit, modifier: Modifier) {
  Row(
    modifier =
      modifier
        .clip(ListItemExpressiveDefaults.singleShape)
        .clickable(onClick = onClick)
        .background(MaterialTheme.colorScheme.tertiaryContainer)
        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(Res.string.settings_author),
        style = ListItemExpressiveDefaults.headlineTextStyle,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      Text(
        text = "@sadellie",
        style = ListItemExpressiveDefaults.supportingTextStyle,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation =
      infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10_000), RepeatMode.Reverse),
      )
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.tertiaryFixedDim),
      contentAlignment = Alignment.Center,
    ) {
      val backgroundShape = remember {
        listOf(
            MaterialShapes.Slanted,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Pill,
            MaterialShapes.Ghostish,
            MaterialShapes.Gem,
            MaterialShapes.Arch,
          )
          .random()
      }
      Box(
        modifier =
          Modifier.rotate(rotation.value)
            .padding(8.dp)
            .clip(backgroundShape.toShape())
            .background(Brush.verticalGradient(listOf(Color(BG_TOP), Color(BG_BOTTOM))))
            .size(36.dp)
      )
      Box(
        modifier =
          Modifier.clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(FG_TOP), Color(FG_BOTTOM))))
            .size(18.dp)
      )
    }
  }
}

private const val BG_TOP = 0xFF3E0077
private const val BG_BOTTOM = 0xFF2B002B
private const val FG_TOP = 0xFFD329D1
private const val FG_BOTTOM = 0xFF30003C

@Preview
@Composable
private fun PreviewAboutScreen() {
  AboutScreen(navigateUpAction = {}, navigateToThirdParty = {})
}
