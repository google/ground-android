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
package com.google.android.ground.ui.home.mapcontainer

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.locationofinterest.Point.Companion.zero
import com.google.android.ground.rx.Nil
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationOfInterestRepositionViewModelTest : BaseHiltTest() {
    @Inject
    lateinit var viewModel: LocationOfInterestRepositionViewModel

    @Test
    fun testConfirmButtonClicks_notReplayed() {
        viewModel.onCameraMoved(TEST_POINT)

        viewModel.onConfirmButtonClick()

        viewModel.confirmButtonClicks.test().assertNoValues().assertNoErrors().assertNotComplete()
    }

    @Test
    fun testConfirmButtonClicks() {
        viewModel.onCameraMoved(TEST_POINT)
        val testObserver = viewModel.confirmButtonClicks.test()

        viewModel.onConfirmButtonClick()

        testObserver.assertValue(TEST_POINT).assertNoErrors().assertNotComplete()
    }

    @Test
    fun testCancelButtonClicks_notReplayed() {
        viewModel.onCancelButtonClick()

        viewModel.cancelButtonClicks.test().assertNoValues().assertNoErrors().assertNotComplete()
    }

    @Test
    fun testCancelButtonClicks() {
        val testObserver = viewModel.cancelButtonClicks.test()

        viewModel.onCancelButtonClick()

        testObserver.assertValue(Nil.NIL).assertNoErrors().assertNotComplete()
    }

    companion object {
        private val TEST_POINT = zero()
    }
}