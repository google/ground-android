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

package org.groundplatform.domain.model.imagery

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

const val TEST_WORLD_URL = "world_url/5/world.tif"
const val TEST_HIGH_RES_URL = "high_res_url/5/{x}/{y}.tif"
const val TEST_HIGH_RES_MIN_ZOOM = 5
const val TEST_HIGH_RES_MAX_ZOOM = 14
val MOG_SOURCE_0_TO_4 = MogSource(IntRange(0, TEST_HIGH_RES_MIN_ZOOM - 1), TEST_WORLD_URL)
val MOG_SOURCE_5_TO_14 =
  MogSource(IntRange(TEST_HIGH_RES_MIN_ZOOM, TEST_HIGH_RES_MAX_ZOOM), TEST_HIGH_RES_URL)

class MogCollectionTest {

  private lateinit var mogCollection: MogCollection

  @BeforeTest
  fun setUp() {
    mogCollection = MogCollection(listOf(MOG_SOURCE_0_TO_4, MOG_SOURCE_5_TO_14))
  }

  @Test
  fun `getMogSource returns source for valid zoom level 1`() {
    val mogSource = mogCollection.getMogSource(4)
    assertEquals(MOG_SOURCE_0_TO_4, mogSource)
  }

  @Test
  fun `getMogSource returns source for valid zoom level 2`() {
    val mogSource = mogCollection.getMogSource(5)
    assertEquals(MOG_SOURCE_5_TO_14, mogSource)
  }

  @Test
  fun `getMogSource returns null for invalid zoom level`() {
    val mogSource = mogCollection.getMogSource(15)
    assertNull(mogSource)
  }
}
