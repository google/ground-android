/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android.ui.offlineareas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun OfflineAreaListItem(
  modifier: Modifier = Modifier,
  offlineAreaDetails: OfflineAreaDetails,
  itemClicked: (areaId: String) -> Unit = {},
) {
  Column {
    Row(
      modifier =
        modifier
          .fillMaxWidth()
          .padding(start = 16.dp, top = 4.dp, end = 24.dp, bottom = 4.dp)
          .clickable { itemClicked(offlineAreaDetails.id) },
      horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = ImageVector.vectorResource(id = R.drawable.ic_offline_pin),
        contentDescription = stringResource(id = R.string.offline_area_list_item_icon),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
      ) {
        Text(
          text = offlineAreaDetails.name,
          style =
            TextStyle(
              fontSize = 16.sp,
              lineHeight = 24.sp,
              fontFamily = FontFamily(Font(R.font.text_500)),
              color = MaterialTheme.colorScheme.onSurface,
            ),
        )

        Text(
          text =
            stringResource(
              id = R.string.offline_area_list_item_size_on_disk_mb,
              offlineAreaDetails.sizeOnDisk,
            ),
          style =
            TextStyle(
              fontSize = 16.sp,
              lineHeight = 24.sp,
              fontFamily = FontFamily(Font(R.font.text_500)),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
private fun PreviewOfflineAreaListItem() {
  AppTheme {
    OfflineAreaListItem(
      offlineAreaDetails =
        OfflineAreaDetails(id = "id", name = "Region name, Country", sizeOnDisk = "12 MB")
    )
  }
}
