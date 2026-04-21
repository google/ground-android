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
package org.groundplatform.android.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.local.stores.LocalSubmissionStore
import org.groundplatform.android.data.remote.RemoteDataStore
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.domain.model.User
import org.groundplatform.domain.model.mutation.LocationOfInterestMutation
import org.groundplatform.domain.model.mutation.Mutation
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.COMPLETED
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.FAILED
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.IN_PROGRESS
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_AWAITING_RETRY
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_IN_PROGRESS
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.MEDIA_UPLOAD_PENDING
import org.groundplatform.domain.model.mutation.Mutation.SyncStatus.PENDING
import org.groundplatform.domain.model.mutation.SubmissionMutation
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.testing.FakeDataGenerator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class MutationRepositoryTest {

  @Mock private lateinit var authenticationManager: AuthenticationManager
  @Mock private lateinit var localLoiStore: LocalLocationOfInterestStore
  @Mock private lateinit var localSubmissionStore: LocalSubmissionStore
  @Mock private lateinit var remoteDataStore: RemoteDataStore
  @Mock private lateinit var userRepository: UserRepositoryInterface

  private lateinit var repository: MutationRepository

  @Before
  fun setUp() {
    repository =
      MutationRepository(
        authenticationManager,
        localLoiStore,
        localSubmissionStore,
        remoteDataStore,
        userRepository,
      )
  }

  @Test
  fun `getIncompleteUploads filters out completed and media-status entries`() = runTest {
    val pending = FakeDataGenerator.newLoiMutation(id = 1, collectionId = "a", syncStatus = PENDING)
    val inProgress =
      FakeDataGenerator.newLoiMutation(id = 2, collectionId = "b", syncStatus = IN_PROGRESS)
    val failed =
      FakeDataGenerator.newSubmissionMutation(id = 3, collectionId = "c", syncStatus = FAILED)
    val mediaPending =
      FakeDataGenerator.newSubmissionMutation(
        id = 4,
        collectionId = "d",
        syncStatus = MEDIA_UPLOAD_PENDING,
      )
    val completed =
      FakeDataGenerator.newSubmissionMutation(id = 5, collectionId = "e", syncStatus = COMPLETED)
    setupMocks(
      loiMutations = listOf(pending, inProgress),
      submissionMutations = listOf(failed, mediaPending, completed),
    )

    val result = repository.getIncompleteUploads()

    assertThat(result.map { it.uploadStatus }).containsExactly(PENDING, IN_PROGRESS, FAILED)
  }

  @Test
  fun `getIncompleteMediaMutations filters to media statuses and returns submission mutations`() =
    runTest {
      val mediaPending =
        FakeDataGenerator.newSubmissionMutation(
          id = 1,
          collectionId = "a",
          syncStatus = MEDIA_UPLOAD_PENDING,
        )
      val mediaInProgress =
        FakeDataGenerator.newSubmissionMutation(
          id = 2,
          collectionId = "b",
          syncStatus = MEDIA_UPLOAD_IN_PROGRESS,
        )
      val mediaAwaitingRetry =
        FakeDataGenerator.newSubmissionMutation(
          id = 3,
          collectionId = "c",
          syncStatus = MEDIA_UPLOAD_AWAITING_RETRY,
        )
      val unrelated =
        FakeDataGenerator.newSubmissionMutation(id = 4, collectionId = "d", syncStatus = PENDING)
      setupMocks(
        loiMutations = emptyList(),
        submissionMutations = listOf(mediaPending, mediaInProgress, mediaAwaitingRetry, unrelated),
      )

      val result = repository.getIncompleteMediaMutations()

      assertThat(result).containsExactly(mediaPending, mediaInProgress, mediaAwaitingRetry)
    }

  @Test
  fun `getUploadQueueFlow filters out mutations belonging to other users`() = runTest {
    val ownMutation = FakeDataGenerator.newLoiMutation(id = 1, collectionId = "own")
    val otherUserMutation =
      FakeDataGenerator.newLoiMutation(id = 2, collectionId = "other", userId = "someone-else")
    setupMocks(
      loiMutations = listOf(ownMutation, otherUserMutation),
      submissionMutations = emptyList(),
    )

    val queue = repository.getUploadQueueFlow().first()

    assertThat(queue.mapNotNull { it.loiMutation }).containsExactly(ownMutation)
  }

  @Test
  fun `getUploadQueueFlow sorts entries by clientTimestamp`() = runTest {
    val older = FakeDataGenerator.newLoiMutation(id = 1, collectionId = "older", timestamp = 1_000)
    val newer =
      FakeDataGenerator.newSubmissionMutation(id = 2, collectionId = "newer", timestamp = 2_000)
    setupMocks(loiMutations = listOf(older), submissionMutations = listOf(newer))

    val queue = repository.getUploadQueueFlow().first()

    assertThat(queue.map { it.clientTimestamp }).containsExactly(1_000L, 2_000L).inOrder()
  }

  @Test
  fun `finalizePendingMutationsForMediaUpload marks non-delete mutations for media upload`() =
    runTest {
      val loi = FakeDataGenerator.newLoiMutation(id = 1, collectionId = "a")
      val submission = FakeDataGenerator.newSubmissionMutation(id = 2, collectionId = "a")

      repository.finalizePendingMutationsForMediaUpload(listOf(loi, submission))

      val loiCaptor = argumentCaptor<List<LocationOfInterestMutation>>()
      val submissionCaptor = argumentCaptor<List<SubmissionMutation>>()
      verify(localLoiStore).updateAll(loiCaptor.capture())
      verify(localSubmissionStore).updateAll(submissionCaptor.capture())
      assertThat(loiCaptor.firstValue.map { it.syncStatus }).containsExactly(MEDIA_UPLOAD_PENDING)
      assertThat(submissionCaptor.firstValue.map { it.syncStatus })
        .containsExactly(MEDIA_UPLOAD_PENDING)
    }

  @Test
  fun `finalizePendingMutationsForMediaUpload deletes LOI and submission for DELETE mutations`() =
    runTest {
      val loi = FakeDataGenerator.newLoiMutation(id = 1, mutationType = Mutation.Type.DELETE)
      val submission =
        FakeDataGenerator.newSubmissionMutation(
          id = 2,
          collectionId = "a",
          mutationType = Mutation.Type.DELETE,
          submissionId = "submission-to-delete",
        )

      repository.finalizePendingMutationsForMediaUpload(listOf(loi, submission))

      verify(localLoiStore).deleteLocationOfInterest(loi.locationOfInterestId)
      verify(localSubmissionStore).deleteSubmission("submission-to-delete")
    }

  @Test
  fun `markAsInProgress saves mutations with IN_PROGRESS status`() = runTest {
    val mutation = FakeDataGenerator.newLoiMutation(syncStatus = PENDING)

    repository.markAsInProgress(listOf(mutation))

    val captor = argumentCaptor<List<LocationOfInterestMutation>>()
    verify(localLoiStore).updateAll(captor.capture())
    assertThat(captor.firstValue.single().syncStatus).isEqualTo(IN_PROGRESS)
  }

  @Test
  fun `markAsComplete saves mutations with COMPLETED status`() = runTest {
    val mutation =
      FakeDataGenerator.newSubmissionMutation(collectionId = "a", syncStatus = IN_PROGRESS)

    repository.markAsComplete(listOf(mutation))

    val captor = argumentCaptor<List<SubmissionMutation>>()
    verify(localSubmissionStore).updateAll(captor.capture())
    assertThat(captor.firstValue.single().syncStatus).isEqualTo(COMPLETED)
  }

  @Test
  fun `markAsFailed increments retry count and records error message`() = runTest {
    val mutation = FakeDataGenerator.newLoiMutation(retryCount = 2)

    repository.markAsFailed(listOf(mutation), RuntimeException("Test exception"))

    val captor = argumentCaptor<List<LocationOfInterestMutation>>()
    verify(localLoiStore).updateAll(captor.capture())
    with(captor.firstValue.single()) {
      assertThat(syncStatus).isEqualTo(FAILED)
      assertThat(retryCount).isEqualTo(3)
      assertThat(lastError).isEqualTo("Test exception")
    }
  }

  @Test
  fun `markAsFailedMediaUpload increments retry count and records error message`() = runTest {
    val mutation = FakeDataGenerator.newSubmissionMutation(retryCount = 0)
    val error = RuntimeException("upload failed")

    repository.markAsFailedMediaUpload(listOf(mutation), error)

    val captor = argumentCaptor<List<SubmissionMutation>>()
    verify(localSubmissionStore).updateAll(captor.capture())
    with(captor.firstValue.single()) {
      assertThat(syncStatus).isEqualTo(MEDIA_UPLOAD_AWAITING_RETRY)
      assertThat(retryCount).isEqualTo(1)
      assertThat(lastError).isEqualTo("upload failed")
    }
  }

  @Test
  fun `markAsMediaUploadInProgress preserves retry count`() = runTest {
    val mutation = FakeDataGenerator.newSubmissionMutation(retryCount = 4, lastError = "last error")

    repository.markAsMediaUploadInProgress(listOf(mutation))

    val captor = argumentCaptor<List<SubmissionMutation>>()
    verify(localSubmissionStore).updateAll(captor.capture())
    with(captor.firstValue.single()) {
      assertThat(syncStatus).isEqualTo(MEDIA_UPLOAD_IN_PROGRESS)
      assertThat(retryCount).isEqualTo(4)
      assertThat(lastError).isEqualTo("last error")
    }
  }

  @Test
  fun `uploadMutations delegates to remote data store with authenticated user`() = runTest {
    setupMocks()
    val mutations = listOf(FakeDataGenerator.newLoiMutation())

    repository.uploadMutations(mutations)

    verify(remoteDataStore).applyMutations(mutations, TEST_USER)
  }

  @Test
  fun `finalizePendingMutationsForMediaUpload with empty list does not touch stores`() = runTest {
    repository.finalizePendingMutationsForMediaUpload(emptyList())

    verify(localLoiStore, never()).deleteLocationOfInterest(org.mockito.kotlin.any())
    verify(localSubmissionStore, never()).deleteSubmission(org.mockito.kotlin.any())
  }

  private suspend fun setupMocks(
    user: User = TEST_USER,
    loiMutations: List<LocationOfInterestMutation> = emptyList(),
    submissionMutations: List<SubmissionMutation> = emptyList(),
  ) {
    whenever(authenticationManager.getAuthenticatedUser()).thenReturn(user)
    whenever(userRepository.getAuthenticatedUser()).thenReturn(user)
    whenever(localLoiStore.getAllMutationsFlow()).thenReturn(flowOf(loiMutations))
    whenever(localSubmissionStore.getAllMutationsFlow()).thenReturn(flowOf(submissionMutations))
  }

  private companion object {
    val TEST_USER = FakeDataGenerator.newUser()
  }
}
