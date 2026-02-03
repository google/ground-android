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
package org.groundplatform.android.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.User

@Composable
@Suppress("LongMethod")
fun HomeDrawer(
  user: User?,
  survey: Survey?,
  onSwitchSurvey: () -> Unit,
  onNavigateToOfflineAreas: () -> Unit,
  onNavigateToSyncStatus: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToAbout: () -> Unit,
  onNavigateToTerms: () -> Unit,
  onSignOut: () -> Unit,
  versionText: String,
) {
  Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
    // App Info Header
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Image(
          painter = painterResource(R.drawable.ground_logo),
          contentDescription = null,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.app_name),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
          )
        }
        if (user?.photoUrl != null) {
          androidx.compose.ui.viewinterop.AndroidView(
            factory = { context ->
              android.widget.ImageView(context).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
              }
            },
            update = { imageView ->
              com.bumptech.glide.Glide.with(imageView)
                .load(user.photoUrl)
                .circleCrop()
                .into(imageView)
            },
            modifier = Modifier.size(32.dp).clip(CircleShape),
          )
        }
      }
    }

    // Survey Info
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(R.drawable.ic_content_paste),
          contentDescription = stringResource(R.string.current_survey),
          modifier = Modifier.size(14.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
          text = stringResource(R.string.current_survey),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.height(8.dp))

      if (survey == null) {
        Text(stringResource(R.string.no_survey_selected))
      } else {
        Text(
          text = survey.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        if (survey.description.isNotEmpty()) {
          Text(
            text = survey.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
          )
        }
      }

      Spacer(Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.switch_survey),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable(onClick = onSwitchSurvey).padding(vertical = 8.dp),
      )
    }

    HorizontalDivider()

    // Navigation Items
    val navItems =
      listOf(
        DrawerItem(
          label = stringResource(R.string.offline_map_imagery),
          icon = IconSource.Drawable(R.drawable.ic_offline_pin),
          onClick = onNavigateToOfflineAreas,
        ),
        DrawerItem(
          label = stringResource(R.string.sync_status),
          icon = IconSource.Drawable(R.drawable.ic_sync),
          onClick = onNavigateToSyncStatus,
        ),
        DrawerItem(
          label = stringResource(R.string.settings),
          icon = IconSource.Vector(Icons.Default.Settings),
          onClick = onNavigateToSettings,
        ),
        DrawerItem(
          label = stringResource(R.string.about),
          icon = IconSource.Drawable(R.drawable.info_outline),
          onClick = onNavigateToAbout,
        ),
        DrawerItem(
          label = stringResource(R.string.terms_of_service),
          icon = IconSource.Drawable(R.drawable.feed),
          onClick = onNavigateToTerms,
        ),
        DrawerItem(
          label = stringResource(R.string.sign_out),
          icon = IconSource.Vector(Icons.AutoMirrored.Filled.ExitToApp),
          onClick = onSignOut,
        ),
        DrawerItem(
          label = versionText,
          icon = IconSource.Vector(Icons.Default.Build),
          onClick = {},
        ),
      )

    navItems.forEach { item ->
      NavigationDrawerItem(
        label = { Text(item.label) },
        selected = false,
        onClick = item.onClick,
        icon = {
          val description = item.label
          when (item.icon) {
            is IconSource.Vector -> Icon(item.icon.imageVector, contentDescription = description)
            is IconSource.Drawable ->
              Icon(painterResource(item.icon.id), contentDescription = description)
          }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
      )
    }
  }
}

private data class DrawerItem(
  val label: String,
  val icon: IconSource,
  val onClick: () -> Unit,
)

private sealed interface IconSource {
  data class Vector(val imageVector: androidx.compose.ui.graphics.vector.ImageVector) : IconSource
  data class Drawable(@androidx.annotation.DrawableRes val id: Int) : IconSource
}
