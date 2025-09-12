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
package org.groundplatform.android.usecases.datasharingterms

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.data.remote.FakeRemoteDataStore
import org.groundplatform.android.model.Survey
import org.groundplatform.android.proto.Survey.DataSharingTerms
import org.groundplatform.android.proto.SurveyKt.dataSharingTerms
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetDataSharingTermsUseCaseTest : BaseHiltTest() {
  @Inject lateinit var activateSurveyUseCase: ActivateSurveyUseCase
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject lateinit var getDataSharingTermsUseCase: GetDataSharingTermsUseCase
  @Inject lateinit var localValueStore: LocalValueStore

  private fun activateSurvey(survey: Survey) = runWithTestDispatcher {
    fakeRemoteDataStore.surveys = listOf(survey)
    activateSurveyUseCase(survey.id)
  }

  @Test
  fun `Fails with exception if no survey active`() {
    val result = getDataSharingTermsUseCase()

    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    assertThat(result.exceptionOrNull()?.message).isEqualTo("No active survey")
  }

  @Test
  fun `Fails with custom exception if custom data sharing terms are invalid`() {
    val survey =
      SURVEY.copy(
        dataSharingTerms =
          dataSharingTerms {
            type = DataSharingTerms.Type.CUSTOM
            customText = ""
          }
      )
    activateSurvey(survey)

    val result = getDataSharingTermsUseCase()

    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull())
      .isInstanceOf(GetDataSharingTermsUseCase.InvalidCustomSharingTermsException::class.java)
  }

  @Test
  fun `Succeeds with null if data sharing terms is already accepted`() {
    activateSurvey(SURVEY)
    localValueStore.setDataSharingConsent(SURVEY.id, true)

    val result = getDataSharingTermsUseCase()

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isNull()
  }

  @Test
  fun `Succeeds with null if data sharing terms is missing`() {
    activateSurvey(SURVEY.copy(dataSharingTerms = null))

    val result = getDataSharingTermsUseCase()

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isNull()
  }

  @Test
  fun `Succeeds with data sharing terms if not already accepted`() {
    activateSurvey(SURVEY)

    val result = getDataSharingTermsUseCase()

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrNull()).isEqualTo(SURVEY.dataSharingTerms)
  }
}
