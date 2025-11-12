/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.surveyselector.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import timber.log.Timber

@Composable
fun HeaderRow(item: SurveyListItem, menuClick: (String) -> Unit) {
  val iconRes = item.generalAccess.iconRes() ?: return
  val labelRes = item.generalAccess.labelString() ?: return

  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Icon(
      painter = painterResource(iconRes),
      contentDescription = stringResource(labelRes),
      modifier = Modifier.size(24.dp).padding(end = 4.dp),
    )

    Text(
      text = stringResource(labelRes),
      fontFamily = FontFamily(Font(R.font.text_500)),
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp),
    )

    Spacer(modifier = Modifier.weight(1f))

    if (item.availableOffline) {
      Icon(
        painter = painterResource(R.drawable.ic_offline_pin),
        contentDescription = stringResource(R.string.offline_icon_description),
        tint = Color(0xff006E2C),
        modifier = Modifier.size(24.dp).padding(end = 4.dp),
      )
      Icon(
        painter = painterResource(R.drawable.ic_more_vert),
        contentDescription = stringResource(R.string.more_options_icon_description),
        modifier =
          Modifier.size(24.dp)
            .clickable { menuClick(item.id) }
            .padding(end = 4.dp)
            .testTag("overflow_${item.id}"),
      )
    }
  }
}

private fun Survey.GeneralAccess.iconRes(): Int? =
  when (ordinal) {
    Survey.GeneralAccess.UNLISTED.ordinal -> {
      R.drawable.ic_unlisted
    }
    Survey.GeneralAccess.PUBLIC.ordinal -> {
      R.drawable.ic_public
    }
    Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED.ordinal,
    Survey.GeneralAccess.RESTRICTED.ordinal -> {
      R.drawable.ic_restricted
    }
    else -> {
      Timber.w("Unsupported GeneralAccess: $this")
      null
    }
  }

private fun Survey.GeneralAccess.labelString(): Int? =
  when (ordinal) {
    Survey.GeneralAccess.UNLISTED.ordinal -> {
      R.string.access_unlisted
    }
    Survey.GeneralAccess.PUBLIC.ordinal -> {
      R.string.access_public
    }
    Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED.ordinal,
    Survey.GeneralAccess.RESTRICTED.ordinal -> {
      R.string.access_restricted
    }
    else -> {
      Timber.w("Unsupported GeneralAccess: $this")
      null
    }
  }
