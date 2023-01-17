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
package com.google.android.ground.ui.editsubmission

import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.ground.BR
import com.google.android.ground.R
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DropAPinTaskViewModel
import com.google.android.ground.ui.datacollection.PolygonDrawingViewModel
import com.google.android.ground.ui.util.ViewUtil.assignGeneratedId
import javax.inject.Inject

/** Inflates a new view and generates a view model for a given [Task.Type]. */
class TaskViewFactory @Inject internal constructor() {

  @Inject lateinit var fragment: Fragment
  @Inject lateinit var viewModelFactory: ViewModelFactory

  /**
   * Inflates the view, generates a new view model and binds to the [ViewDataBinding].
   *
   * @param taskType Type of the task
   * @param root Parent layout
   * @return [ViewDataBinding]
   */
  fun addTaskView(taskType: Task.Type, root: LinearLayout): ViewDataBinding {
    val binding =
      DataBindingUtil.inflate<ViewDataBinding>(
        fragment.layoutInflater,
        getLayoutId(taskType),
        root,
        true
      )
    binding.lifecycleOwner = fragment
    binding.setVariable(BR.viewModel, viewModelFactory.create(getViewModelClass(taskType)))
    assignGeneratedId(binding.root)
    return binding
  }

  companion object {
    fun getViewModelClass(taskType: Task.Type): Class<out AbstractTaskViewModel> =
      when (taskType) {
        Task.Type.TEXT -> TextTaskViewModel::class.java
        Task.Type.MULTIPLE_CHOICE -> MultipleChoiceTaskViewModel::class.java
        Task.Type.PHOTO -> PhotoTaskViewModel::class.java
        Task.Type.NUMBER -> NumberTaskViewModel::class.java
        Task.Type.DATE -> DateTaskViewModel::class.java
        Task.Type.TIME -> TimeTaskViewModel::class.java
        Task.Type.DROP_A_PIN -> DropAPinTaskViewModel::class.java
        Task.Type.DRAW_POLYGON -> PolygonDrawingViewModel::class.java
        Task.Type.UNKNOWN -> throw IllegalArgumentException("Unsupported task type: $taskType")
      }

    @LayoutRes
    private fun getLayoutId(taskType: Task.Type): Int =
      when (taskType) {
        Task.Type.TEXT -> R.layout.text_input_task
        Task.Type.MULTIPLE_CHOICE -> R.layout.multiple_choice_input_task
        Task.Type.PHOTO -> R.layout.photo_input_task
        Task.Type.NUMBER -> R.layout.number_input_task
        Task.Type.DATE -> R.layout.date_input_task
        Task.Type.TIME -> R.layout.time_input_task
        else -> throw IllegalArgumentException("Unsupported task type: $taskType")
      }
  }
}
