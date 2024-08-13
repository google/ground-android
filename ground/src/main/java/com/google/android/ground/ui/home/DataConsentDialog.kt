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

import android.annotation.SuppressLint
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
import com.google.android.ground.proto.Survey
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
fun Modifier.longDialog() = this.height(400.dp).padding(0.dp)

@Composable
fun Modifier.shortDialog() = this.height(135.dp).padding(0.dp)

@Composable
fun DataConsentDialog(
  showDataConsentDialog: MutableState<Boolean>,
  dataSharingTerms: Survey.DataSharingTerms,
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
        when (dataSharingTerms.type) {
          Survey.DataSharingTerms.Type.PRIVATE ->
            stringResource(R.string.data_sharing_private_message)
          Survey.DataSharingTerms.Type.PUBLIC_CC0 ->
            stringResource(R.string.data_sharing_public_message)
          Survey.DataSharingTerms.Type.CUSTOM -> dataSharingTerms.customText
          else -> "*No terms to display.*"
        }
      val flavour = CommonMarkFlavourDescriptor()
      val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownSrc)
      val html = HtmlGenerator(markdownSrc, parsedTree, flavour).generateHtml()
      val dialogSize = when(dataSharingTerms.type) {
        Survey.DataSharingTerms.Type.PRIVATE -> Modifier.shortDialog()
        Survey.DataSharingTerms.Type.PUBLIC_CC0 -> Modifier.longDialog()
        Survey.DataSharingTerms.Type.CUSTOM -> Modifier.longDialog()
        else -> Modifier.shortDialog()
      }
      HtmlText(html, dialogSize)
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
