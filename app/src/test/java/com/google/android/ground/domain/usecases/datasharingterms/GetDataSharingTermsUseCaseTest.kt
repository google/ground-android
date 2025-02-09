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
package com.google.android.ground.domain.usecases.datasharingterms

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData.SURVEY
import com.google.android.ground.domain.usecases.survey.ActivateSurveyUseCase
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.remote.FakeRemoteDataStore
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetDataSharingTermsUseCaseTest : BaseHiltTest() {
  @Inject
  lateinit var activateSurvey: ActivateSurveyUseCase
  @Inject
  lateinit var fakeRemoteDataStore: FakeRemoteDataStore
  @Inject
  lateinit var getDataSharingTermsUseCase: GetDataSharingTermsUseCase
  @Inject
  lateinit var localValueStore: LocalValueStore

  @Before
  override fun setUp() {
    super.setUp()
    fakeRemoteDataStore.surveys = listOf(SURVEY)
  }

  @Test
  fun `Returns null if no survey active`() = runWithTestDispatcher {
    assertThat(getDataSharingTermsUseCase()).isNull()
  }

  @Test
  fun `Returns null if data sharing terms is already accepted`() = runWithTestDispatcher {
    activateSurvey(SURVEY.id)
    localValueStore.setDataSharingConsent(SURVEY.id, true)

    assertThat(getDataSharingTermsUseCase()).isNull()
  }

  @Test
  fun `Returns data sharing terms if not already accepted`() = runWithTestDispatcher {
    activateSurvey(SURVEY.id)

    assertThat(getDataSharingTermsUseCase()).isEqualTo(SURVEY.dataSharingTerms)
  }
}
