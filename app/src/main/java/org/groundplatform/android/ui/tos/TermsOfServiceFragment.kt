/*
 * Copyright 2021 Google LLC
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.compose.HtmlText
import org.groundplatform.android.ui.compose.Toolbar
import org.groundplatform.android.ui.surveyselector.SurveySelectorFragmentDirections
import org.groundplatform.android.util.createComposeView

@AndroidEntryPoint
class TermsOfServiceFragment : AbstractFragment() {

  private lateinit var viewModel: TermsOfServiceViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(TermsOfServiceViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = createComposeView {
    CreateView(TermsOfServiceFragmentArgs.fromBundle(requireArguments()))
  }

  @Composable
  private fun CreateView(args: TermsOfServiceFragmentArgs) {
    Scaffold(
      topBar = {
        Toolbar(
          stringRes = R.string.tos_title,
          showNavigationIcon = args.isViewOnly,
          iconClick = { findNavController().navigateUp() },
        )
      }
    ) { innerPadding ->
      val termsText by viewModel.termsOfServiceText.observeAsState(AnnotatedString(""))
      val agreeChecked by viewModel.agreeCheckboxChecked.observeAsState(false)

      Column(
        modifier =
          Modifier.padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(16.dp))
        HtmlText(
          html = termsText.toString(),
          modifier = Modifier.padding(8.dp).testTag("sddsfsdfsdf"),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!args.isViewOnly) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
              checked = agreeChecked,
              onCheckedChange = { viewModel.agreeCheckboxChecked.value = it },
            )
            Text(
              text = stringResource(R.string.agree_checkbox),
              modifier = Modifier.clickable { viewModel.agreeCheckboxChecked.value = !agreeChecked },
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(onClick = { viewModel.onButtonClicked() }, enabled = agreeChecked) {
            Text(text = stringResource(R.string.agree_terms))
          }
          Spacer(modifier = Modifier.height(32.dp))
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      viewModel.navigateToSurveySelector.collect {
        findNavController()
          .navigate(SurveySelectorFragmentDirections.showSurveySelectorScreen(true))
      }
    }
  }
}
