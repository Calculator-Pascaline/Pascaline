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

package com.sadellie.unitto.feature.settings.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sadellie.unitto.core.common.WebFiles
import com.sadellie.unitto.core.common.WebStorage
import com.sadellie.unitto.core.designsystem.icons.symbols.FileSave
import com.sadellie.unitto.core.designsystem.icons.symbols.RestorePage
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.designsystem.shapes.Sizes
import com.sadellie.unitto.core.ui.NavigateUpButton
import com.sadellie.unitto.core.ui.ScaffoldWithLargeTopBar
import kotlin.time.Clock
import kotlinx.browser.window
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.settings_back_up
import unitto.core.common.generated.resources.settings_backup
import unitto.core.common.generated.resources.settings_restore

/**
 * Backup of the web app: everything it saved in the browser (history, units, settings) in one file.
 * The Android app's backup is its database, which a browser can't read.
 */
@Composable
internal fun BackupRoute(navigateUpAction: () -> Unit) {
  ScaffoldWithLargeTopBar(
    title = stringResource(Res.string.settings_backup),
    navigationIcon = { NavigateUpButton(navigateUpAction) },
  ) { paddingValues ->
    Row(
      modifier = Modifier.padding(paddingValues).padding(Sizes.large).fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      BackupRestoreButton(
        onClick = { WebFiles.save(backupFileName(), BACKUP_MIME_TYPE, WebStorage.exportAll()) },
        imageVector = Symbols.FileSave,
        label = stringResource(Res.string.settings_back_up),
      )
      BackupRestoreButton(
        onClick = {
          WebFiles.open(".json") { text ->
            // Starts again with what was restored, like the Android app
            if (WebStorage.importAll(text)) window.location.reload()
          }
        },
        imageVector = Symbols.RestorePage,
        label = stringResource(Res.string.settings_restore),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupRestoreButton(onClick: () -> Unit, imageVector: ImageVector, label: String) {
  val interactionSource = remember { MutableInteractionSource() }
  Column(
    modifier =
      Modifier.clickable(
        onClick = onClick,
        indication = null,
        interactionSource = interactionSource,
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Sizes.small),
  ) {
    FilledIconButton(
      onClick = onClick,
      shapes = IconButtonDefaults.shapes(),
      interactionSource = interactionSource,
      colors =
        IconButtonDefaults.filledIconButtonColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      modifier =
        Modifier.size(
          IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)
        ),
    ) {
      Icon(
        imageVector = imageVector,
        contentDescription = label,
        modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
      )
    }
    Text(
      text = label,
      color = MaterialTheme.colorScheme.onSurface,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

private fun backupFileName(): String {
  val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
  fun Int.twoDigits() = toString().padStart(2, '0')
  return "pascaline-web-${now.year}-${(now.month.ordinal + 1).twoDigits()}-${now.day.twoDigits()}" +
    "-${now.hour.twoDigits()}-${now.minute.twoDigits()}-${now.second.twoDigits()}.json"
}

private const val BACKUP_MIME_TYPE = "application/json"
