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
import kotlin.test.assertFailsWith
import org.junit.Test

class SurveyTest {

  private val ownerEmail = "owner_email@gmail.com"
  private val viewerEmail = "viewer_email@gmail.com"
  private val dataCollectorEmail = "data_collector_email@gmail.com"
  private val surveyOrganizerEmail = "survey_organizer_email@gmail.com"
  private val badRoleEmail = "random_email@gmail.com"
  private val testSurvey =
    Survey(
      id = "survey 1",
      title = "Survey title",
      description = "Survey description",
      jobMap = mapOf(),
      tileSources = listOf(),
      acl =
        mapOf(
          Pair(ownerEmail, Role.OWNER.toString()),
          Pair(viewerEmail, Role.VIEWER.toString()),
          Pair(dataCollectorEmail, Role.DATA_COLLECTOR.toString()),
          Pair(surveyOrganizerEmail, Role.SURVEY_ORGANIZER.toString()),
          Pair(badRoleEmail, "UNKNOWN_ROLE")
        )
    )

  @Test
  fun `getRole() returns OWNER`() {
    assertThat(testSurvey.getRole(ownerEmail)).isEqualTo(Role.OWNER)
  }

  @Test
  fun `getRole() returns VIEWER`() {

    assertThat(testSurvey.getRole(viewerEmail)).isEqualTo(Role.VIEWER)
  }

  @Test
  fun `getRole() returns DATA_COLLECTOR`() {

    assertThat(testSurvey.getRole(dataCollectorEmail)).isEqualTo(Role.DATA_COLLECTOR)
  }

  @Test
  fun `getRole() returns SURVEY_ORGANIZER`() {

    assertThat(testSurvey.getRole(surveyOrganizerEmail)).isEqualTo(Role.SURVEY_ORGANIZER)
  }

  @Test
  fun `getRole() throws error on unknown email`() {
    assertFailsWith<IllegalStateException> { testSurvey.getRole("") }
  }

  @Test
  fun `getRole() throws error on unsupported role`() {
    assertFailsWith<IllegalStateException>("") { testSurvey.getRole(badRoleEmail) }
  }
}
