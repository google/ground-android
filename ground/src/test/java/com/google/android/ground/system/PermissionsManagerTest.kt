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
package com.google.android.ground.system

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.android.ground.BaseHiltTest
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowApplication

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PermissionsManagerTest : BaseHiltTest() {

  private val testPermission = Manifest.permission.ACCESS_COARSE_LOCATION

  @Inject lateinit var permissionsManager: PermissionsManager

  @Test
  fun permissionAvailable_granted() {
    val shadowApplication = extract<ShadowApplication>(getInstrumentation().targetContext)
    shadowApplication.grantPermissions(testPermission)

    permissionsManager.obtainPermission(testPermission).test().assertComplete()
  }

  @Test
  fun permissionNotAvailable_granted() {
    permissionsManager.obtainPermission(testPermission).test().assertNotComplete()
  }
}
