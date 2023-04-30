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

package com.google.android.ground

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelStore
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider.getApplicationContext

/** Helper class for creating [TestNavHostController] for tests. */
object NavControllerTestUtil {

  // TODO: Refactor existing tests to use this util.

  /** Creates a [TestNavHostController]. */
  fun createTestNavController(destId: Int, argsBundle: Bundle?): TestNavHostController {
    val navController = TestNavHostController(getApplicationContext())
    navController.setViewModelStore(ViewModelStore()) // Required for graph scoped view models.
    navController.setGraph(R.navigation.nav_graph)
    navController.setCurrentDestination(destId, argsBundle ?: bundleOf())
    return navController
  }
}
