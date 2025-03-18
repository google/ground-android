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
package org.groundplatform.android.ui.datacollection

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Provider
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.date.DateTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.instruction.InstructionTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.location.CaptureLocationTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.multiplechoice.MultipleChoiceTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.number.NumberTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.photo.PhotoTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.point.DropPinTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.text.TextTaskFragment
import org.groundplatform.android.ui.datacollection.tasks.time.TimeTaskFragment

/**
 * A simple pager adapter that presents the [Task]s associated with a given Submission, in sequence.
 */
class DataCollectionViewPagerAdapter
@AssistedInject
constructor(
  private val drawAreaTaskFragmentProvider: Provider<DrawAreaTaskFragment>,
  private val captureLocationTaskFragmentProvider: Provider<CaptureLocationTaskFragment>,
  private val dropPinTaskFragmentProvider: Provider<DropPinTaskFragment>,
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
        Task.Type.DROP_PIN -> dropPinTaskFragmentProvider.get()
        Task.Type.DRAW_AREA -> drawAreaTaskFragmentProvider.get()
        Task.Type.NUMBER -> NumberTaskFragment()
        Task.Type.DATE -> DateTaskFragment()
        Task.Type.TIME -> TimeTaskFragment()
        Task.Type.CAPTURE_LOCATION -> captureLocationTaskFragmentProvider.get()
        Task.Type.INSTRUCTIONS -> InstructionTaskFragment()
        Task.Type.UNKNOWN ->
          throw UnsupportedOperationException("Unsupported task type: ${task.type}")
      }

    return taskFragment.also { it.taskId = task.id }
  }
}
