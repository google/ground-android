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
import com.google.android.ground.ui.datacollection.tasks.date.DateTaskFragment
import com.google.android.ground.ui.datacollection.tasks.location.CaptureLocationTaskFragment
import com.google.android.ground.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskFragment
import com.google.android.ground.ui.datacollection.tasks.number.NumberTaskFragment
import com.google.android.ground.ui.datacollection.tasks.photo.PhotoTaskFragment
import com.google.android.ground.ui.datacollection.tasks.point.DropPinTaskFragment
import com.google.android.ground.ui.datacollection.tasks.polygon.DrawAreaTaskFragment
import com.google.android.ground.ui.datacollection.tasks.text.TextTaskFragment
import com.google.android.ground.ui.datacollection.tasks.time.TimeTaskFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Provider

/**
 * A simple pager adapter that presents the [Task]s associated with a given Submission, in sequence.
 */
class DataCollectionViewPagerAdapter
@AssistedInject
constructor(
  private val drawAreaTaskFragmentProvider: Provider<DrawAreaTaskFragment>,
  @Assisted fragment: Fragment,
  @Assisted val tasks: List<Task>,
) : FragmentStateAdapter(fragment) {
  override fun getItemCount(): Int = tasks.size

  override fun createFragment(position: Int): Fragment {
    val task = tasks[position]

    val taskFragment =
      when (task.type) {
        Task.Type.TEXT -> TextTaskFragment()
        Task.Type.MULTIPLE_CHOICE -> MultipleChoiceTaskFragment()
        Task.Type.PHOTO -> PhotoTaskFragment()
        Task.Type.DROP_PIN -> DropPinTaskFragment()
        Task.Type.DRAW_AREA -> drawAreaTaskFragmentProvider.get()
        Task.Type.NUMBER -> NumberTaskFragment()
        Task.Type.DATE -> DateTaskFragment()
        Task.Type.TIME -> TimeTaskFragment()
        Task.Type.CAPTURE_LOCATION -> CaptureLocationTaskFragment()
        Task.Type.UNKNOWN ->
          throw UnsupportedOperationException("Unsupported task type: ${task.type}")
      }

    return taskFragment.also { it.taskId = task.id }
  }
}
