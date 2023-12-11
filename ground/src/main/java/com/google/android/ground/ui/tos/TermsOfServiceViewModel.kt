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
import com.google.android.ground.repository.TermsOfServiceRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.surveyselector.SurveySelectorFragmentDirections
import javax.inject.Inject

class TermsOfServiceViewModel
@Inject
constructor(
  private val navigator: Navigator,
  private val termsOfServiceRepository: TermsOfServiceRepository
) : AbstractViewModel() {
  // TODO(#1478): Convert to MutableLiveData.
  var termsOfServiceText = ""
  val agreeCheckboxChecked: MutableLiveData<Boolean> = MutableLiveData()

  fun onButtonClicked() {
    termsOfServiceRepository.isTermsOfServiceAccepted = true
    navigator.navigate(SurveySelectorFragmentDirections.showSurveySelectorScreen(true))
  }
}
