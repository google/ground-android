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
import org.groundplatform.android.data.local.LocalValueStore
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
import org.mockito.kotlin.any

@RunWith(MockitoJUnitRunner::class)
class FirebaseMessagingSurveyTest {
  @JvmField @Rule val rule: MockitoRule = MockitoJUnit.rule()
  @Mock private lateinit var surveySyncService: SurveySyncService
  @Mock private lateinit var localValueStore: LocalValueStore
  @Mock private lateinit var remoteMessage: RemoteMessage

  private lateinit var messagingService: FirebaseMessagingService

  @Before
  fun setUp() {
    messagingService = FirebaseMessagingService()
    messagingService.surveySyncService = surveySyncService
    messagingService.localValueStore = localValueStore
  }

  @Test
  fun `enqueues survey sync when the survey is the active one`() {
    `when`(remoteMessage.from).thenReturn("/topics/$SURVEY_ID")
    `when`(localValueStore.lastActiveSurveyId).thenReturn(SURVEY_ID)

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService).enqueueSync(SURVEY_ID)
    verify(localValueStore, never()).staleSurveyIds = any()
  }

  @Test
  fun `defers survey sync when a different survey is active`() {
    `when`(remoteMessage.from).thenReturn("/topics/$SURVEY_ID")
    `when`(localValueStore.lastActiveSurveyId).thenReturn("some-other-survey")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
    verify(localValueStore).staleSurveyIds = setOf(SURVEY_ID)
  }

  @Test
  fun `defers survey sync when no survey is active`() {
    `when`(remoteMessage.from).thenReturn("/topics/$SURVEY_ID")
    `when`(localValueStore.lastActiveSurveyId).thenReturn("")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
    verify(localValueStore).staleSurveyIds = setOf(SURVEY_ID)
  }

  @Test
  fun `ignores null topic`() {
    `when`(remoteMessage.from).thenReturn(null)

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
    verify(localValueStore, never()).staleSurveyIds = any()
  }

  @Test
  fun `ignores empty topic`() {
    `when`(remoteMessage.from).thenReturn("/topics/")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
    verify(localValueStore, never()).staleSurveyIds = any()
  }

  private companion object {
    const val SURVEY_ID = "test-survey-id"
  }
}
