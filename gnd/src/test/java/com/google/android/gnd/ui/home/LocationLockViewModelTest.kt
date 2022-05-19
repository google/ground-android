/*
 * Copyright 2022 Google LLC
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

package com.google.android.gnd.ui.home

import com.google.android.gnd.BaseHiltTest
import com.google.android.gnd.R
import com.google.android.gnd.rx.BooleanOrError
import com.jraska.livedata.TestObserver
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class LocationLockViewModelTest : BaseHiltTest() {

    @Inject
    lateinit var viewModel: LocationLockViewModel

    private lateinit var locationLockEnabledObserver: TestObserver<Boolean>
    private lateinit var locationLockIconTintObserver: TestObserver<Int>
    private lateinit var locationLockStateObserver: TestObserver<BooleanOrError>
    private lateinit var locationLockUpdatesEnabledObserver: TestObserver<Boolean>

    override fun setUp() {
        super.setUp()

        locationLockEnabledObserver = TestObserver.test(viewModel.locationLockEnabled)
        locationLockIconTintObserver = TestObserver.test(viewModel.locationLockIconTint)
        locationLockStateObserver = TestObserver.test(viewModel.locationLockState)
        locationLockUpdatesEnabledObserver = TestObserver.test(viewModel.locationUpdatesEnabled)
    }

    @Test
    fun setLocationLockEnabled() {
        viewModel.setLocationLockEnabled(false)
        locationLockEnabledObserver.assertValue(false)

        viewModel.setLocationLockEnabled(true)
        locationLockEnabledObserver.assertValue(true)
    }

    @Test
    fun requestLocationLock() {
        viewModel.requestLocationLock()

        assertLocationLocked()
    }

    @Test
    fun releaseLocationLock() {
        viewModel.requestLocationLock()
        viewModel.releaseLocationLock()

        assertLocationLockReleased()
    }

    @Test
    fun toggleLocationLock() {
        viewModel.toggleLocationLock()

        assertLocationLocked()

        viewModel.toggleLocationLock()

        assertLocationLockReleased()
    }

    private fun assertLocationLocked() {
        locationLockIconTintObserver.assertValue(R.color.colorMapBlue)
        locationLockStateObserver.assertValue { it.isTrue }
        locationLockUpdatesEnabledObserver.assertValue(true)
    }

    private fun assertLocationLockReleased() {
        locationLockIconTintObserver.assertValue(R.color.colorGrey800)
        locationLockStateObserver.assertValue { !it.isTrue }
        locationLockUpdatesEnabledObserver.assertValue(false)
    }
}
