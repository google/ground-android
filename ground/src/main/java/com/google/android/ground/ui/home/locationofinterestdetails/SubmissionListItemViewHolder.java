/*
 * Copyright 2018 Google LLC
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

package com.google.android.ground.ui.home.locationofinterestdetails;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.ground.R;
import com.google.android.ground.databinding.SubmissionListItemBinding;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.model.submission.TaskData;
import com.google.android.ground.model.task.Task;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;

class SubmissionListItemViewHolder extends RecyclerView.ViewHolder {

  private static final int MAX_COLUMNS = 4;

  private final SubmissionListItemBinding binding;

  SubmissionListItemViewHolder(@NonNull SubmissionListItemBinding binding) {
    super(binding.getRoot());
    this.binding = binding;
  }

  public void bind(SubmissionListItemViewModel viewModel, Submission submission) {
    binding.setViewModel(viewModel);
    binding.executePendingBindings();

    // Add UI elements for each task with data.
    addTasksFromSubmission(submission);
  }

  private void addTasksFromSubmission(Submission submission) {
    binding.taskLabelRow.removeAllViews();
    binding.taskValueRow.removeAllViews();

    Job job = submission.getJob();
    // TODO: Clean this up.
    ImmutableList<Task> tasks = job.getTasksSorted();
    for (int i = 0; i < MAX_COLUMNS && i < tasks.size(); i++) {
      Task task = tasks.get(i);
      Optional<TaskData> response = submission.getResponses().getResponse(task.getId());
      binding.taskLabelRow.addView(
          newTextView(task.getLabel(), R.style.SubmissionListText_TaskLabel));
      binding.taskValueRow.addView(
          newTextView(
              response.map(TaskData::getDetailsText).orElse(""), R.style.SubmissionListText_Task));
    }
  }

  @NonNull
  private TextView newTextView(String text, @StyleRes int textAppearance) {
    Context context = binding.getRoot().getContext();
    Resources resources = context.getResources();
    TextView v = new TextView(context);
    v.setTextAppearance(context, textAppearance);
    // NOTE: These attributes don't work when applying text appearance programmatically, so we set
    // them here individually instead.
    v.setPadding(
        0, 0, resources.getDimensionPixelSize(R.dimen.submission_summary_text_padding_right), 0);
    v.setMaxWidth(resources.getDimensionPixelSize(R.dimen.submission_summary_text_max_width));
    v.setMaxLines(1);
    v.setSingleLine();
    v.setEllipsize(TextUtils.TruncateAt.END);
    v.setText(text);
    return v;
  }
}
