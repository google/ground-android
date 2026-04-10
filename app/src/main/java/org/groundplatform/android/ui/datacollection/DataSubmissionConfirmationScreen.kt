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
package org.groundplatform.android.ui.datacollection

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.ui.components.qrcode.GroundQrCode
import org.groundplatform.ui.theme.AppTheme

private val DEFAULT_TOOLBAR_HEIGHT = 56.dp

@Composable
fun DataSubmissionConfirmationScreen(loiReport: LoiReport? = null, onDismissed: () -> Unit) {
  val baseModifier =
    Modifier.fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .padding(top = DEFAULT_TOOLBAR_HEIGHT)
      .systemBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 48.dp)
  if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    Row(
      modifier = baseModifier,
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        HeaderContent()
        OutlinedButton(modifier = Modifier.padding(top = 24.dp), onClick = { onDismissed() }) {
          Text(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            text = stringResource(id = R.string.close),
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
      ShareableContent(modifier = Modifier.weight(1f), loiReport = loiReport)
    }
  } else {
    Column(modifier = baseModifier, horizontalAlignment = Alignment.CenterHorizontally) {
      HeaderContent(modifier = Modifier.padding(vertical = 16.dp))
      ShareableContent(loiReport = loiReport)
      OutlinedButton(modifier = Modifier.padding(vertical = 24.dp), onClick = { onDismissed() }) {
        Text(
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
          text = stringResource(id = R.string.close),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}

@Composable
private fun DataSubmittedIcon(modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary)
  ) {
    Icon(
      modifier = Modifier.align(Alignment.Center).fillMaxSize().padding(8.dp),
      painter = painterResource(id = R.drawable.baseline_check_24),
      contentDescription = null,
      tint = Color.White,
    )
  }
}

@Composable
private fun HeaderContent(modifier: Modifier = Modifier) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    DataSubmittedIcon(modifier = Modifier.padding(8.dp))
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(R.string.data_collection_complete_details),
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Normal,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(30.dp))
  }
}

@Composable
private fun ShareableContent(modifier: Modifier = Modifier, loiReport: LoiReport?) {
  loiReport?.let {
    Column(modifier) {
      Text(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        text = stringResource(R.string.share_location),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
      )
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              MaterialTheme.colorScheme.background,
              RoundedCornerShape(12.dp),
            )
            .padding(24.dp)
      ) {
        GroundQrCode(
          modifier = Modifier.align(Alignment.Center),
          title = loiReport.loiName,
          footer = stringResource(R.string.scan_this_qr_to_download_geojson),
          content = loiReport.geoJson.toString(),
          contentDescription = "QR code with LOI Geometry",
          centerLogoPainter = painterResource(R.drawable.ground_logo),
        )
      }
    }
  }
}

private val testLoiReport =
  LoiReport(
    loiName = "Test LOI",
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

@Composable
@Preview(showSystemUi = true)
@ExcludeFromJacocoGeneratedReport
private fun DataSubmissionConfirmationScreenPortraitPreview() {
  AppTheme { DataSubmissionConfirmationScreen(loiReport = testLoiReport) {} }
}

@Composable
@Preview(heightDp = 320, widthDp = 800)
@ExcludeFromJacocoGeneratedReport
private fun DataSubmissionConfirmationScreenLandscapePreview() {
  AppTheme { DataSubmissionConfirmationScreen(loiReport = testLoiReport) {} }
}
