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

package com.google.android.ground.ui.home.mapcontainer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.databinding.MapTypeDialogFragmentBinding
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.map.MapType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Dialog fragment containing a list of [MapType] for updating basemap layer. */
@AndroidEntryPoint
class MapTypeDialogFragment : BottomSheetDialogFragment() {

  @Inject lateinit var mapStateRepository: MapStateRepository
  @Inject lateinit var viewModelFactory: ViewModelFactory

  private lateinit var binding: MapTypeDialogFragmentBinding
  private lateinit var mapTypes: List<MapType>
  private lateinit var viewModel: MapTypeViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = viewModelFactory[this, MapTypeViewModel::class.java]
  }

  // TODO(#936): Remove the suppress annotation when fragment dependency is upgraded to 1.3.4
  // TODO(#1753): Handle click for help icon
  @SuppressLint("UseRequireInsteadOfGet")
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    mapTypes = MapTypeDialogFragmentArgs.fromBundle(arguments!!).mapTypes.toList()
    binding = MapTypeDialogFragmentBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val index = mapTypes.indexOfFirst { it == viewModel.mapType }
    binding.recyclerView.adapter =
      MapTypeAdapter(requireContext(), mapTypes, index) { viewModel.mapType = mapTypes[it] }
    binding.recyclerView.addItemDecoration(AdaptiveSpacingItemDecorator(resources, 80))
  }
}
