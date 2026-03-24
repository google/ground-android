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

package org.groundplatform.android.data.remote.firebase.schema

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.remote.firebase.protobuf.toFirestoreMap
import org.groundplatform.android.data.remote.firebase.protobuf.toProto
import org.groundplatform.android.model.Survey
import org.groundplatform.android.proto.Role
import org.groundplatform.android.proto.Survey as SurveyProto
import org.groundplatform.android.proto.Survey.DataVisibility.DATA_VISIBILITY_UNSPECIFIED
import org.groundplatform.android.proto.survey
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class SurveyConverterTest {

  @Test
  fun `Converts to Survey from Survey proto`() {
    with(FakeData) {
      val surveyProto = survey {
        name = SURVEY.title
        description = SURVEY.description
        acl.put(USER.email, Role.DATA_COLLECTOR)
        dataSharingTerms = DATA_SHARING_TERMS.toProto()
        generalAccess = FAKE_GENERAL_ACCESS.toProto()
        dataVisibility = DATA_VISIBILITY_UNSPECIFIED
      }
      val snapshot = createSurveyProtoDocumentSnapshot(surveyProto)
      assertThat(SurveyConverter.toSurvey(snapshot, listOf(JOB, ADHOC_JOB)))
        .isEqualTo(SURVEY.copy(dataVisibility = Survey.DataVisibility.UNSPECIFIED))
    }
  }

  private fun createSurveyProtoDocumentSnapshot(surveyProto: SurveyProto): DocumentSnapshot {
    val snapshot = mock(DocumentSnapshot::class.java)
    whenever(snapshot.id).thenReturn(FakeData.SURVEY.id)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(surveyProto.toFirestoreMap())
    return snapshot
  }
}
