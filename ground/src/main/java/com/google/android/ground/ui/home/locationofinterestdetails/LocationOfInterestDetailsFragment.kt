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
package com.google.android.ground.ui.home.locationofinterestdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import com.google.android.ground.MainViewModel
import com.google.android.ground.R
import com.google.android.ground.databinding.LocationOfInterestDetailsFragBinding
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.home.BottomSheetState
import com.google.android.ground.ui.home.HomeScreenViewModel
import dagger.hilt.android.AndroidEntryPoint
import java8.util.Optional
import javax.inject.Inject

/** Fragment containing the contents of the bottom sheet shown when a LOI is selected. */
@AndroidEntryPoint(AbstractFragment::class)
class LocationOfInterestDetailsFragment @Inject constructor() :
  Hilt_LocationOfInterestDetailsFragment() {

  private lateinit var binding: LocationOfInterestDetailsFragBinding
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var mainViewModel: MainViewModel
  private lateinit var viewModel: LocationOfInterestDetailsViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(LocationOfInterestDetailsViewModel::class.java)
    mainViewModel = getViewModel(MainViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = LocationOfInterestDetailsFragBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setHasOptionsMenu(true)
    mainViewModel.windowInsets.observe(viewLifecycleOwner) { insets: WindowInsetsCompat ->
      onApplyWindowInsets(insets)
    }
    homeScreenViewModel.bottomSheetState.observe(viewLifecycleOwner) { state: BottomSheetState ->
      onBottomSheetStateChange(state)
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.loi_sheet_menu, menu)
  }

  private fun onBottomSheetStateChange(state: BottomSheetState) {
    val loi = if (state.isVisible) state.locationOfInterest else null
    viewModel.onLocationOfInterestSelected(Optional.ofNullable(loi))
  }

  private fun onApplyWindowInsets(insets: WindowInsetsCompat) {
    binding.root
      .findViewById<View>(R.id.submission_list_container)
      .setPadding(0, 0, 0, insets.systemWindowInsetBottom)
  }
}
