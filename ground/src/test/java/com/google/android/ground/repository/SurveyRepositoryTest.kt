/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.repository

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.Survey
import com.google.android.ground.persistence.local.LocalDataStore
import com.google.android.ground.persistence.local.LocalDataStoreModule
import com.google.common.collect.ImmutableMap
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.Maybe
import java8.util.Optional
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@UninstallModules(LocalDataStoreModule::class)
@RunWith(RobolectricTestRunner::class)
class SurveyRepositoryTest : BaseHiltTest() {
  @BindValue @Mock lateinit var mockLocalDataStore: LocalDataStore

  @Inject lateinit var surveyRepository: SurveyRepository

  @Test
  fun testActivateSurvey() {
    val survey = Survey("", "", "", ImmutableMap.of())
    setTestSurvey(survey)
    surveyRepository.activateSurvey("id")
    surveyRepository.activeSurvey.test().assertValue(Optional.of(survey))
  }

  private fun setTestSurvey(survey: Survey) {
    Mockito.`when`(mockLocalDataStore.getSurveyById(anyString())).thenReturn(Maybe.just(survey))
  }
}
