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

package com.google.android.ground.ui.editsubmission;

import static com.google.android.ground.ui.util.ViewUtil.assignGeneratedId;

import android.widget.LinearLayout;
import androidx.annotation.LayoutRes;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import com.google.android.ground.BR;
import com.google.android.ground.R;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.ui.common.ViewModelFactory;
import javax.inject.Inject;

/** Inflates a new view and generates a view model for a given {@link Task.Type}. */
public class TaskViewFactory {

  @Inject Fragment fragment;
  @Inject ViewModelFactory viewModelFactory;

  @Inject
  TaskViewFactory() {}

  public static Class<? extends AbstractTaskViewModel> getViewModelClass(Task.Type taskType) {
    switch (taskType) {
      case TEXT:
        return TextTaskViewModel.class;
      case MULTIPLE_CHOICE:
        return MultipleChoiceTaskViewModel.class;
      case PHOTO:
        return PhotoTaskViewModel.class;
      case NUMBER:
        return NumberTaskViewModel.class;
      case DATE:
        return DateTaskViewModel.class;
      case TIME:
        return TimeTaskViewModel.class;
      default:
        throw new IllegalArgumentException("Unsupported task type: " + taskType);
    }
  }

  @LayoutRes
  private static int getLayoutId(Task.Type taskType) {
    switch (taskType) {
      case TEXT:
        return R.layout.text_input_task;
      case MULTIPLE_CHOICE:
        return R.layout.multiple_choice_input_task;
      case PHOTO:
        return R.layout.photo_input_task;
      case NUMBER:
        return R.layout.number_input_task;
      case DATE:
        return R.layout.date_input_task;
      case TIME:
        return R.layout.time_input_task;
      default:
        throw new IllegalArgumentException("Unsupported task type: " + taskType);
    }
  }

  /**
   * Inflates the view, generates a new view model and binds to the {@link ViewDataBinding}.
   *
   * @param taskType Type of the task
   * @param root Parent layout
   * @return {@link ViewDataBinding}
   */
  ViewDataBinding addTaskView(Task.Type taskType, LinearLayout root) {
    ViewDataBinding binding =
        DataBindingUtil.inflate(fragment.getLayoutInflater(), getLayoutId(taskType), root, true);
    binding.setLifecycleOwner(fragment);
    binding.setVariable(BR.viewModel, viewModelFactory.create(getViewModelClass(taskType)));
    assignGeneratedId(binding.getRoot());
    return binding;
  }
}
