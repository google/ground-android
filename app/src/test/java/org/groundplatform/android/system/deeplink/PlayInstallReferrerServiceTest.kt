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
package org.groundplatform.android.system.deeplink

import android.content.Context
import android.os.RemoteException
import androidx.test.core.app.ApplicationProvider
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.data.local.LocalValueStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayInstallReferrerServiceTest {

  private val localValueStore: LocalValueStore = mock()
  private val client: InstallReferrerClient = mock()
  private val builder: InstallReferrerClient.Builder = mock()
  private lateinit var staticClient: MockedStatic<InstallReferrerClient>
  private lateinit var service: PlayInstallReferrerService

  @Before
  fun setUp() {
    staticClient = mockStatic(InstallReferrerClient::class.java)
    staticClient
      .`when`<InstallReferrerClient.Builder> { InstallReferrerClient.newBuilder(any<Context>()) }
      .thenReturn(builder)
    whenever(builder.build()).thenReturn(client)
    whenever(localValueStore.isDeferredDeeplinkConsumed).thenReturn(false)
    service =
      PlayInstallReferrerService(ApplicationProvider.getApplicationContext(), localValueStore)
  }

  @After
  fun tearDown() {
    staticClient.close()
  }

  @Test
  fun `parseSurveyId returns survey id when referrer holds only the survey id`() {
    assertThat(service.parseSurveyId("survey_id=$SURVEY_ID")).isEqualTo(SURVEY_ID)
  }

  @Test
  fun `parseSurveyId returns survey id even when referrer holds other params`() {
    assertThat(service.parseSurveyId("source=ground&survey_id=$SURVEY_ID&medium=email"))
      .isEqualTo(SURVEY_ID)
  }

  @Test
  fun `parseSurveyId decodes percent encoded values`() {
    assertThat(service.parseSurveyId("survey_id=survey%20123")).isEqualTo("survey 123")
  }

  @Test
  fun `parseSurveyId returns null for an empty referrer`() {
    assertThat(service.parseSurveyId("")).isNull()
  }

  @Test
  fun `parseSurveyId returns null when the value is missing`() {
    assertThat(service.parseSurveyId("survey_id=")).isNull()
  }

  @Test
  fun `parseSurveyId returns null when the value is blank`() {
    assertThat(service.parseSurveyId("survey_id=%20")).isNull()
  }

  @Test
  fun `parseSurveyId ignores keys that merely end with the survey_id key`() {
    assertThat(service.parseSurveyId("other_survey_id=$SURVEY_ID")).isNull()
  }

  @Test
  fun `parseSurveyId returns null when a param has no value delimiter`() {
    assertThat(service.parseSurveyId("survey_id")).isNull()
  }

  @Test
  fun `parseSurveyId ignores other params except for survey_id`() {
    assertThat(service.parseSurveyId("=ignored&survey_id=$SURVEY_ID")).isEqualTo(SURVEY_ID)
  }

  @Test
  fun `getDeferredSurveyId returns the survey id and marks the referrer consumed`() = runTest {
    setUpReferrer("survey_id=$SURVEY_ID")

    assertThat(service.getDeferredSurveyId()).isEqualTo(SURVEY_ID)

    verify(localValueStore).isDeferredDeeplinkConsumed = true
  }

  @Test
  fun `getDeferredSurveyId marks the referrer consumed even when it holds no survey id`() =
    runTest {
      setUpReferrer("utm_source=google-play&utm_medium=organic")

      assertThat(service.getDeferredSurveyId()).isNull()

      verify(localValueStore).isDeferredDeeplinkConsumed = true
    }

  @Test
  fun `getDeferredSurveyId marks the referrer consumed when it is empty`() = runTest {
    setUpReferrer("")

    assertThat(service.getDeferredSurveyId()).isNull()

    verify(localValueStore).isDeferredDeeplinkConsumed = true
  }

  @Test
  fun `getDeferredSurveyId marks the referrer consumed when the feature isn't supported`() =
    runTest {
      setUpSetupFinished(InstallReferrerResponse.FEATURE_NOT_SUPPORTED)

      assertThat(service.getDeferredSurveyId()).isNull()

      verify(localValueStore).isDeferredDeeplinkConsumed = true
    }

  @Test
  fun `getDeferredSurveyId keeps the referrer eligible when the service is unavailable`() =
    runTest {
      setUpSetupFinished(InstallReferrerResponse.SERVICE_UNAVAILABLE)

      assertThat(service.getDeferredSurveyId()).isNull()

      verify(localValueStore, never()).isDeferredDeeplinkConsumed = true
    }

  @Test
  fun `getDeferredSurveyId keeps the referrer eligible when setup reports a developer error`() =
    runTest {
      setUpSetupFinished(InstallReferrerResponse.DEVELOPER_ERROR)

      assertThat(service.getDeferredSurveyId()).isNull()

      verify(localValueStore, never()).isDeferredDeeplinkConsumed = true
    }

  @Test
  fun `getDeferredSurveyId returns null when reading the referrer throws`() = runTest {
    setUpSetupFinished(InstallReferrerResponse.OK)
    whenever(client.installReferrer).thenThrow(RemoteException())

    assertThat(service.getDeferredSurveyId()).isNull()

    verify(localValueStore, never()).isDeferredDeeplinkConsumed = true
  }

  @Test
  fun `getDeferredSurveyId returns null when starting the connection throws`() = runTest {
    doThrow(SecurityException("no play store")).whenever(client).startConnection(any())

    assertThat(service.getDeferredSurveyId()).isNull()

    verify(localValueStore, never()).isDeferredDeeplinkConsumed = true
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `getDeferredSurveyId gives up on the query after three seconds`() = runTest {
    doAnswer {}.whenever(client).startConnection(any())
    val start = testScheduler.currentTime

    service.getDeferredSurveyId()

    assertThat(testScheduler.currentTime - start).isEqualTo(3000L)
  }

  @Test
  fun `getDeferredSurveyId closes the connection after a successful read`() = runTest {
    setUpReferrer("survey_id=$SURVEY_ID")

    service.getDeferredSurveyId()

    verify(client).endConnection()
  }

  @Test
  fun `getDeferredSurveyId closes the connection after a timeout`() = runTest {
    doAnswer {}.whenever(client).startConnection(any())

    service.getDeferredSurveyId()

    verify(client).endConnection()
  }

  @Test
  fun `getDeferredSurveyId returns null once the referrer has been consumed`() = runTest {
    whenever(localValueStore.isDeferredDeeplinkConsumed).thenReturn(true)

    assertThat(service.getDeferredSurveyId()).isNull()
    verify(client, never()).startConnection(any())
    staticClient.verify({ InstallReferrerClient.newBuilder(any<Context>()) }, never())
  }

  private fun setUpSetupFinished(responseCode: Int) {
    doAnswer { invocation ->
        invocation
          .getArgument<InstallReferrerStateListener>(0)
          .onInstallReferrerSetupFinished(responseCode)
      }
      .whenever(client)
      .startConnection(any())
  }

  private fun setUpReferrer(referrer: String) {
    setUpSetupFinished(InstallReferrerResponse.OK)
    val details: ReferrerDetails = mock()
    whenever(details.installReferrer).thenReturn(referrer)
    whenever(client.installReferrer).thenReturn(details)
  }

  companion object {
    private const val SURVEY_ID = "survey_123"
  }
}
