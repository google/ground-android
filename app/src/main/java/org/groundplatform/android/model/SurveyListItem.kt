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

package org.groundplatform.android.model

import org.groundplatform.android.proto.Survey.GeneralAccess

data class SurveyListItem(
  val id: String,
  val title: String,
  val description: String,
  val availableOffline: Boolean,
  val generalAccess: GeneralAccess,
)

fun Survey.toListItem(availableOffline: Boolean): SurveyListItem =
  SurveyListItem(
    id,
    title,
    description,
    availableOffline,
    generalAccess ?: GeneralAccess.GENERAL_ACCESS_UNSPECIFIED,
  )
