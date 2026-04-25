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
package org.groundplatform.android.ui.tos

import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.ExcludeFromJacocoGeneratedReport
import org.groundplatform.android.ui.components.Toolbar

@Composable
fun TermsOfServiceScreen(
  isViewOnly: Boolean,
  onNavigateUp: () -> Unit,
  onNavigateToSurveySelector: () -> Unit,
  onError: (String) -> Unit,
  viewModel: TermsOfServiceViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is TosEvent.NavigateToSurveySelector -> onNavigateToSurveySelector()
        is TosEvent.ShowError -> onError(event.message)
      }
    }
  }

  TermsOfServiceContent(
    isViewOnly = isViewOnly,
    uiState = uiState,
    onAgreeCheckedChange = { viewModel.setAgreeCheckboxChecked(it) },
    onAgreeClick = { viewModel.onAgreeButtonClicked() },
    onBackClick = onNavigateUp,
  )
}

@Composable
private fun TermsOfServiceContent(
  isViewOnly: Boolean,
  uiState: TosUiState,
  onAgreeCheckedChange: (Boolean) -> Unit,
  onAgreeClick: () -> Unit,
  onBackClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      Toolbar(
        stringRes = R.string.tos_title,
        showNavigationIcon = isViewOnly,
        iconClick = onBackClick,
      )
    }
  ) { innerPadding ->
    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
      when (uiState) {
        is TosUiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is TosUiState.Success -> {
          Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Spacer(modifier = Modifier.height(16.dp))
            TermsTextView(uiState.termsText)
            Spacer(modifier = Modifier.height(16.dp))

            if (!isViewOnly) {
              AgreeSection(
                agreeChecked = uiState.agreeChecked,
                onCheckedChange = onAgreeCheckedChange,
                onClick = onAgreeClick,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TermsTextView(termsText: Spanned) {
  AndroidView(
    factory = { context ->
      TextView(context).apply {
        movementMethod = LinkMovementMethod.getInstance()
        autoLinkMask = Linkify.WEB_URLS
        setTextAppearance(android.R.style.TextAppearance_Material_Body1)
      }
    },
    update = { textView ->
      textView.text = termsText
      Linkify.addLinks(textView, Linkify.WEB_URLS)
    },
    modifier = Modifier.fillMaxWidth().padding(8.dp),
  )
}

@Composable
private fun AgreeSection(
  agreeChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  onClick: () -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    Checkbox(checked = agreeChecked, onCheckedChange = onCheckedChange)
    Text(
      text = stringResource(R.string.agree_checkbox),
      modifier = Modifier.clickable { onCheckedChange(!agreeChecked) },
    )
  }

  Spacer(modifier = Modifier.height(16.dp))

  Button(onClick = onClick, enabled = agreeChecked) {
    Text(text = stringResource(R.string.agree_terms))
  }

  Spacer(modifier = Modifier.height(32.dp))
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TermsOfServiceContentPreview() {
  TermsOfServiceContent(
    isViewOnly = false,
    uiState =
      TosUiState.Success(
        termsText = android.text.SpannedString("Sample Terms of Service content."),
        agreeChecked = false,
      ),
    onAgreeCheckedChange = {},
    onAgreeClick = {},
    onBackClick = {},
  )
}

@Preview(showBackground = true)
@Composable
@ExcludeFromJacocoGeneratedReport
private fun TermsOfServiceContentIsViewOnlyPreview() {
  TermsOfServiceContent(
    isViewOnly = true,
    uiState =
      TosUiState.Success(
        termsText = android.text.SpannedString("Sample Terms of Service content."),
        agreeChecked = false,
      ),
    onAgreeCheckedChange = {},
    onAgreeClick = {},
    onBackClick = {},
  )
}
