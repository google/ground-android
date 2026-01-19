/*
 * Copyright 2018 Google LLC
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.ui.common.AbstractFragment
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.theme.AppTheme

@AndroidEntryPoint
class StartupFragment : AbstractFragment() {

  @Inject lateinit var popups: EphemeralPopups

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View =
    ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          StartupScreen(
            onLoadFailed = { errorMessageId ->
              errorMessageId?.let { popups.ErrorPopup().show(it) }
              requireActivity().finish()
            }
          )
        }
      }
    }
}
