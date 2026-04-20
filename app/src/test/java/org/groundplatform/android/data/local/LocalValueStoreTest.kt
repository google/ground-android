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

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.groundplatform.android.FakeData
import org.groundplatform.android.common.PrefKeys
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.map.Bounds
import org.groundplatform.domain.model.map.CameraPosition
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalValueStoreTest {

  private lateinit var sharedPreferences: SharedPreferences
  private lateinit var localValueStore: LocalValueStore

  @Before
  fun setUp() {
    sharedPreferences =
      ApplicationProvider.getApplicationContext<android.content.Context>()
        .getSharedPreferences("test_prefs", android.content.Context.MODE_PRIVATE)
    sharedPreferences.edit { clear() }
    localValueStore = LocalValueStore(sharedPreferences, Locale.getDefault())
  }

  @Test
  fun `stores camera position and its coordinates correctly`() {
    val position = CameraPosition(coordinates = COORDINATES)

    localValueStore.setLastCameraPosition(SURVEY_ID, position)

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isEqualTo(position)
  }

  @Test
  fun `stores camera position and zoom level correctly`() {
    val position = CameraPosition(coordinates = COORDINATES, zoomLevel = ZOOM_LEVEL)

    localValueStore.setLastCameraPosition(SURVEY_ID, position)

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isEqualTo(position)
  }

  @Test
  fun `stores camera position and bounds correctly`() {
    val position = CameraPosition(coordinates = COORDINATES, bounds = BOUNDS)

    localValueStore.setLastCameraPosition(SURVEY_ID, position)

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isEqualTo(position)
  }

  @Test
  fun `stores camera position and all its fields correctly`() {
    val position = CameraPosition(COORDINATES, ZOOM_LEVEL, BOUNDS)

    localValueStore.setLastCameraPosition(SURVEY_ID, position)

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isEqualTo(position)
  }

  @Test
  fun `getLastCameraPosition returns null when the preference doesn't exist`() {
    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isNull()
  }

  @Test
  fun `getLastCameraPosition returns null when stored value is malformed`() {
    sharedPreferences.edit {
      putString(PrefKeys.LAST_VIEWPORT_PREFIX + SURVEY_ID, "Invalid camera position")
    }

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isNull()
  }

  @Test
  fun `getLastCameraPosition returns null when coordinates are not numeric`() {
    sharedPreferences.edit {
      putString(
        PrefKeys.LAST_VIEWPORT_PREFIX + SURVEY_ID,
        "not_a_number, not_a_number, 13.0, null, null, null, null",
      )
    }

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isNull()
  }

  @Test
  fun `getLastCameraPosition returns null when serialized value has missing parts`() {
    sharedPreferences.edit { putString(PrefKeys.LAST_VIEWPORT_PREFIX + SURVEY_ID, "1.0, 2.0") }

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isNull()
  }

  @Test
  fun `clearLastCameraPosition removes stored value`() {
    localValueStore.setLastCameraPosition(SURVEY_ID, CameraPosition(COORDINATES, ZOOM_LEVEL, BOUNDS))

    localValueStore.clearLastCameraPosition(SURVEY_ID)

    assertThat(localValueStore.getLastCameraPosition(SURVEY_ID)).isNull()
  }

  @Test
  fun `camera positions are scoped per survey`() {
    val a = CameraPosition(COORDINATES, ZOOM_LEVEL)
    val b = CameraPosition(COORDINATES, zoomLevel = 5.0f)

    localValueStore.setLastCameraPosition("survey-a", a)
    localValueStore.setLastCameraPosition("survey-b", b)

    assertThat(localValueStore.getLastCameraPosition("survey-a")).isEqualTo(a)
    assertThat(localValueStore.getLastCameraPosition("survey-b")).isEqualTo(b)
  }

  companion object {
    private const val SURVEY_ID = "survey-1"
    private val BOUNDS = Bounds(-10.0, -20.0, 10.0, 20.0)
    private val COORDINATES = Coordinates(-1.0, 2.0)
    private const val ZOOM_LEVEL = 13.0f
  }
}
