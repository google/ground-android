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
package com.google.android.ground.ui.home

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.R
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.compose.HyperlinkText
import com.google.android.ground.ui.compose.Toolbar
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : AbstractFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = ComposeView(requireContext()).apply { setContent { AppTheme { CreateView() } } }

  @Preview
  @ExcludeFromJacocoGeneratedReport
  @Composable
  private fun CreateView() {
    Scaffold(
      topBar = {
        Toolbar(stringRes = R.string.about, iconClick = { findNavController().navigateUp() })
      }
    ) { innerPadding ->
      Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 35.dp)) {
        Image(
          bitmap = R.drawable.ground_logo.toImageBitmap(),
          contentDescription = "Logo",
          modifier = Modifier.padding(0.dp, 8.dp, 0.dp, 22.dp).width(122.dp).height(122.dp),
        )

        val uriHandler = LocalUriHandler.current

        HyperlinkText(
          modifier = Modifier.fillMaxWidth(),
          textStyle = MaterialTheme.typography.bodyMedium,
          fullTextResId = R.string.about_ground,
          linkTextColor = MaterialTheme.colorScheme.primary,
          hyperLinks =
            mapOf(
              "apache_license" to
                {
                  uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0.txt")
                },
              "all_licenses" to
                {
                  OssLicensesMenuActivity.setActivityTitle(getString(R.string.view_licenses_title))
                  startActivity(Intent(activity, OssLicensesMenuActivity::class.java))
                },
            ),
        )
      }
    }
  }

  @Composable
  private fun Int.toImageBitmap(): ImageBitmap {
    val drawable = ResourcesCompat.getDrawable(LocalContext.current.resources, this, null)!!
    return (drawable as BitmapDrawable).bitmap.asImageBitmap()
  }
}
