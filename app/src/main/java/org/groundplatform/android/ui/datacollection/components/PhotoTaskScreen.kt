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
package org.groundplatform.android.ui.datacollection.components

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.BindingAdapters
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.theme.AppTheme

@Composable
fun PhotoTaskScreen(
  isPhotoPresent: Boolean,
  uri: Uri?,
  onTakePhoto: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
    if (!isPhotoPresent) {
      FilledTonalButton(
        onClick = onTakePhoto,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
      ) {
        Icon(
          imageVector = ImageVector.vectorResource(id = R.drawable.outline_photo_camera),
          contentDescription = stringResource(id = R.string.camera),
          modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(id = R.string.camera))
      }
    } else {
      AndroidView(
        factory = { context ->
          ImageView(context).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = context.getString(R.string.photo_preview)
            setImageResource(R.drawable.ic_photo_grey_600_24dp)
          }
        },
        update = { imageView ->
          if (uri != null) {
            BindingAdapters.bindUri(imageView, uri)
          } else {
            imageView.setImageResource(R.drawable.ic_photo_grey_600_24dp)
          }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun PhotoTaskScreenPreviewEmpty() {
  AppTheme { PhotoTaskScreen(isPhotoPresent = false, uri = null, onTakePhoto = {}) }
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun PhotoTaskScreenPreviewWithPhoto() {
  AppTheme {
    PhotoTaskScreen(
      isPhotoPresent = true,
      uri = "content://media/external/images/media/1".toUri(),
      onTakePhoto = {},
    )
  }
}
