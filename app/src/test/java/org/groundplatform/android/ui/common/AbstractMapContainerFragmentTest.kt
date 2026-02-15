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
package org.groundplatform.android.ui.common

import org.groundplatform.android.ui.map.MapFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AbstractMapContainerFragmentTest {

  @Mock
  lateinit var mapFragment: MapFragment

  @Mock
  lateinit var viewModel: BaseMapViewModel

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `onDestroyView disables current location indicator`() {
    val fragment = TestMapContainerFragment(viewModel)
    fragment.map = mapFragment
    
    // Call onDestroyView
    fragment.onDestroyView()
    
    verify(mapFragment).disableCurrentLocationIndicator()
  }

  // Concrete implementation for testing
  class TestMapContainerFragment(private val viewModel: BaseMapViewModel) : AbstractMapContainerFragment() {
    override fun getMapViewModel(): BaseMapViewModel = viewModel
  }
}

