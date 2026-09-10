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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.ui.theme.AppTheme

@VisibleForTesting const val LOCATION_INFO_CARD_TEST_TAG = "location_info_card"
@VisibleForTesting const val CURRENT_LOCATION_TITLE_TEST_TAG = "location_info_card_title"
@VisibleForTesting const val CURRENT_LOCATION_VALUE_TEST_TAG = "location_info_card_value"
@VisibleForTesting const val ACCURACY_TITLE_TEST_TAG = "location_info_card_accuracy_title"
@VisibleForTesting const val ACCURACY_VALUE_TEST_TAG = "location_info_card_accuracy_value"

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
  val hasAccuracy = !locationInfo.accuracyText.isNullOrBlank()

  Card(
    modifier = modifier.testTag(LOCATION_INFO_CARD_TEST_TAG),
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = CardDefaults.outlinedCardBorder(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier =
          if (hasAccuracy) {
            Modifier.weight(0.75f)
          } else {
            Modifier.weight(1f)
          }
      ) {
        Text(
          text = stringResource(locationInfo.titleRes),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(CURRENT_LOCATION_TITLE_TEST_TAG),
        )
        Text(
          text = locationInfo.locationText,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(CURRENT_LOCATION_VALUE_TEST_TAG),
        )
      }

      if (hasAccuracy) {
        Column(modifier = Modifier.weight(0.25f)) {
          Text(
            text = stringResource(R.string.accuracy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ACCURACY_TITLE_TEST_TAG),
          )
          Text(
            text = locationInfo.accuracyText.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            color =
              colorResource(
                if (locationInfo.isAccuracyGood) {
                  R.color.accuracy_good
                } else {
                  R.color.accuracy_bad
                }
              ),
            modifier = Modifier.testTag(ACCURACY_VALUE_TEST_TAG),
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
