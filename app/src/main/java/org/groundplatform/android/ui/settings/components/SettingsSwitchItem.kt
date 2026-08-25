/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.groundplatform.android.ui.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.ui.theme.AppTheme
import org.groundplatform.ui.theme.sizes

/**
 * A reusable settings item component with a title, optional summary, and a switch toggle.
 *
 * @param modifier The [Modifier] to be applied to the root of this item.
 * @param icon Drawable resource for the icon shown next to the title.
 * @param title The primary text to be displayed for the setting.
 * @param summary Optional secondary text providing additional details about the setting.
 * @param checked Whether the switch is currently in the "on" position.
 * @param onCheckedChange Callback to be invoked when the switch state is toggled.
 */
@Composable
internal fun SettingsSwitchItem(
  modifier: Modifier = Modifier,
  @DrawableRes icon: Int,
  title: String,
  summary: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
        .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      modifier =
        Modifier.padding(end = MaterialTheme.sizes.settingsItemIconEndPadding)
          .size(MaterialTheme.sizes.settingsItemIconSize),
      painter = painterResource(icon),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      if (summary != null) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Switch(checked = checked, onCheckedChange = null)
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun Preview() {
  AppTheme {
    Column(verticalArrangement = Arrangement.SpaceEvenly) {
      SettingsSwitchItem(
        icon = R.drawable.ic_cloud_upload,
        title = "Name",
        summary = "Value",
        checked = true,
        onCheckedChange = {},
      )
      SettingsSwitchItem(
        icon = R.drawable.ic_cloud_upload,
        title = "Name",
        summary = null,
        checked = false,
        onCheckedChange = {},
      )
    }
  }
}
