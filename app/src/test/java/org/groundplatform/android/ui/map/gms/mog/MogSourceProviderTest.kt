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
package org.groundplatform.android.ui.map.gms.mog

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MogSourceProviderTest {

  @Test
  fun `Provides 2 default MOG sources`() {
    assertThat(MogSourceProvider.defaultMogSources).hasSize(2)
  }

  @Test
  fun `Validate zoom range and path template for overview tile source`() {
    val overviewSource = MogSourceProvider.defaultMogSources[0]
    assertThat(overviewSource.zoomRange).isEqualTo(0..7)
    assertThat(overviewSource.pathTemplate).isEqualTo("/offline-imagery/default/8/overview.tif")
  }

  @Test
  fun `Validate zoom range and path template for geolocated tile source`() {
    val tileSource = MogSourceProvider.defaultMogSources[1]
    assertThat(tileSource.zoomRange).isEqualTo(8..14)
    assertThat(tileSource.pathTemplate).isEqualTo("/offline-imagery/default/8/{x}/{y}.tif")
  }

  @Test
  fun defaultMogSources_maxZoomIsCorrect() {
    assertThat(MogSourceProvider.DEFAULT_MOG_MAX_ZOOM).isEqualTo(14)
  }
}
