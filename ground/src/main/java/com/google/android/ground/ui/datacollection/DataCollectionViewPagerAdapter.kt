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
package com.google.android.ground.ui.datacollection

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.editsubmission.TaskViewFactory
import com.google.android.ground.ui.home.mapcontainer.BaseMapViewModel
import com.google.android.ground.ui.map.LocationController
import com.google.android.ground.ui.map.MapController
import com.google.common.collect.ImmutableList
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java8.util.Optional

/**
 * A simple pager adapter that presents the [Task]s associated with a given Submission, in sequence.
 */
class DataCollectionViewPagerAdapter
@AssistedInject
constructor(
  private val viewModelFactory: ViewModelFactory,
  @Assisted fragment: Fragment,
  @Assisted private val tasks: ImmutableList<Task>,
  @Assisted private val dataCollectionViewModel: DataCollectionViewModel,
  private val locationController: LocationController,
  private val mapController: MapController
) : FragmentStateAdapter(fragment) {
  override fun getItemCount(): Int = tasks.size

  override fun createFragment(position: Int): Fragment {
    // TODO: DELETE ME!
    val type = Task.Type.GPS

    val task = tasks[position]
    val viewModel = viewModelFactory.create(TaskViewFactory.getViewModelClass(type))

    // TODO(#1146): Pass in the existing taskData if there is one
    viewModel.initialize(task, Optional.empty())

    dataCollectionViewModel.addTaskViewModel(viewModel)
    return when (type) {
      Task.Type.TEXT -> QuestionDataCollectionFragment(task, viewModel)
      Task.Type.MULTIPLE_CHOICE -> MultipleChoiceDataCollectionFragment(task, viewModel)
      Task.Type.GPS ->
        GpsDataCollectionFragment(
          task,
          viewModel,
          BaseMapViewModel(locationController, mapController)
        )
      else -> DataCollectionTaskFragment()
    }
  }
}
