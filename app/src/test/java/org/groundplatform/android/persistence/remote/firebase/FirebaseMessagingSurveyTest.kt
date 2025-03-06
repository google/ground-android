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

package org.groundplatform.android.persistence.remote.firebase

import com.google.firebase.messaging.RemoteMessage
import org.groundplatform.android.persistence.sync.SurveySyncService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule

@RunWith(MockitoJUnitRunner::class)
class FirebaseMessagingSurveyTest {
  @JvmField @Rule val rule: MockitoRule = MockitoJUnit.rule()
  @Mock private lateinit var surveySyncService: SurveySyncService
  @Mock private lateinit var remoteMessage: RemoteMessage

  private lateinit var messagingService: FirebaseMessagingService

  @Before
  fun setUp() {
    messagingService = FirebaseMessagingService()
    messagingService.surveySyncService = surveySyncService
  }

  @Test
  fun enqueueSurveySyncForTopic() {
    val surveyId = "test-survey-id"
    `when`(remoteMessage.from).thenReturn("/topics/${surveyId}")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService).enqueueSync(surveyId)
  }

  @Test
  fun ignoreNullTopic() {
    `when`(remoteMessage.from).thenReturn(null)

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
  }

  @Test
  fun ignoreEmptyTopic() {
    `when`(remoteMessage.from).thenReturn("/topics/")

    messagingService.onMessageReceived(remoteMessage)

    verify(surveySyncService, never()).enqueueSync(anyString())
  }
}
