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
package org.groundplatform.android.system

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFailsWith
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.system.PermissionsManager.Companion.PERMISSIONS_REQUEST_CODE
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowApplication

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PermissionsManagerTest : BaseHiltTest() {

  private val testPermission = Manifest.permission.ACCESS_COARSE_LOCATION

  @BindValue @Mock lateinit var activityStreamsMock: ActivityStreams
  @Inject lateinit var permissionsManager: PermissionsManager

  @Test
  fun `Permission is available when granted`() = runWithTestDispatcher {
    val shadowApplication = extract<ShadowApplication>(getInstrumentation().targetContext)
    shadowApplication.grantPermissions(testPermission)

    permissionsManager.obtainPermission(testPermission)
  }

  @Test
  fun `Permission is not available when granted`() = runWithTestDispatcher {
    setupPermissionResult(true)

    permissionsManager.obtainPermission(testPermission)
  }

  @Test
  fun `Permission is not available and not granted fails`() = runWithTestDispatcher {
    setupPermissionResult(false)

    assertFailsWith<PermissionDeniedException> {
      permissionsManager.obtainPermission(testPermission)
    }
  }

  private fun setupPermissionResult(granted: Boolean) = runWithTestDispatcher {
    whenever(activityStreamsMock.getNextRequestPermissionsResult(PERMISSIONS_REQUEST_CODE))
      .thenReturn(
        RequestPermissionsResult(
          PERMISSIONS_REQUEST_CODE,
          arrayOf(testPermission),
          intArrayOf(
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
          ),
        )
      )
  }
}
