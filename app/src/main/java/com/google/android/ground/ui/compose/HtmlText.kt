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

package com.google.android.ground.ui.compose

import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.google.android.ground.ExcludeFromJacocoGeneratedReport
import com.google.android.ground.R
import com.google.android.ground.ui.theme.AppTheme

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier,
    factory = { context -> TextView(context) },
    update = {
      it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
      it.movementMethod = ScrollingMovementMethod()
      it.setBackgroundResource(R.color.md_theme_onPrimary)
      it.setPadding(24, 16, 24, 16)
    },
  )
}

@Composable
@Preview
@ExcludeFromJacocoGeneratedReport
fun PreviewHtmlText() {
  AppTheme { HtmlText(html = "<h1>Hello World</h1><br/><p>This is a preview.") }
}
