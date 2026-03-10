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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.User
import org.groundplatform.ui.theme.AppTheme

@Composable
fun HomeDrawer(
  user: User,
  survey: Survey?,
  versionText: String,
  onAction: (HomeDrawerAction) -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .systemBarsPadding()
        .verticalScroll(rememberScrollState())
  ) {
    AppInfoHeader(user = user, onAction = onAction)
    SurveySelector(survey = survey, onSwitchSurvey = { onAction(HomeDrawerAction.OnSwitchSurvey) })
    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
    DrawerItems(onAction, versionText)
  }
}

private val NAV_ITEMS =
  listOf(
    DrawerItem(
      labelId = R.string.sync_status,
      icon = IconSource.Drawable(R.drawable.ic_history),
      action = HomeDrawerAction.OnNavigateToSyncStatus,
    ),
    DrawerItem(
      labelId = R.string.offline_map_imagery,
      icon = IconSource.Drawable(R.drawable.cloud_off),
      action = HomeDrawerAction.OnNavigateToOfflineAreas,
    ),
    DrawerItem(
      labelId = R.string.settings,
      icon = IconSource.Vector(Icons.Default.Settings),
      action = HomeDrawerAction.OnNavigateToSettings,
    ),
    DrawerItem(
      labelId = R.string.about,
      icon = IconSource.Drawable(R.drawable.info_outline),
      action = HomeDrawerAction.OnNavigateToAbout,
    ),
    DrawerItem(
      labelId = R.string.terms_of_service,
      icon = IconSource.Drawable(R.drawable.feed),
      action = HomeDrawerAction.OnNavigateToTerms,
    ),
    DrawerItem(
      labelId = R.string.sign_out,
      icon = IconSource.Vector(Icons.AutoMirrored.Filled.ExitToApp),
      action = HomeDrawerAction.OnSignOut,
    ),
  )

@Composable
private fun DrawerItems(onAction: (HomeDrawerAction) -> Unit, versionText: String) {
  NAV_ITEMS.forEach { item -> DrawerNavigationItem(item, onAction) }

  DrawerVersionFooter(versionText)
}

@Composable
private fun DrawerNavigationItem(item: DrawerItem, onAction: (HomeDrawerAction) -> Unit) {
  val label = stringResource(item.labelId)
  NavigationDrawerItem(
    label = {
      Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        fontFamily =
          androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(R.font.manrope_medium, FontWeight.Medium)
          ),
        lineHeight = 20.sp,
      )
    },
    selected = false,
    onClick = { onAction(item.action) },
    icon = {
      val description = null
      when (item.icon) {
        is IconSource.Vector -> Icon(item.icon.imageVector, contentDescription = description)
        is IconSource.Drawable ->
          Icon(painterResource(item.icon.id), contentDescription = description)
      }
    },
    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).testTag(label),
  )
}

@Composable
private fun DrawerVersionFooter(versionText: String) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(NavigationDrawerItemDefaults.ItemPadding)
        .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Default.Build,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text = versionText,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      fontFamily =
        androidx.compose.ui.text.font.FontFamily(
          androidx.compose.ui.text.font.Font(R.font.manrope_medium, FontWeight.Medium)
        ),
      lineHeight = 20.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

private data class DrawerItem(
  @androidx.annotation.StringRes val labelId: Int,
  val icon: IconSource,
  val action: HomeDrawerAction,
)

@Composable
private fun AppInfoHeader(user: User, onAction: (HomeDrawerAction) -> Unit) {
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
          fontFamily =
            androidx.compose.ui.text.font.FontFamily(
              androidx.compose.ui.text.font.Font(R.font.google_sans)
            ),
          fontWeight = FontWeight.Normal,
          lineHeight = 24.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (user.photoUrl != null) {
        coil.compose.AsyncImage(
          model = user.photoUrl,
          contentDescription = null,
          modifier =
            Modifier.size(32.dp).clip(CircleShape).clickable {
              onAction(HomeDrawerAction.OnUserDetails)
            },
          contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
      }
    }
  }
}

@Suppress("LongMethod")
@Composable
private fun SurveySelector(survey: Survey?, onSwitchSurvey: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        painter = painterResource(R.drawable.ic_content_paste),
        contentDescription = stringResource(R.string.current_survey),
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.width(4.dp))
      Text(
        text = stringResource(R.string.current_survey),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        fontFamily =
          androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(R.font.google_sans)
          ),
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(16.dp))

    if (survey == null) {
      Text(stringResource(R.string.no_survey_selected))
    } else {
      Text(
        text = survey.title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        fontFamily =
          androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(R.font.google_sans)
          ),
        lineHeight = 24.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )
      if (survey.description.isNotEmpty()) {
        Text(
          text = survey.description,
          fontSize = 14.sp,
          fontWeight = FontWeight.Normal,
          fontFamily =
            androidx.compose.ui.text.font.FontFamily(
              androidx.compose.ui.text.font.Font(R.font.google_sans)
            ),
          lineHeight = 20.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = 8.dp),
        )
      }
    }

    Spacer(Modifier.height(16.dp))

    Text(
      text = stringResource(R.string.switch_survey),
      fontSize = 14.sp,
      fontWeight = FontWeight.SemiBold,
      fontFamily =
        androidx.compose.ui.text.font.FontFamily(
          androidx.compose.ui.text.font.Font(R.font.manrope_bold, FontWeight.SemiBold),
          androidx.compose.ui.text.font.Font(R.font.manrope_medium, FontWeight.Medium),
        ),
      lineHeight = 20.sp,
      color = MaterialTheme.colorScheme.primary,
      modifier =
        Modifier.clip(CircleShape).clickable(onClick = onSwitchSurvey).padding(vertical = 10.dp),
    )
  }
}

private sealed interface IconSource {
  data class Vector(val imageVector: androidx.compose.ui.graphics.vector.ImageVector) : IconSource

  data class Drawable(@androidx.annotation.DrawableRes val id: Int) : IconSource
}

@Preview(showBackground = true)
@Composable
private fun HomeDrawerPreview() {
  val mockUser =
    User(id = "1", email = "test@example.com", displayName = "Jane Doe", photoUrl = null)
  val mockSurvey =
    Survey(
      id = "1",
      title = "Tree Survey",
      description = "A comprehensive survey for mapping urban tree canopy and assessing health.",
      jobMap = emptyMap(),
      generalAccess = org.groundplatform.android.proto.Survey.GeneralAccess.PUBLIC,
    )
  AppTheme {
    HomeDrawer(user = mockUser, survey = mockSurvey, versionText = "1.0.0-preview", onAction = {})
  }
}
