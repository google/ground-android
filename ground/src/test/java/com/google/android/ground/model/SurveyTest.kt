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
package com.google.android.ground.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SurveyTest {

  private val ownerEmail = "owner_email@gmail.com"
  private val viewerEmail = "viewer_email@gmail.com"
  private val dataCollectorEmail = "data_collector_email@gmail.com"
  private val surveyOrganizerEmail = "survey_organizer_email@gmail.com"
  private val randomEmail = "random_email@gmail.com"
  private val testSurvey =
    Survey(
      id = "survey 1",
      title = "Survey title",
      description = "Survey description",
      jobMap = mapOf(),
      tileSources = listOf(),
      acl =
        mapOf(
          Pair(ownerEmail, "owner"),
          Pair(viewerEmail, "viewer"),
          Pair(dataCollectorEmail, "data-collector"),
          Pair(surveyOrganizerEmail, "survey-organizer"),
          Pair(randomEmail, "random-acl")
        )
    )

  @Test
  fun getRole() {
    // known acl
    assertThat(testSurvey.getRole(ownerEmail)).isEqualTo(Role.OWNER)
    assertThat(testSurvey.getRole(viewerEmail)).isEqualTo(Role.VIEWER)
    assertThat(testSurvey.getRole(dataCollectorEmail)).isEqualTo(Role.DATA_COLLECTOR)
    assertThat(testSurvey.getRole(surveyOrganizerEmail)).isEqualTo(Role.SURVEY_ORGANIZER)

    // unknown acl
    assertThrows(IllegalStateException::class.java) { testSurvey.getRole(randomEmail) }

    // missing email
    assertThrows(IllegalStateException::class.java) { testSurvey.getRole("") }
  }
}
