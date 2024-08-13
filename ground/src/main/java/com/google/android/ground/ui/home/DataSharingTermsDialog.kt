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

import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.ground.R
import com.google.android.ground.proto.Survey
import com.google.android.ground.ui.compose.HtmlText
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun DataSharingTermsDialog(
  showDataSharingTermsDialog: MutableState<Boolean>,
  dataSharingTerms: Survey.DataSharingTerms,
  consentGivenCallback: () -> Unit,
) {
  fun dismissDialog() {
    showDataSharingTermsDialog.value = false
  }
  AlertDialog(
    onDismissRequest = { dismissDialog() },
    title = { Text(text = stringResource(R.string.data_consent_dialog_title), fontSize = 22.sp) },
    text = {
      val markdownSrc =
        when (dataSharingTerms.type) {
          Survey.DataSharingTerms.Type.PRIVATE ->
            stringResource(R.string.data_sharing_private_message)
          Survey.DataSharingTerms.Type.PUBLIC_CC0 ->
            stringResource(R.string.data_sharing_public_message)
          Survey.DataSharingTerms.Type.CUSTOM -> dataSharingTerms.customText
          else -> stringResource(R.string.data_sharing_no_terms)
        }
      val flavor = CommonMarkFlavourDescriptor()
      val parsedTree = MarkdownParser(flavor).buildMarkdownTreeFromString(markdownSrc)
      val html = HtmlGenerator(markdownSrc, parsedTree, flavor).generateHtml()
      val dialogSize = Modifier.height(450.dp)
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
