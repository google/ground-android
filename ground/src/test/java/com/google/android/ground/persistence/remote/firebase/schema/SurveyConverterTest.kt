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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.android.ground.persistence.remote.firebase.protobuf.toFirestoreMap
import com.google.android.ground.proto.Role
import com.google.android.ground.proto.Survey
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.DocumentSnapshot
import com.sharedtest.FakeData
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class SurveyConverterTest {

  @Test
  fun `Converts to Survey from SurveyDocument`() {
    with(FakeData) {
      val doc =
        SurveyDocument(
          title = SURVEY.title,
          description = SURVEY.description,
          jobs = mapOf(JOB.id to JOB_NESTED_OBJECT),
          acl = SURVEY.acl,
        )
      val snapshot = createSurveyDocumentSnapshot(doc)
      assertThat(SurveyConverter.toSurvey(snapshot)).isEqualTo(SURVEY)
    }
  }

  @Test
  fun `Converts to Survey from Survey proto`() {
    with(FakeData) {
      val surveyProto =
        Survey.newBuilder()
          .setName(SURVEY.title)
          .setDescription(SURVEY.description)
          .putAcl(USER.email, Role.DATA_COLLECTOR)
          .build()
      val snapshot = createSurveyProtoDocumentSnapshot(surveyProto)
      assertThat(SurveyConverter.toSurvey(snapshot, listOf(JOB))).isEqualTo(SURVEY)
    }
  }

  private fun createSurveyDocumentSnapshot(surveyDocument: SurveyDocument): DocumentSnapshot {
    val snapshot = mock(DocumentSnapshot::class.java)
    whenever(snapshot.id).thenReturn(FakeData.SURVEY.id)
    whenever(snapshot.toObject(SurveyDocument::class.java)).thenReturn(surveyDocument)
    whenever(snapshot.exists()).thenReturn(true)
    return snapshot
  }

  private fun createSurveyProtoDocumentSnapshot(surveyProto: Survey): DocumentSnapshot {
    val snapshot = mock(DocumentSnapshot::class.java)
    whenever(snapshot.id).thenReturn(FakeData.SURVEY.id)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(surveyProto.toFirestoreMap())
    return snapshot
  }
}
