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

package com.sadellie.unitto.feature.bodymass.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowDown
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.rememberLinkOpener
import com.sadellie.unitto.core.ui.textfield.formatExpression
import com.sadellie.unitto.feature.bodymass.WaistToHeightBand
import com.sadellie.unitto.feature.bodymass.waistToHeightBand
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.body_mass_method_fat
import unitto.core.common.generated.resources.body_mass_method_fat_title
import unitto.core.common.generated.resources.body_mass_method_hip
import unitto.core.common.generated.resources.body_mass_method_hip_title
import unitto.core.common.generated.resources.body_mass_method_ratio
import unitto.core.common.generated.resources.body_mass_method_waist
import unitto.core.common.generated.resources.body_mass_method_waist_title
import unitto.core.common.generated.resources.body_mass_methods
import unitto.core.common.generated.resources.body_mass_note
import unitto.core.common.generated.resources.body_mass_title
import unitto.core.common.generated.resources.body_mass_waist_healthy
import unitto.core.common.generated.resources.body_mass_waist_high
import unitto.core.common.generated.resources.body_mass_waist_increased
import unitto.core.common.generated.resources.body_mass_waist_ratio

/** Ratio with the health risk it shows. Under 0.4 it stays neutral: that is no healthy band. */
@Composable
internal fun WaistToHeightTile(modifier: Modifier, ratio: KBigDecimal, formatterSymbols: FormatterSymbols) {
  val band = waistToHeightBand(ratio)
  val text = ratio.toFormattedString(RATIO_SCALE, OutputFormat.PLAIN).formatExpression(formatterSymbols)
  val ratioName = stringResource(Res.string.body_mass_waist_ratio)
  BodyMassTile(
    modifier = modifier,
    color = band?.color() ?: MaterialTheme.colorScheme.secondaryContainer,
    value = text,
    label = band?.let { "${stringResource(it.title())}\n$ratioName" } ?: ratioName,
  )
}

private fun WaistToHeightBand.title(): StringResource =
  when (this) {
    WaistToHeightBand.HEALTHY -> Res.string.body_mass_waist_healthy
    WaistToHeightBand.INCREASED -> Res.string.body_mass_waist_increased
    WaistToHeightBand.HIGH -> Res.string.body_mass_waist_high
  }

private fun WaistToHeightBand.color(): Color =
  when (this) {
    WaistToHeightBand.HEALTHY -> HEALTHY_COLOR
    WaistToHeightBand.INCREASED -> INCREASED_COLOR
    WaistToHeightBand.HIGH -> HIGH_COLOR
  }

/**
 * What BMI can't tell, what else can be checked and where that comes from. One topic is open at a
 * time so that the page stays short, the limits of BMI are open first.
 */
@Composable
internal fun BodyMassGuide(modifier: Modifier) {
  val topics =
    listOf(
      Res.string.body_mass_title to Res.string.body_mass_note,
      Res.string.body_mass_waist_ratio to Res.string.body_mass_method_ratio,
      Res.string.body_mass_method_waist_title to Res.string.body_mass_method_waist,
      Res.string.body_mass_method_hip_title to Res.string.body_mass_method_hip,
      Res.string.body_mass_method_fat_title to Res.string.body_mass_method_fat,
    )
  var open by rememberSaveable { mutableIntStateOf(0) }
  Column(
    modifier =
      modifier
        .clip(MaterialTheme.shapes.extraLarge)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(Sizes.large),
  ) {
    Text(
      text = stringResource(Res.string.body_mass_methods),
      modifier = Modifier.padding(bottom = 4.dp),
      style = MaterialTheme.typography.titleMedium,
    )
    topics.forEachIndexed { index, (title, text) ->
      if (index > 0) HorizontalDivider()
      GuideTopic(title, text, isOpen = open == index) { open = if (open == index) NO_TOPIC else index }
    }
    BodyMassSources(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
  }
}

@Composable
private fun GuideTopic(title: StringResource, text: StringResource, isOpen: Boolean, onClick: () -> Unit) {
  val arrowAngle by animateFloatAsState(if (isOpen) HALF_TURN else 0f, label = "Topic arrow")
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(title),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
      )
      Icon(Symbols.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(arrowAngle))
    }
    AnimatedVisibility(visible = isOpen) {
      Text(
        text = stringResource(text),
        modifier = Modifier.padding(bottom = 12.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun BodyMassSources(modifier: Modifier) {
  val linkOpener = rememberLinkOpener()
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    SourceButton("WHO") { linkOpener.launch(WHO_OBESITY_FACT_SHEET) }
    SourceButton("The Lancet") { linkOpener.launch(LANCET_CLINICAL_OBESITY) }
    SourceButton("NHS") { linkOpener.launch(NHS_WAIST_TO_HEIGHT) }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceButton(name: String, onClick: () -> Unit) {
  OutlinedButton(
    onClick = onClick,
    shapes = ButtonDefaults.shapes(),
    contentPadding = ButtonDefaults.SmallContentPadding,
    modifier = Modifier.height(ButtonDefaults.MinHeight),
  ) {
    Text(text = name, style = ButtonDefaults.textStyleFor(ButtonDefaults.MinHeight))
  }
}

/** Categories of BMI, kept up to date by the World Health Organization. */
private const val WHO_OBESITY_FACT_SHEET =
  "https://www.who.int/news-room/fact-sheets/detail/obesity-and-overweight"

/** How to measure a waist, and who the ratio is for. */
private const val NHS_WAIST_TO_HEIGHT = "https://www.nhs.uk/health-assessment-tools/calculate-your-waist-to-height-ratio"

/** Rubino et al., Definition and diagnostic criteria of clinical obesity, 2025. */
private const val LANCET_CLINICAL_OBESITY = "https://doi.org/10.1016/S2213-8587(24)00316-4"

private const val RATIO_SCALE = 2
private const val NO_TOPIC = -1
private const val HALF_TURN = 180f
// Same colors as the categories of BMI
private val HEALTHY_COLOR = Color(0x805BF724)
private val INCREASED_COLOR = Color(0x80DBEC18)
private val HIGH_COLOR = Color(0x80FF9634)
