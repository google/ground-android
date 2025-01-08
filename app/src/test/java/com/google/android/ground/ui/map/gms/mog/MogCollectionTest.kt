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
package com.google.android.ground.ui.map.gms.mog

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

const val TEST_WORLD_URL = "world_url/5/world.tif"
const val TEST_HIGH_RES_URL = "high_res_url/5/{x}/{y}.tif"
const val TEST_HIGH_RES_MIN_ZOOM = 5
const val TEST_HIGH_RES_MAX_ZOOM = 14
val MOG_SOURCE_0_TO_4 = MogSource(IntRange(0, TEST_HIGH_RES_MIN_ZOOM - 1), TEST_WORLD_URL)
val MOG_SOURCE_5_TO_14 =
  MogSource(IntRange(TEST_HIGH_RES_MIN_ZOOM, TEST_HIGH_RES_MAX_ZOOM), TEST_HIGH_RES_URL)

@RunWith(MockitoJUnitRunner::class)
class MogCollectionTest {

  private lateinit var mogCollection: MogCollection

  @Before
  fun setUp() {
    mogCollection = MogCollection(listOf(MOG_SOURCE_0_TO_4, MOG_SOURCE_5_TO_14))
  }

  @Test
  fun getMogSource_validZoom1_returnsSource() {
    val mogSource = mogCollection.getMogSource(4)
    assertThat(mogSource).isEqualTo(MOG_SOURCE_0_TO_4)
  }

  @Test
  fun getMogSource_validZoom2_returnsSource() {
    val mogSource = mogCollection.getMogSource(5)
    assertThat(mogSource).isEqualTo(MOG_SOURCE_5_TO_14)
  }

  @Test
  fun getMogSource_invalidZoom_returnsNull() {
    val mogSource = mogCollection.getMogSource(15)
    assertThat(mogSource).isNull()
  }
}
