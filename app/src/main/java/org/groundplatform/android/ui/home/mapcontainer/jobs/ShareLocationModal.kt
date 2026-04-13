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
package org.groundplatform.android.ui.home.mapcontainer.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.groundplatform.android.R
import org.groundplatform.domain.model.locationofinterest.LoiReport
import org.groundplatform.ui.components.qrcode.GroundQrCode
import org.groundplatform.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLocationModal(loiReport: LoiReport, onDismiss: () -> Unit) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Card(
      modifier = Modifier.padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(
          text = stringResource(R.string.share_location),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Normal,
        )

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = 16.dp)
              .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
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

        TextButton(
          modifier = Modifier.align(Alignment.End).padding(top = 16.dp),
          onClick = onDismiss,
        ) {
          Text(text = stringResource(R.string.close))
        }
      }
    }
  }
}

@Preview
@Composable
private fun ShareLocationModalPreview() {
  val testLoiReport =
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

  AppTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      ShareLocationModal(loiReport = testLoiReport, onDismiss = {})
    }
  }
}
