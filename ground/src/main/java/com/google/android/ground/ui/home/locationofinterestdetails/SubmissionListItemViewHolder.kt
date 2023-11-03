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
package com.google.android.ground.ui.home.locationofinterestdetails

import android.text.TextUtils
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.R
import com.google.android.ground.databinding.SubmissionListItemBinding
import com.google.android.ground.model.submission.Submission

internal class SubmissionListItemViewHolder(private val binding: SubmissionListItemBinding) :
  RecyclerView.ViewHolder(binding.root) {
  fun bind(viewModel: SubmissionListItemViewModel?, submission: Submission) {
    binding.viewModel = viewModel
    binding.executePendingBindings()

    // Add UI elements for each task with data.
    addTasksFromSubmission(submission)
  }

  private fun addTasksFromSubmission(submission: Submission) {
    binding.taskLabelRow.removeAllViews()
    binding.taskValueRow.removeAllViews()
    val job = submission.job
    // TODO: Clean this up.
    val tasks = job.tasksSorted
    var i = 0
    while (i < MAX_COLUMNS && i < tasks.size) {
      val task = tasks[i]
      val response = submission.data.getResponse(task.id)
      binding.taskLabelRow.addView(newTextView(task.label, R.style.SubmissionListText_TaskLabel))
      binding.taskValueRow.addView(
        newTextView(response?.getDetailsText() ?: "", R.style.SubmissionListText_Task)
      )
      i++
    }
  }

  private fun newTextView(text: String, @StyleRes textAppearance: Int): TextView {
    val context = binding.root.context
    val resources = context.resources
    val v = TextView(context)
    v.setTextAppearance(context, textAppearance)
    // NOTE: These attributes don't work when applying text appearance programmatically, so we set
    // them here individually instead.
    v.setPadding(
      0,
      0,
      resources.getDimensionPixelSize(R.dimen.submission_summary_text_padding_right),
      0
    )
    v.maxWidth = resources.getDimensionPixelSize(R.dimen.submission_summary_text_max_width)
    v.maxLines = 1
    v.setSingleLine()
    v.ellipsize = TextUtils.TruncateAt.END
    v.text = text
    return v
  }

  companion object {
    private const val MAX_COLUMNS = 4
  }
}
