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
package org.groundplatform.android.data.local

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData.SURVEY
import org.groundplatform.android.data.local.stores.LocalSurveyStore
import org.groundplatform.android.data.local.stores.LocalSurveySyncStateStore
import org.groundplatform.domain.model.Survey
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocalSurveySyncStateStoreTest : BaseHiltTest() {

  @Inject lateinit var localSurveyStore: LocalSurveyStore
  @Inject lateinit var localSurveySyncStateStore: LocalSurveySyncStateStore

  @Test
  fun `get returns null for a survey that was never synced`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    assertThat(localSurveySyncStateStore.get(SURVEY.id)).isNull()
  }

  @Test
  fun `recordFullSync stores the sync state`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    localSurveySyncStateStore.recordFullSync(
      SURVEY.id,
      latestLoiServerTimestamp = 1_234L,
      dataVisibility = Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS,
    )

    val state = localSurveySyncStateStore.get(SURVEY.id)!!
    assertThat(state.surveyId).isEqualTo(SURVEY.id)
    assertThat(state.latestLoiServerTimestamp).isEqualTo(1_234L)
    assertThat(state.syncedDataVisibility).isEqualTo(Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS)
  }

  @Test
  fun `recordFullSync stamps the client timestamp with the current time`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    val before = System.currentTimeMillis()
    localSurveySyncStateStore.recordFullSync(SURVEY.id, 0L, null)
    val after = System.currentTimeMillis()

    val recorded = localSurveySyncStateStore.get(SURVEY.id)!!.lastFullSyncClientTimestamp
    assertThat(recorded).isAtLeast(before)
    assertThat(recorded).isAtMost(after)
  }

  @Test
  fun `recordFullSync round trips every data visibility`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)

    for (dataVisibility in Survey.DataVisibility.entries) {
      localSurveySyncStateStore.recordFullSync(SURVEY.id, 1L, dataVisibility)

      assertThat(localSurveySyncStateStore.get(SURVEY.id)!!.syncedDataVisibility)
        .isEqualTo(dataVisibility)
    }
  }

  @Test
  fun `recordFullSync replaces the state of an already synced survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    localSurveySyncStateStore.recordFullSync(
      SURVEY.id,
      10L,
      Survey.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS,
    )
    val firstSync = localSurveySyncStateStore.get(SURVEY.id)!!.lastFullSyncClientTimestamp

    localSurveySyncStateStore.recordFullSync(
      SURVEY.id,
      20L,
      Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS,
    )

    val state = localSurveySyncStateStore.get(SURVEY.id)!!
    assertThat(state.latestLoiServerTimestamp).isEqualTo(20L)
    assertThat(state.syncedDataVisibility).isEqualTo(Survey.DataVisibility.ALL_SURVEY_PARTICIPANTS)
    assertThat(state.lastFullSyncClientTimestamp).isAtLeast(firstSync)
  }

  @Test
  fun `recordIncrementalSync only advances the LOI timestamp`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    localSurveySyncStateStore.recordFullSync(
      SURVEY.id,
      10L,
      Survey.DataVisibility.CONTRIBUTOR_AND_ORGANIZERS,
    )
    val fullSyncState = localSurveySyncStateStore.get(SURVEY.id)!!

    localSurveySyncStateStore.recordIncrementalSync(SURVEY.id, 20L)

    assertThat(localSurveySyncStateStore.get(SURVEY.id))
      .isEqualTo(fullSyncState.copy(latestLoiServerTimestamp = 20L))
  }

  @Test
  fun `recordIncrementalSync does nothing for a survey that was never fully synced`() =
    runWithTestDispatcher {
      localSurveyStore.insertOrUpdateSurvey(SURVEY)

      localSurveySyncStateStore.recordIncrementalSync(SURVEY.id, 20L)

      assertThat(localSurveySyncStateStore.get(SURVEY.id)).isNull()
    }

  @Test
  fun `sync state is tracked per survey`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    localSurveyStore.insertOrUpdateSurvey(OTHER_SURVEY)

    localSurveySyncStateStore.recordFullSync(SURVEY.id, 10L, null)
    localSurveySyncStateStore.recordFullSync(OTHER_SURVEY.id, 20L, null)
    localSurveySyncStateStore.recordIncrementalSync(SURVEY.id, 30L)

    assertThat(localSurveySyncStateStore.get(SURVEY.id)!!.latestLoiServerTimestamp).isEqualTo(30L)
    assertThat(localSurveySyncStateStore.get(OTHER_SURVEY.id)!!.latestLoiServerTimestamp)
      .isEqualTo(20L)
  }

  @Test
  fun `deleting a survey discards its sync state`() = runWithTestDispatcher {
    localSurveyStore.insertOrUpdateSurvey(SURVEY)
    localSurveySyncStateStore.recordFullSync(SURVEY.id, 10L, null)

    localSurveyStore.deleteSurvey(SURVEY)

    assertThat(localSurveySyncStateStore.get(SURVEY.id)).isNull()
  }

  companion object {
    private val OTHER_SURVEY = SURVEY.copy(id = "other survey id")
  }
}
