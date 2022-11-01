/*
 * Copyright 2020 Google LLC
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

package com.google.android.ground.ui.offlinebasemap.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.databinding.OfflineBaseMapSelectorFragBinding
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.RxAutoDispose.autoDisposable
import com.google.android.ground.ui.common.AbstractMapViewerFragment
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.map.gms.toGoogleMapsObject
import com.google.android.ground.ui.offlinebasemap.selector.OfflineAreaSelectorViewModel.DownloadMessage
import com.google.android.ground.util.toImmutableList
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OfflineAreaSelectorFragment : AbstractMapViewerFragment() {

  @Inject lateinit var navigator: Navigator
  @Inject lateinit var popups: EphemeralPopups

  private lateinit var viewModel: OfflineAreaSelectorViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(OfflineAreaSelectorViewModel::class.java)
    viewModel.downloadMessages.observe(this) { e: Event<DownloadMessage> ->
      e.ifUnhandled { message: DownloadMessage -> onDownloadMessage(message) }
    }
  }

  private fun onDownloadMessage(message: DownloadMessage) {
    when (message) {
      DownloadMessage.STARTED -> {
        popups.showSuccess(R.string.offline_base_map_download_started)
        navigator.navigateUp()
      }
      DownloadMessage.FAILURE -> {
        popups.showError(R.string.offline_base_map_download_failed)
        navigator.navigateUp()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = OfflineBaseMapSelectorFragBinding.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    (activity as MainActivity?)!!.setActionBar(binding.offlineAreaSelectorToolbar, true)
    return binding.root
  }

  override fun onMapReady(mapFragment: MapFragment) {
    viewModel.remoteTileSets
      .map { tileSets -> tileSets.map { it.url }.toImmutableList() }
      .`as`(autoDisposable(this))
      .subscribe(mapFragment::addRemoteTileOverlays)

    viewModel.requestRemoteTileSets()

    mapFragment.cameraMovedEvents
      .map { mapFragment.viewport }
      .startWith(mapFragment.viewport)
      .map { it.toGoogleMapsObject() }
      .`as`(autoDisposable(this))
      .subscribe(viewModel::setViewport)
  }
}
