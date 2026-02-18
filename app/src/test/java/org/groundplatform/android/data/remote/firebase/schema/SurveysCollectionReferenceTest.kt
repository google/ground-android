/*
 * Copyright 2025 Google LLC
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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.FakeData
import org.groundplatform.android.data.remote.firebase.protobuf.toFirestoreMap
import org.groundplatform.android.model.User
import org.groundplatform.android.proto.Role
import org.groundplatform.android.proto.Survey as SurveyProto
import org.groundplatform.android.proto.Survey.DataVisibility.DATA_VISIBILITY_UNSPECIFIED
import org.groundplatform.android.proto.copy
import org.groundplatform.android.proto.survey
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurveysCollectionReferenceTest {
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock private lateinit var mockQuery: Query
  @Mock private lateinit var mockQuerySnapshot: QuerySnapshot

  private lateinit var surveysCollectionReference: SurveysCollectionReference

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    whenever(mockCollectionReference.whereIn(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockCollectionReference.whereIn(any<FieldPath>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereIn(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.whereIn(any<FieldPath>(), any())).thenReturn(mockQuery)

    val mockRegistration: ListenerRegistration = mock()
    whenever(mockQuery.addSnapshotListener(any<Executor>(), any<MetadataChanges>(), any()))
      .thenAnswer { invocation ->
        val listener = invocation.getArgument<EventListener<QuerySnapshot>>(2)
        listener.onEvent(mockQuerySnapshot, null)
        mockRegistration
      }

    surveysCollectionReference = SurveysCollectionReference(mockCollectionReference)
  }

  @Test
  fun `getReadable includes RESTRICTED surveys`() = runTest {
    val doc = createDocumentSnapshot("restricted", SurveyProto.GeneralAccess.RESTRICTED)
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(doc))

    val result = surveysCollectionReference.getReadable(TEST_USER).first()

    assertThat(result).hasSize(1)
    assertThat(result[0].id).isEqualTo("restricted")
  }

  @Test
  fun `getReadable includes UNLISTED surveys`() = runTest {
    val doc = createDocumentSnapshot("unlisted", SurveyProto.GeneralAccess.UNLISTED)
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(doc))

    val result = surveysCollectionReference.getReadable(TEST_USER).first()

    assertThat(result).hasSize(1)
    assertThat(result[0].id).isEqualTo("unlisted")
  }

  @Test
  fun `getReadable includes GENERAL_ACCESS_UNSPECIFIED surveys`() = runTest {
    val doc =
      createDocumentSnapshot("unspecified", SurveyProto.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED)
    whenever(mockQuerySnapshot.documents).thenReturn(listOf(doc))

    val result = surveysCollectionReference.getReadable(TEST_USER).first()

    assertThat(result).hasSize(1)
    assertThat(result[0].id).isEqualTo("unspecified")
  }

  @Test
  fun `getReadable filters out PUBLIC from mixed list`() = runTest {
    val restrictedDoc = createDocumentSnapshot("restricted", SurveyProto.GeneralAccess.RESTRICTED)
    val publicDoc = createDocumentSnapshot("public", SurveyProto.GeneralAccess.PUBLIC)
    val unlistedDoc = createDocumentSnapshot("unlisted", SurveyProto.GeneralAccess.UNLISTED)
    val unspecifiedDoc =
      createDocumentSnapshot("unspecified", SurveyProto.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED)
    whenever(mockQuerySnapshot.documents)
      .thenReturn(listOf(restrictedDoc, publicDoc, unlistedDoc, unspecifiedDoc))

    val result = surveysCollectionReference.getReadable(TEST_USER).first()

    assertThat(result.map { it.id }).containsExactly("restricted", "unlisted", "unspecified")
  }

  @Test
  fun `getReadable queries for correct state and user ACL`() = runTest {
    whenever(mockQuerySnapshot.documents).thenReturn(emptyList())
    surveysCollectionReference.getReadable(TEST_USER).first()

    verify(mockCollectionReference)
      .whereIn(
        eq(SurveyProto.STATE_FIELD_NUMBER.toString()),
        eq(listOf(SurveyProto.State.READY_VALUE)),
      )
    verify(mockQuery)
      .whereIn(
        eq(FieldPath.of(SurveyProto.ACL_FIELD_NUMBER.toString(), TEST_USER.email)),
        eq(listOf(Role.SURVEY_ORGANIZER.ordinal, Role.DATA_COLLECTOR.ordinal, Role.VIEWER.ordinal)),
      )
  }

  private fun createDocumentSnapshot(
    id: String,
    generalAccess: SurveyProto.GeneralAccess,
  ): DocumentSnapshot {
    val proto = survey {
      this.id = id
      name = "Survey $id"
      description = "Description for $id"
      acl.put(TEST_USER.email, Role.DATA_COLLECTOR)
      dataSharingTerms = FakeData.DATA_SHARING_TERMS.copy {}
      this.generalAccess = generalAccess
      dataVisibility = DATA_VISIBILITY_UNSPECIFIED
    }
    val snapshot: DocumentSnapshot = mock()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(proto.toFirestoreMap())
    return snapshot
  }

  private companion object {
    val TEST_USER = User("id", "test@email.com", "User")
  }
}
