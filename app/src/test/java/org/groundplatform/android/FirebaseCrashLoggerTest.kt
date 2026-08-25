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
package org.groundplatform.android

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.groundplatform.android.common.Constants
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class FirebaseCrashLoggerTest {

  private val crashlytics: FirebaseCrashlytics = mock()
  private val firebaseCrashLogger = FirebaseCrashLogger()
  private lateinit var mockCrashlyticsStatic: MockedStatic<FirebaseCrashlytics>
  private lateinit var mockConstantsStatic: MockedStatic<Constants>

  @Before
  fun setup() {
    mockCrashlyticsStatic =
      mockStatic(FirebaseCrashlytics::class.java).apply {
        `when`<FirebaseCrashlytics> { FirebaseCrashlytics.getInstance() }.thenReturn(crashlytics)
      }
    mockConstantsStatic =
      mockStatic(Constants::class.java).apply {
        `when`<Boolean> { Constants.isReleaseBuild() }.thenReturn(true)
      }
  }

  @After
  fun tearDown() {
    mockCrashlyticsStatic.close()
    mockConstantsStatic.close()
  }

  @Test
  fun recordException_logsMessage() {
    firebaseCrashLogger.recordException(Log.INFO, "Test message", null)
    verify(crashlytics).log("Test message")
  }

  @Test
  fun recordException_withErrorAndThrowable_recordsException() {
    val t = RuntimeException("Test exception")
    firebaseCrashLogger.recordException(Log.ERROR, "Error message", t)
    verify(crashlytics).recordException(t)
  }

  @Test
  fun recordException_withoutThrowable_doesNotRecordException() {
    firebaseCrashLogger.recordException(Log.ERROR, "Error message", null)
    verify(crashlytics, never()).recordException(any())
  }

  @Test
  fun recordException_withInfoAndThrowable_doesNotRecordException() {
    val t = RuntimeException("Test exception")
    firebaseCrashLogger.recordException(Log.INFO, "Info message", t)
    verify(crashlytics, never()).recordException(t)
  }

  @Test
  fun setSelectedSurveyId_setsCustomKey() {
    firebaseCrashLogger.setSelectedSurveyId("survey-123")
    verify(crashlytics).setCustomKey("selectedSurveyId", "survey-123")
  }

  @Test
  fun setSelectedSurveyId_withNull_setsEmptyString() {
    firebaseCrashLogger.setSelectedSurveyId(null)
    verify(crashlytics).setCustomKey("selectedSurveyId", "")
  }

  @Test
  fun setScreenName_setsCustomKey() {
    firebaseCrashLogger.setScreenName("MyScreen")
    verify(crashlytics).setCustomKey("screenName", "MyScreen")
  }

  @Test
  fun setSelectedSurveyId_inDebug_doesNotSetCustomKey() {
    mockConstantsStatic.`when`<Boolean> { Constants.isReleaseBuild() }.thenReturn(false)
    firebaseCrashLogger.setSelectedSurveyId("survey-123")
    verify(crashlytics, never()).setCustomKey("selectedSurveyId", "survey-123")
  }
}
