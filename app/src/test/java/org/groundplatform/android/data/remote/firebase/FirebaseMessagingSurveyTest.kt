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

package org.groundplatform.android.data.remote.firebase

import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.groundplatform.android.data.local.stores.LocalLocationOfInterestStore
import org.groundplatform.android.data.sync.SurveySyncService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.verifyBlocking

@RunWith(MockitoJUnitRunner::class)
class FirebaseMessagingSurveyTest {
  @JvmField @Rule val rule: MockitoRule = MockitoJUnit.rule()
  @Mock private lateinit var surveySyncService: SurveySyncService
  @Mock private lateinit var remoteMessage: RemoteMessage
  @Mock private lateinit var localLoiStore: LocalLocationOfInterestStore

  private lateinit var messagingService: FirebaseMessagingService

  @Before
  fun setUp() {
    messagingService = FirebaseMessagingService()
    messagingService.surveySyncService = surveySyncService
    messagingService.localLoiStore = localLoiStore
    messagingService.externalScope = CoroutineScope(UnconfinedTestDispatcher())
  }

  @Test
  fun `enqueues survey sync for topic`() {
    val surveyId = "test-survey-id"
    `when`(remoteMessage.from).thenReturn("/topics/${surveyId}")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService).enqueueSync(surveyId)
  }

  @Test
  fun `drops an loi the message reports as deleted`() {
    `when`(remoteMessage.from).thenReturn("/topics/survey")
    `when`(remoteMessage.data).thenReturn(mapOf("loiId" to "loi1", "deleted" to "true"))

    messagingService.onMessageReceived(remoteMessage)

    verifyBlocking(localLoiStore) { safeDeleteLocalLoi("loi1") }
  }

  @Test
  fun `keeps an loi the message only reports as changed`() {
    `when`(remoteMessage.from).thenReturn("/topics/survey")
    `when`(remoteMessage.data).thenReturn(mapOf("loiId" to "loi1"))

    messagingService.onMessageReceived(remoteMessage)

    verifyBlocking(localLoiStore, never()) { safeDeleteLocalLoi(anyString()) }
  }

  @Test
  fun `ignores null topic`() {
    `when`(remoteMessage.from).thenReturn(null)

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
  }

  @Test
  fun `ignores empty topic`() {
    `when`(remoteMessage.from).thenReturn("/topics/")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
  }
}
