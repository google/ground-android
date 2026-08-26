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
package org.groundplatform.domain.model.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapConfigTest {

  @Test
  fun `default values are set correctly`() {
    val config = MapConfig()

    assertTrue(config.showOfflineImagery)
    assertNull(config.overrideMapType)
    assertTrue(config.allowGestures)
    assertTrue(config.allowRotateGestures)
  }

  @Test
  fun `custom values are set correctly`() {
    val config =
      MapConfig(
        showOfflineImagery = false,
        overrideMapType = MapType.SATELLITE,
        allowGestures = false,
        allowRotateGestures = false,
      )

    assertFalse(config.showOfflineImagery)
    assertEquals(MapType.SATELLITE, config.overrideMapType)
    assertFalse(config.allowGestures)
    assertFalse(config.allowRotateGestures)
  }

  @Test
  fun `copy updates specified properties and keeps others`() {
    val initial = MapConfig()
    val updated = initial.copy(allowGestures = false, overrideMapType = MapType.TERRAIN)

    assertTrue(updated.showOfflineImagery)
    assertEquals(MapType.TERRAIN, updated.overrideMapType)
    assertFalse(updated.allowGestures)
    assertTrue(updated.allowRotateGestures)
  }
}
