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
package com.google.android.ground.ui.tos

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.surveyselector.SurveySelectorFragmentDirections
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TermsOfServiceViewModel
@Inject
constructor(
  private val navigator: Navigator,
  private val termsOfServiceRepository: TermsOfServiceRepository
) : AbstractViewModel() {
  val agreeCheckboxChecked: MutableLiveData<Boolean> = MutableLiveData()
  val termsOfServiceText: MutableStateFlow<String?> = MutableStateFlow(null)

  init {
    viewModelScope.launch {
      // TODO(#1478): Either cache terms of service text in repository or display a loading spinner.
      termsOfServiceText.emit(termsOfServiceRepository.getTermsOfService()?.text)
    }
  }

  fun onButtonClicked() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    navigator.navigate(SurveySelectorFragmentDirections.showSurveySelectorScreen(true))
  }
}
