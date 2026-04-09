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
package org.groundplatform.android.ui.components

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.groundplatform.android.R
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.ui.components.qrcode.GroundQrCode
import org.groundplatform.ui.theme.AppTheme
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting const val TEST_TAG_SHARE_LOI_COMPONENT = "TEST_TAG_GROUND_QR_CODE"

@Composable
fun ShareLoiComponent(
  modifier: Modifier = Modifier,
  loiReport: LoiReport,
) {
  Box(
    modifier =
      modifier
        .testTag(TEST_TAG_SHARE_LOI_COMPONENT)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
        .padding(24.dp)
  ) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = loiReport.loiName,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      GroundQrCode(
        content = loiReport.geoJson.toString(),
        contentDescription = "QR code with LOI Geometry",
        centerLogoPainter = painterResource(R.drawable.ground_logo),
      )

      loiReport.geoId?.let {
        GeoIdItem(content = it)
      }

      Text(
        text = stringResource(R.string.scan_this_qr_to_download_geojson),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun GeoIdItem(modifier: Modifier = Modifier, content: String) {
  val clipboardManager = LocalClipboard.current
  val scope = rememberCoroutineScope()

  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = "GeoId: $content",
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    IconButton(
      onClick = {
        scope.launch {
          val clipData = ClipData.newPlainText("geoId", content)
          val clipEntry = ClipEntry(clipData)
          clipboardManager.setClipEntry(clipEntry)
        }
      }
    ) {
      Icon(
        modifier = Modifier.size(16.dp),
        painter = painterResource(id = R.drawable.ic_copy),
        contentDescription = "Copy",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Preview
@Composable
private fun ShareLoiComponentPreview() {
  val testLoiReport =
    LoiReport(
      loiName = "Test LOI",
      geoId = "1234567890",
      geoJson =
        JsonObject(
          mapOf(
            "type" to JsonPrimitive("Feature"),
            "properties" to JsonObject(mapOf("name" to JsonPrimitive("Point test"))),
            "geometry" to
              JsonObject(
                mapOf(
                  "type" to JsonPrimitive("Point"),
                  "coordinates" to JsonArray(listOf(JsonPrimitive(-89.0), JsonPrimitive(41.0))),
                )
              ),
          )
        ),
    )

  AppTheme {
    ShareLoiComponent(
      loiReport = testLoiReport,
    )
  }
}
