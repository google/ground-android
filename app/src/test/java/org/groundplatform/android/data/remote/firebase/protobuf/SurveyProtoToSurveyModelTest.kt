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

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.model.Survey as SurveyModel
import org.groundplatform.android.proto.Survey as SurveyProto
import org.groundplatform.android.proto.SurveyKt.dataSharingTerms
import org.junit.Test

class SurveyProtoToSurveyModelTest {

  @Test
  fun `PRIVATE data sharing terms converts to Private model`() {
    val proto = dataSharingTerms { type = SurveyProto.DataSharingTerms.Type.PRIVATE }

    assertThat(proto.toModel()).isEqualTo(SurveyModel.DataSharingTerms.Private)
  }

  @Test
  fun `PUBLIC_CC0 data sharing terms converts to Public model`() {
    val proto = dataSharingTerms { type = SurveyProto.DataSharingTerms.Type.PUBLIC_CC0 }

    assertThat(proto.toModel()).isEqualTo(SurveyModel.DataSharingTerms.Public)
  }

  @Test
  fun `CUSTOM data sharing terms converts to Custom model with text`() {
    val proto = dataSharingTerms {
      type = SurveyProto.DataSharingTerms.Type.CUSTOM
      customText = "Custom terms text"
    }

    val result = proto.toModel()

    assertThat(result).isInstanceOf(SurveyModel.DataSharingTerms.Custom::class.java)
    assertThat((result as SurveyModel.DataSharingTerms.Custom).text).isEqualTo("Custom terms text")
  }

  @Test
  fun `CUSTOM data sharing terms with empty text converts to Custom model`() {
    val proto = dataSharingTerms {
      type = SurveyProto.DataSharingTerms.Type.CUSTOM
      customText = ""
    }

    val result = proto.toModel()

    assertThat(result).isInstanceOf(SurveyModel.DataSharingTerms.Custom::class.java)
    assertThat((result as SurveyModel.DataSharingTerms.Custom).text).isEmpty()
  }

  @Test
  fun `TYPE_UNSPECIFIED data sharing terms converts to null`() {
    val proto = dataSharingTerms { type = SurveyProto.DataSharingTerms.Type.TYPE_UNSPECIFIED }

    assertThat(proto.toModel()).isNull()
  }

  @Test
  fun `RESTRICTED general access converts to RESTRICTED model`() {
    assertThat(SurveyProto.GeneralAccess.RESTRICTED.toModel())
      .isEqualTo(SurveyModel.GeneralAccess.RESTRICTED)
  }

  @Test
  fun `PUBLIC general access converts to PUBLIC model`() {
    assertThat(SurveyProto.GeneralAccess.PUBLIC.toModel())
      .isEqualTo(SurveyModel.GeneralAccess.PUBLIC)
  }

  @Test
  fun `UNLISTED general access converts to UNLISTED model`() {
    assertThat(SurveyProto.GeneralAccess.UNLISTED.toModel())
      .isEqualTo(SurveyModel.GeneralAccess.UNLISTED)
  }

  @Test
  fun `GENERAL_ACCESS_UNSPECIFIED converts to GENERAL_ACCESS_UNSPECIFIED model`() {
    assertThat(SurveyProto.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED.toModel())
      .isEqualTo(SurveyModel.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED)
  }

  @Test
  fun `UNRECOGNIZED general access converts to UNRECOGNIZED model`() {
    assertThat(SurveyProto.GeneralAccess.UNRECOGNIZED.toModel())
      .isEqualTo(SurveyModel.GeneralAccess.UNRECOGNIZED)
  }

  @Test
  fun `ALL_SURVEY_PARTICIPANTS data visibility converts to ALL_SURVEY_PARTICIPANTS model`() {
    assertThat(SurveyProto.DataVisibility.ALL_SURVEY_PARTICIPANTS.toModel())
      .isEqualTo(SurveyModel.DataVisibility.ALL_SURVEY_PARTICIPANTS)
  }

  @Test
  fun `CONTRIBUTOR_AND_ORGANIZERS data visibility converts to CONTRIBUTOR_AND_ORGANIZERS model`() {
    assertThat(SurveyProto.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS.toModel())
      .isEqualTo(SurveyModel.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS)
  }

  @Test
  fun `DATA_VISIBILITY_UNSPECIFIED converts to UNSPECIFIED model`() {
    assertThat(SurveyProto.DataVisibility.DATA_VISIBILITY_UNSPECIFIED.toModel())
      .isEqualTo(SurveyModel.DataVisibility.UNSPECIFIED)
  }

  @Test
  fun `UNRECOGNIZED data visibility converts to UNSPECIFIED model`() {
    assertThat(SurveyProto.DataVisibility.UNRECOGNIZED.toModel())
      .isEqualTo(SurveyModel.DataVisibility.UNSPECIFIED)
  }

  @Test
  fun `null data visibility converts to CONTRIBUTOR_AND_ORGANIZERS model`() {
    val nullVisibility: SurveyProto.DataVisibility? = null

    assertThat(nullVisibility.toModel())
      .isEqualTo(SurveyModel.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS)
  }

  @Test
  fun `Private model converts to PRIVATE proto`() {
    val result = SurveyModel.DataSharingTerms.Private.toProto()

    assertThat(result.type).isEqualTo(SurveyProto.DataSharingTerms.Type.PRIVATE)
  }

  @Test
  fun `Public model converts to PUBLIC_CC0 proto`() {
    val result = SurveyModel.DataSharingTerms.Public.toProto()

    assertThat(result.type).isEqualTo(SurveyProto.DataSharingTerms.Type.PUBLIC_CC0)
  }

  @Test
  fun `Custom model converts to CUSTOM proto with text`() {
    val result = SurveyModel.DataSharingTerms.Custom("My custom terms").toProto()

    assertThat(result.type).isEqualTo(SurveyProto.DataSharingTerms.Type.CUSTOM)
    assertThat(result.customText).isEqualTo("My custom terms")
  }

  @Test
  fun `Custom model with empty text converts to CUSTOM proto`() {
    val result = SurveyModel.DataSharingTerms.Custom("").toProto()

    assertThat(result.type).isEqualTo(SurveyProto.DataSharingTerms.Type.CUSTOM)
    assertThat(result.customText).isEmpty()
  }

  @Test
  fun `Unspecified model converts to TYPE_UNSPECIFIED proto`() {
    val result = SurveyModel.DataSharingTerms.Unspecified.toProto()

    assertThat(result.type).isEqualTo(SurveyProto.DataSharingTerms.Type.TYPE_UNSPECIFIED)
  }

  @Test
  fun `RESTRICTED model converts to RESTRICTED proto`() {
    assertThat(SurveyModel.GeneralAccess.RESTRICTED.toProto())
      .isEqualTo(SurveyProto.GeneralAccess.RESTRICTED)
  }

  @Test
  fun `PUBLIC model converts to PUBLIC proto`() {
    assertThat(SurveyModel.GeneralAccess.PUBLIC.toProto())
      .isEqualTo(SurveyProto.GeneralAccess.PUBLIC)
  }

  @Test
  fun `UNLISTED model converts to UNLISTED proto`() {
    assertThat(SurveyModel.GeneralAccess.UNLISTED.toProto())
      .isEqualTo(SurveyProto.GeneralAccess.UNLISTED)
  }

  @Test
  fun `GENERAL_ACCESS_UNSPECIFIED model converts to GENERAL_ACCESS_UNSPECIFIED proto`() {
    assertThat(SurveyModel.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED.toProto())
      .isEqualTo(SurveyProto.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED)
  }

  @Test
  fun `UNRECOGNIZED model converts to UNRECOGNIZED proto`() {
    assertThat(SurveyModel.GeneralAccess.UNRECOGNIZED.toProto())
      .isEqualTo(SurveyProto.GeneralAccess.UNRECOGNIZED)
  }

  @Test
  fun `ALL_SURVEY_PARTICIPANTS model converts to ALL_SURVEY_PARTICIPANTS proto`() {
    assertThat(SurveyModel.DataVisibility.ALL_SURVEY_PARTICIPANTS.toProto())
      .isEqualTo(SurveyProto.DataVisibility.ALL_SURVEY_PARTICIPANTS)
  }

  @Test
  fun `CONTRIBUTOR_AND_ORGANIZERS model converts to CONTRIBUTOR_AND_ORGANIZERS proto`() {
    assertThat(SurveyModel.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS.toProto())
      .isEqualTo(SurveyProto.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS)
  }

  @Test
  fun `UNSPECIFIED model converts to DATA_VISIBILITY_UNSPECIFIED proto`() {
    assertThat(SurveyModel.DataVisibility.UNSPECIFIED.toProto())
      .isEqualTo(SurveyProto.DataVisibility.DATA_VISIBILITY_UNSPECIFIED)
  }
}
