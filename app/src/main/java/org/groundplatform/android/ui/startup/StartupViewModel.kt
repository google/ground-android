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

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.R
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.GoogleApiManager
import org.groundplatform.android.ui.common.AbstractViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import timber.log.Timber

/** Represents the different states the application can be in during its initial startup flow. */
sealed interface StartupState {
  data object Loading : StartupState

  data class Error(@StringRes val errorMessageId: Int?) : StartupState
}

/** ViewModel responsible for the initial app startup flow. */
@HiltViewModel
class StartupViewModel
@Inject
internal constructor(
  private val googleApiManager: GoogleApiManager,
  private val userRepository: UserRepository,
  val popups: EphemeralPopups,
) : AbstractViewModel() {

  private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
  val state: StateFlow<StartupState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        googleApiManager.installGooglePlayServices()
        userRepository.init()
      } catch (t: Throwable) {
        Timber.e(t, "Failed to launch app")
        _state.value = StartupState.Error(getErrorMessageId(t))
      }
    }
  }

  private fun getErrorMessageId(throwable: Throwable): Int? {
    if (throwable is GooglePlayServicesNotAvailableException) {
      return R.string.google_api_install_failed
    }
    return null
  }
}
