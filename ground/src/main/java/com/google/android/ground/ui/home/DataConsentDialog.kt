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

import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.google.android.ground.R
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
  AndroidView(
    modifier = modifier,
    factory = { context -> TextView(context) },
    update = {
      it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
      it.movementMethod = ScrollingMovementMethod()
      it.setBackgroundResource(R.color.md_theme_onPrimary)
      it.setPadding(32, 32, 32, 32)
    },
  )
}

@Composable
fun DataConsentDialog(
  showDataConsentDialog: MutableState<Boolean>,
  dataSharingConsent: DataSharingConsent,
  consentGivenCallback: () -> Unit,
) {
  fun dismissDialog() {
    showDataConsentDialog.value = false
  }
  AlertDialog(
    onDismissRequest = { dismissDialog() },
    title = { Text(text = stringResource(R.string.data_consent_dialog_title)) },
    text = {
      val markdownSrc =
        when (dataSharingConsent.type) {
          DataSharingConsent.DataSharingConsentType.PRIVATE ->
            stringResource(R.string.data_sharing_private_message)
          DataSharingConsent.DataSharingConsentType.PUBLIC ->
            stringResource(R.string.data_sharing_public_message)
          DataSharingConsent.DataSharingConsentType.CUSTOM -> dataSharingConsent.customText
          else -> ""
        }
      val flavour = CommonMarkFlavourDescriptor()
      val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownSrc)
      val html = HtmlGenerator(markdownSrc, parsedTree, flavour).generateHtml()
      HtmlText(html, Modifier.height(400.dp).padding(0.dp))
    },
    dismissButton = {
      TextButton(onClick = { dismissDialog() }) { Text(text = stringResource(R.string.cancel)) }
    },
    confirmButton = {
      TextButton(
        onClick = {
          consentGivenCallback()
          dismissDialog()
        }
      ) {
        Text(text = stringResource(R.string.agree_checkbox))
      }
    },
  )
}
