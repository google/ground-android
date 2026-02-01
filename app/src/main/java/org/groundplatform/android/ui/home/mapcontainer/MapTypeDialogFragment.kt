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

package org.groundplatform.android.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.groundplatform.android.ui.basemapselector.MapTypeScreen
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.theme.AppTheme
import javax.inject.Inject

/** Dialog fragment containing a list of [MapType] for updating basemap layer. */
@AndroidEntryPoint
class MapTypeDialogFragment : BottomSheetDialogFragment() {

  @Inject lateinit var viewModelFactory: ViewModelFactory

  private lateinit var viewModel: MapTypeViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = viewModelFactory[this, MapTypeViewModel::class.java]
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { AppTheme { MapTypeScreen(onMapTypeSelected = { dismiss() }, viewModel) } }
    }
  }
}
