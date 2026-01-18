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
package org.groundplatform.android.ui.startup

import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import org.groundplatform.android.R
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.GoogleApiManager
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StartupViewModel
@Inject
internal constructor(
  private val googleApiManager: GoogleApiManager,
  private val userRepository: UserRepository,
  val popups: EphemeralPopups,
) : AbstractViewModel() {

  /** Initializes the login flow, installing Google Play Services if necessary. */
  suspend fun initializeLogin() {
    googleApiManager.installGooglePlayServices()
    userRepository.init()
  }

  fun maybeDisplayError(throwable: Throwable) {
    Timber.e(throwable, "Failed to launch app")
    if (throwable is GooglePlayServicesNotAvailableException) {
      popups.ErrorPopup().show(R.string.google_api_install_failed)
    }
  }
}
