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
package com.google.android.ground.ui.datacollection.tasks.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.ui.common.AbstractMapFragmentWithControls
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CaptureLocationTaskMapFragment @Inject constructor() : AbstractMapFragmentWithControls() {

  private lateinit var mapViewModel: CaptureLocationTaskMapViewModel
  private val viewModel: CaptureLocationTaskViewModel by lazy {
    // Access to this viewModel is lazy for testing. This is because the NavHostController could
    // not be initialized before the Fragment under test is created, leading to
    // hiltNavGraphViewModels() to fail when called on launch.
    dataCollectionViewModel.getTaskViewModel(taskId) as CaptureLocationTaskViewModel
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapViewModel = getViewModel(CaptureLocationTaskMapViewModel::class.java)
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    val root = super.onCreateView(inflater, container, savedInstanceState)
    viewLifecycleOwner.lifecycleScope.launch {
      getMapViewModel().getLocationUpdates().collect { viewModel.updateLocation(it) }
    }
    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      viewModel.enableLocationLockFlow.collect {
        if (it == LocationLockEnabledState.NEEDS_ENABLE) {
          showLocationPermissionDialog()
        }
      }
    }
  }

  override fun onMapReady(map: MapFragment) {
    super.onMapReady(map)
    viewLifecycleOwner.lifecycleScope.launch { viewModel.onMapReady(mapViewModel) }
  }

  @Suppress("LabeledExpression")
  private fun showLocationPermissionDialog() {
    val dialogComposeView =
      ComposeView(requireContext()).apply {
        setContent {
          val openAlertDialog = remember { mutableStateOf(true) }
          when {
            openAlertDialog.value -> {
              AppTheme {
                LocationPermissionDialog(
                  onCancel = {
                    binding.locationLockBtn.isClickable = false
                    openAlertDialog.value = false
                  },
                  onDismiss = { openAlertDialog.value = false },
                )
              }
            }
          }

          DisposableEffect(Unit) { onDispose { (parent as? ViewGroup)?.removeView(this@apply) } }
        }
      }

    (view as ViewGroup).addView(dialogComposeView)
  }
}
