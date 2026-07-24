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

package org.groundplatform.android.ui.main

/** One-off navigation effects emitted by [MainViewModel]. */
sealed interface MainUiEffect {
  data class OpenStartDestination(val destination: StartDestination) : MainUiEffect

  data object SignedOut : MainUiEffect

  sealed interface StartDestination {
    data object Home : StartDestination

    data object SurveySelector : StartDestination

    data class ActiveSurvey(val surveyId: String) : StartDestination

    data object TermsOfService : StartDestination
  }
}
