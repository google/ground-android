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
package com.google.android.ground.ui.datacollection.components

import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.ground.databinding.TaskFragWithCombinedHeaderBinding
import com.google.android.ground.databinding.TaskFragWithHeaderBinding

/** Factory for creating a [TaskView]. */
object TaskViewFactory {

  fun createWithHeader(layoutInflater: LayoutInflater): TaskView =
    TaskViewWithHeader(TaskFragWithHeaderBinding.inflate(layoutInflater))

  /** Creates a TaskView with a header that is an extension of the app bar. */
  fun createWithCombinedHeader(
    layoutInflater: LayoutInflater,
    @DrawableRes iconResId: Int? = null,
    @StringRes labelResId: Int? = null,
  ): TaskView {
    val binding = TaskFragWithCombinedHeaderBinding.inflate(layoutInflater)
    iconResId?.let {
      val drawable = AppCompatResources.getDrawable(layoutInflater.context, it)
      binding.headerIcon.setImageDrawable(drawable)
    }
    labelResId?.let { binding.headerLabel.setText(labelResId) }
    return TaskViewWithCombinedHeader(binding)
  }
}
