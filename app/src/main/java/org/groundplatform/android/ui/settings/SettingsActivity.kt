/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractActivity
import org.groundplatform.android.ui.theme.AppTheme

@AndroidEntryPoint
class SettingsActivity : AbstractActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      AppTheme {
        Scaffold(
          topBar = {
            TopAppBar(
              title = { Text(text = stringResource(R.string.settings)) },
              navigationIcon = {
                IconButton(onClick = { finish() }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
              },
            )
          }
        ) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) { SettingsScreen() }
        }
      }
    }
  }
}
