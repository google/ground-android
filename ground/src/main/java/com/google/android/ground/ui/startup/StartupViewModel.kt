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
package com.google.android.ground.ui.startup

import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.GoogleApiManager
import com.google.android.ground.ui.common.AbstractViewModel
import javax.inject.Inject

class StartupViewModel
@Inject
internal constructor(
  private val googleApiManager: GoogleApiManager,
  private val userRepository: UserRepository
) : AbstractViewModel() {

  /** Checks & installs Google Play Services and initializes the login flow. */
  suspend fun initializeLogin() {
    googleApiManager.installGooglePlayServices()
    userRepository.init()
  }
}
