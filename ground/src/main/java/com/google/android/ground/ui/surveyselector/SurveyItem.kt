/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.ui.surveyselector

import com.google.android.ground.R

data class SurveyItem(
  val surveyId: String,
  val surveyTitle: String,
  val surveyDescription: String,
  val isAvailableOffline: Boolean
) {

  val backgroundDrawable =
    if (isAvailableOffline) R.drawable.survey_synced_background
    else R.drawable.survey_default_background
}
