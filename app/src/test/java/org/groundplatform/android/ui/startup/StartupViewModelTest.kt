/*
 * Copyright 2026 Google LLC
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

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.GoogleApiManager
import org.groundplatform.android.ui.common.EphemeralPopups
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class StartupViewModelTest : BaseHiltTest() {

  @Mock private lateinit var googleApiManager: GoogleApiManager
  @Mock private lateinit var userRepository: UserRepository
  @Mock private lateinit var popups: EphemeralPopups

  private lateinit var viewModel: StartupViewModel

  @Test
  fun `init success`() = runWithTestDispatcher {
    viewModel = StartupViewModel(googleApiManager, userRepository, popups)

    viewModel.state.test {
      assertThat(awaitItem()).isEqualTo(StartupState.Loading)
      expectNoEvents()
    }
  }

  @Test
  fun `init fails from GoogleApiManager with other exception`() = runWithTestDispatcher {
    val exception = RuntimeException("some other error")
    whenever(googleApiManager.installGooglePlayServices()).thenThrow(exception)

    viewModel = StartupViewModel(googleApiManager, userRepository, popups)

    viewModel.state.test {
      val errorState = awaitItem()
      assertThat(errorState).isInstanceOf(StartupState.Error::class.java)
      assertThat((errorState as StartupState.Error).errorMessageId).isNull()
    }
  }

  @Test
  fun `init fails from UserRepository`() = runWithTestDispatcher {
    val exception = RuntimeException("user repo error")
    whenever(userRepository.init()).thenThrow(exception)

    viewModel = StartupViewModel(googleApiManager, userRepository, popups)

    viewModel.state.test {
      val errorState = awaitItem()
      assertThat(errorState).isInstanceOf(StartupState.Error::class.java)
      assertThat((errorState as StartupState.Error).errorMessageId).isNull()
    }
  }
}
