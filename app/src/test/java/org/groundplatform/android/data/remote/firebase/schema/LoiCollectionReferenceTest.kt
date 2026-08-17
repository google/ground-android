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
package org.groundplatform.android.data.remote.firebase.schema

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.protobuf.timestamp
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.FakeData.FAKE_GENERAL_ACCESS
import org.groundplatform.android.FakeData.USER
import org.groundplatform.android.data.remote.firebase.protobuf.toFirestoreMap
import org.groundplatform.android.proto.LocationOfInterest.Source
import org.groundplatform.android.proto.auditInfo
import org.groundplatform.android.proto.coordinates
import org.groundplatform.android.proto.geometry
import org.groundplatform.android.proto.locationOfInterest
import org.groundplatform.android.proto.point
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoiCollectionReferenceTest {
  @Mock private lateinit var mockCollectionReference: CollectionReference
  @Mock(answer = Answers.RETURNS_SELF) private lateinit var mockQuery: Query

  private lateinit var loiCollectionReference: LoiCollectionReference

  // Pages the mocked query returns, in order.
  private var pages: List<List<DocumentSnapshot>> = listOf()
  private var pagesFetched = 0

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)

    whenever(mockCollectionReference.whereEqualTo(any<String>(), any())).thenReturn(mockQuery)
    whenever(mockQuery.get()).thenAnswer {
      val page = pages.getOrElse(pagesFetched) { listOf() }
      pagesFetched++
      Tasks.forResult(mock<QuerySnapshot> { on { documents } doReturn page })
    }

    loiCollectionReference = LoiCollectionReference(mockCollectionReference)
  }

  @Test
  fun `fetch stops after a page shorter than the page size`() = runTest {
    pages = mockPages(3)

    val emitted = loiCollectionReference.fetchPredefined(SURVEY).toList()

    assertThat(emitted.flatten().map { it.id }).containsExactly("loi0", "loi1", "loi2").inOrder()
    assertThat(pagesFetched).isEqualTo(1)
  }

  @Test
  fun `fetch keeps requesting while pages come back full`() = runTest {
    pages = mockPages(PAGE_SIZE, PAGE_SIZE, 2)

    val emitted = loiCollectionReference.fetchPredefined(SURVEY).toList()

    // One emission per page, and the short third page ends it.
    assertThat(emitted.map { it.size }).containsExactly(PAGE_SIZE, PAGE_SIZE, 2).inOrder()
    assertThat(pagesFetched).isEqualTo(3)
  }

  @Test
  fun `fetch resumes each page after the last document of the previous one`() = runTest {
    pages = mockPages(PAGE_SIZE, 1)

    loiCollectionReference.fetchPredefined(SURVEY).toList()

    verify(mockQuery).startAfter("loi${PAGE_SIZE - 1}")
  }

  @Test
  fun `fetch emits nothing when the collection is empty`() = runTest {
    pages = mockPages(0)

    val emitted = loiCollectionReference.fetchPredefined(SURVEY).toList()

    assertThat(emitted).isEmpty()
    assertThat(pagesFetched).isEqualTo(1)
  }

  @Test
  fun `fetch drops unreadable documents but still counts them towards the page`() = runTest {
    val (fullPage, lastPage) = mockPages(PAGE_SIZE, 1)
    val brokenFirst = listOf(mockDocument("broken", jobId = "job the survey does not have"))
    pages = listOf(brokenFirst + fullPage.drop(1), lastPage)

    val emitted = loiCollectionReference.fetchPredefined(SURVEY).toList()

    assertThat(emitted.first()).hasSize(PAGE_SIZE - 1)
    assertThat(emitted.flatten().map { it.id }).doesNotContain("broken")
    assertThat(pagesFetched).isEqualTo(2)
  }

  @Test
  fun `fetch orders by document id and limits each page`() = runTest {
    pages = mockPages(1)

    loiCollectionReference.fetchPredefined(SURVEY).toList()

    verify(mockQuery).orderBy(FieldPath.documentId())
    verify(mockQuery).limit(PAGE_SIZE.toLong())
  }

  @Test
  fun `fetch is lazy until collected`() = runTest {
    pages = mockPages(1)

    loiCollectionReference.fetchPredefined(SURVEY)

    verify(mockQuery, never()).get()
    assertThat(pagesFetched).isEqualTo(0)
  }

  private fun mockPages(vararg sizes: Int): List<List<DocumentSnapshot>> {
    var next = 0
    return sizes.map { size ->
      (next until next + size).map { mockDocument("loi$it") }.also { next += size }
    }
  }

  private fun mockDocument(id: String, jobId: String = JOB_ID): DocumentSnapshot {
    val audit = auditInfo {
      userId = USER.id
      displayName = USER.displayName
      photoUrl = USER.photoUrl.orEmpty()
      clientTimestamp = timestamp { seconds = 987654321 }
      serverTimestamp = timestamp { seconds = 987654321 }
    }
    val proto = locationOfInterest {
      this.id = id
      this.jobId = jobId
      source = Source.IMPORTED
      geometry = geometry {
        point = point {
          coordinates = coordinates {
            latitude = 1.0
            longitude = 2.0
          }
        }
      }
      created = audit
      lastModified = audit
    }
    return mock {
      on { this.id } doReturn id
      on { data } doReturn proto.toFirestoreMap()
      on { exists() } doReturn true
    }
  }

  companion object {
    private const val JOB_ID = "job001"
    private val JOB = Job(JOB_ID, Style("#112233"), "JOB_NAME", mapOf())
    private val SURVEY =
      Survey("survey1", "", "", mapOf(JOB.id to JOB), generalAccess = FAKE_GENERAL_ACCESS)
  }
}
