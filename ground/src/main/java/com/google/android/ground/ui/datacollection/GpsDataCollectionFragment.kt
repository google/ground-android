package com.google.android.ground.ui.datacollection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.ground.BR
import com.google.android.ground.databinding.GpsDataCollectionFragBinding
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.AbstractMapViewerFragment
import com.google.android.ground.ui.editsubmission.AbstractTaskViewModel
import com.google.android.ground.ui.home.mapcontainer.BaseMapViewModel
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GpsDataCollectionFragment(
  private val task: Task,
  private val viewModel: AbstractTaskViewModel,
  private val mapViewModel: BaseMapViewModel
) : AbstractMapViewerFragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    val binding = GpsDataCollectionFragBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this
    binding.mapViewModel = mapViewModel
    binding.setVariable(BR.viewModel, viewModel)

    return binding.root
  }

  override fun onMapReady(mapFragment: MapFragment) {
    //    mapFragment.disableGestures()
  }

  override fun getMapViewModel(): BaseMapViewModel = mapViewModel
}
