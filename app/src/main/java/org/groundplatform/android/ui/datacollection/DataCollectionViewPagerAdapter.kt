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

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.groundplatform.android.ui.datacollection.tasks.DataCollectionTaskFragment
import org.groundplatform.domain.model.task.Task
import javax.inject.Provider

/**
 * A simple pager adapter that presents the [Task]s associated with a given Submission, in sequence.
 */
class DataCollectionViewPagerAdapter
@AssistedInject
constructor(
  private val taskFragmentProvider: Provider<DataCollectionTaskFragment>,
  @Assisted fragment: Fragment,
  @Assisted val tasks: List<Task>,
) : FragmentStateAdapter(fragment) {
  override fun getItemCount(): Int = tasks.size

  override fun createFragment(position: Int): Fragment {
    val task = tasks[position]

    val taskFragment =
      when (task.type) {
        Task.Type.TEXT,
        Task.Type.NUMBER,
        Task.Type.DATE,
        Task.Type.TIME,
        Task.Type.INSTRUCTIONS,
        Task.Type.MULTIPLE_CHOICE,
        Task.Type.PHOTO,
        Task.Type.DROP_PIN,
        Task.Type.DRAW_AREA,
        Task.Type.CAPTURE_LOCATION -> taskFragmentProvider.get()
        Task.Type.UNKNOWN ->
          throw UnsupportedOperationException("Unsupported task type: ${task.type}")
      }

    return taskFragment.also {
      val bundle = it.arguments ?: Bundle()
      bundle.putString(DataCollectionTaskFragment.TASK_ID, task.id)
      it.arguments = bundle
    }
  }
}
