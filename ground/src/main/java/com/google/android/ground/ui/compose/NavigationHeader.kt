/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.ground.R

@Composable
fun NavigationHeader(
  navigate: (Int) -> Unit
) {
  Column(modifier = Modifier
    .fillMaxWidth()
    .padding(bottom = 24.dp)
  ) {
    Row(
      modifier =
      Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        painter = painterResource(id = R.drawable.ground_logo),
        contentDescription = null,
        modifier = Modifier
          .size(64.dp)
          .padding(horizontal = 16.dp),
      )
      Text(
        text = stringResource(R.string.app_name),
        fontSize = 18.sp,
        fontFamily = FontFamily(Font(R.font.google_sans)),
        modifier = Modifier.padding(horizontal = 4.dp),
      )
      Spacer(modifier = Modifier.weight(1f))
      Image(
        painter = painterResource(id = R.drawable.ground_logo),
        contentDescription = null,
        modifier = Modifier
          .size(64.dp)
          .padding(end = 24.dp)
      )
    }

    Column(modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_content_paste),
          contentDescription = stringResource(R.string.current_survey),
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = stringResource(R.string.current_survey),
          modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        )
      }

      Column(modifier = Modifier.padding(end = 24.dp)) {
        Text(
          text = "survey.title",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "survey.description",
          modifier = Modifier.padding(top = 8.dp),
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Text(
        text = stringResource(R.string.no_survey_selected),
        modifier = Modifier
          .wrapContentWidth()
          .wrapContentHeight(),
      )

      Text(
        text = stringResource(R.string.switch_survey),
        modifier = Modifier.padding(top = 22.dp, bottom = 22.dp),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )

      HorizontalDivider(modifier = Modifier
        .fillMaxWidth()
        .padding(end = 24.dp))

      Spacer(modifier = Modifier.height(8.dp))
      MenuList(navigate)
    }
    Spacer(modifier = Modifier.weight(1f))
    Text(
      text = "Build.......",
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .padding(bottom = 16.dp),
      style = MaterialTheme.typography.bodyLarge
    )
  }
}

@Composable
private fun MenuList(navigate: (Int) -> Unit) {
  Column(
    modifier = Modifier
  ) {
    MenuItem(
      icon = painterResource(id = R.drawable.ic_history),
      title = stringResource(id = R.string.sync_status),
      navigate
    )
    Spacer(modifier = Modifier.height(12.dp))
    MenuItem(
      icon = painterResource(id = R.drawable.cloud_off),
      title = stringResource(id = R.string.offline_map_imagery),
      navigate
    )
    Spacer(modifier = Modifier.height(12.dp))
    MenuItem(
      icon = painterResource(id = R.drawable.ic_settings),
      title = stringResource(id = R.string.settings),
      navigate
    )
    Spacer(modifier = Modifier.height(12.dp))
    MenuItem(
      icon = painterResource(id = R.drawable.info_outline),
      title = stringResource(id = R.string.about),
      navigate
    )
    Spacer(modifier = Modifier.height(12.dp))
    MenuItem(
      icon = painterResource(id = R.drawable.feed),
      title = stringResource(id = R.string.terms_of_service),
      navigate
    )
  }
}

@Composable
fun MenuItem(icon: Painter, title: String, navigate: (Int) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { navigate(1) },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      painter = icon,
      contentDescription = title,
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
  }
}

@Preview
@Composable
private fun DefaultNavigationHeaderPreview() {
  NavigationHeader(){}
}
