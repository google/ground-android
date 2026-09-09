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
package org.groundplatform.android.ui.datacollection.tasks.map.components

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.ui.theme.AppTheme

@VisibleForTesting
object LocationInfoCardTestTags {
  const val LOCATION_INFO_CARD = "location_info_card"
  const val CURRENT_LOCATION_TITLE = "location_info_card_title"
  const val CURRENT_LOCATION_VALUE = "location_info_card_value"
  const val ACCURACY_TITLE = "location_info_card_accuracy_title"
  const val ACCURACY_VALUE = "location_info_card_accuracy_value"
}

@Immutable
data class LocationInfo(
  @StringRes val titleRes: Int,
  val locationText: String,
  val accuracyText: String? = null,
  val isAccuracyGood: Boolean = false,
)

/** A floating card displaying current/map coordinates and GPS accuracy. */
@Composable
fun LocationInfoCard(locationInfo: LocationInfo, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.testTag(LocationInfoCardTestTags.LOCATION_INFO_CARD),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier =
          if (locationInfo.accuracyText != null) {
            Modifier.weight(0.75f)
          } else {
            Modifier.fillMaxWidth()
          }
      ) {
        Text(
          text = stringResource(locationInfo.titleRes),
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF5E5E5E),
          modifier = Modifier.testTag(LocationInfoCardTestTags.CURRENT_LOCATION_TITLE),
        )
        Text(
          text = locationInfo.locationText,
          style = MaterialTheme.typography.labelMedium,
          color = Color(0xFF424940),
          modifier = Modifier.testTag(LocationInfoCardTestTags.CURRENT_LOCATION_VALUE),
        )
      }

      if (locationInfo.accuracyText != null) {
        Column(modifier = Modifier.weight(0.25f)) {
          Text(
            text = stringResource(R.string.accuracy),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF5E5E5E),
            modifier = Modifier.testTag(LocationInfoCardTestTags.ACCURACY_TITLE),
          )
          Text(
            text = locationInfo.accuracyText,
            style = MaterialTheme.typography.labelMedium,
            color =
              colorResource(
                if (locationInfo.isAccuracyGood) {
                  R.color.accuracy_good
                } else {
                  R.color.accuracy_bad
                }
              ),
            modifier = Modifier.testTag(LocationInfoCardTestTags.ACCURACY_VALUE),
          )
        }
      }
    }
  }
}

@Preview
@Composable
@ExcludeFromJacocoGeneratedReport
private fun LocationInfoCardPreview() {
  AppTheme {
    LocationInfoCard(
      locationInfo =
        LocationInfo(
          titleRes = R.string.current_location,
          locationText = "29º58’15” N  114º36’17”W",
          accuracyText = "3m",
          isAccuracyGood = true,
        )
    )
  }
}
