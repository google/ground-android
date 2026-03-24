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
package org.groundplatform.android.data.remote.firebase.protobuf

import org.groundplatform.android.model.Survey as SurveyModel
import org.groundplatform.android.proto.Survey as SurveyProto

fun SurveyProto.DataSharingTerms.toModel(): SurveyModel.DataSharingTerms? =
  when (type) {
    SurveyProto.DataSharingTerms.Type.PRIVATE -> SurveyModel.DataSharingTerms.Private
    SurveyProto.DataSharingTerms.Type.PUBLIC_CC0 -> SurveyModel.DataSharingTerms.Public
    SurveyProto.DataSharingTerms.Type.CUSTOM -> SurveyModel.DataSharingTerms.Custom(customText)
    SurveyProto.DataSharingTerms.Type.TYPE_UNSPECIFIED,
    SurveyProto.DataSharingTerms.Type.UNRECOGNIZED -> null
  }

fun SurveyProto.GeneralAccess.toModel(): SurveyModel.GeneralAccess =
  when (this) {
    SurveyProto.GeneralAccess.RESTRICTED -> SurveyModel.GeneralAccess.RESTRICTED
    SurveyProto.GeneralAccess.PUBLIC -> SurveyModel.GeneralAccess.PUBLIC
    SurveyProto.GeneralAccess.UNLISTED -> SurveyModel.GeneralAccess.UNLISTED
    SurveyProto.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED ->
      SurveyModel.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED
    SurveyProto.GeneralAccess.UNRECOGNIZED -> SurveyModel.GeneralAccess.UNRECOGNIZED
  }

fun SurveyProto.DataVisibility?.toModel(): SurveyModel.DataVisibility =
  when (this) {
    SurveyProto.DataVisibility.ALL_SURVEY_PARTICIPANTS ->
      SurveyModel.DataVisibility.ALL_SURVEY_PARTICIPANTS
    SurveyProto.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS ->
      SurveyModel.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS
    SurveyProto.DataVisibility.DATA_VISIBILITY_UNSPECIFIED,
    SurveyProto.DataVisibility.UNRECOGNIZED -> SurveyModel.DataVisibility.UNSPECIFIED
    null -> SurveyModel.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS
  }

fun SurveyModel.DataSharingTerms.toProto(): SurveyProto.DataSharingTerms =
  SurveyProto.DataSharingTerms.newBuilder()
    .apply {
      when (this@toProto) {
        is SurveyModel.DataSharingTerms.Private -> {
          type = SurveyProto.DataSharingTerms.Type.PRIVATE
        }
        is SurveyModel.DataSharingTerms.Public -> {
          type = SurveyProto.DataSharingTerms.Type.PUBLIC_CC0
        }
        is SurveyModel.DataSharingTerms.Custom -> {
          type = SurveyProto.DataSharingTerms.Type.CUSTOM
          customText = this@toProto.text
        }
        SurveyModel.DataSharingTerms.Unspecified -> {
          type = SurveyProto.DataSharingTerms.Type.TYPE_UNSPECIFIED
        }
      }
    }
    .build()

fun SurveyModel.GeneralAccess.toProto(): SurveyProto.GeneralAccess =
  when (this) {
    SurveyModel.GeneralAccess.RESTRICTED -> SurveyProto.GeneralAccess.RESTRICTED
    SurveyModel.GeneralAccess.PUBLIC -> SurveyProto.GeneralAccess.PUBLIC
    SurveyModel.GeneralAccess.UNLISTED -> SurveyProto.GeneralAccess.UNLISTED
    SurveyModel.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED ->
      SurveyProto.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED
    SurveyModel.GeneralAccess.UNRECOGNIZED -> SurveyProto.GeneralAccess.UNRECOGNIZED
  }

fun SurveyModel.DataVisibility.toProto(): SurveyProto.DataVisibility =
  when (this) {
    SurveyModel.DataVisibility.ALL_SURVEY_PARTICIPANTS ->
      SurveyProto.DataVisibility.ALL_SURVEY_PARTICIPANTS
    SurveyModel.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS ->
      SurveyProto.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS
    SurveyModel.DataVisibility.UNSPECIFIED -> SurveyProto.DataVisibility.DATA_VISIBILITY_UNSPECIFIED
  }
