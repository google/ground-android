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
package com.google.android.ground.ui.submissiondetails

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.databinding.PhotoTaskBinding
import com.google.android.ground.databinding.SubmissionDetailsFragBinding
import com.google.android.ground.databinding.SubmissionDetailsFragBindingImpl
import com.google.android.ground.databinding.SubmissionDetailsTaskBindingImpl
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Loadable.LoadState
import com.google.android.ground.rx.RxAutoDispose.autoDisposable
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.editsubmission.PhotoTaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class SubmissionDetailsFragment : AbstractFragment() {

  @Inject lateinit var navigator: Navigator
  @Inject lateinit var popups: EphemeralPopups

  lateinit var viewModel: SubmissionDetailsViewModel
  lateinit var binding: SubmissionDetailsFragBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val args = submissionDetailsFragmentArgs
    viewModel = getViewModel(SubmissionDetailsViewModel::class.java)
    viewModel.submission.observe(this) { submission: Loadable<Submission> -> onUpdate(submission) }
    viewModel.loadSubmissionDetails(args)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = SubmissionDetailsFragBindingImpl.inflate(inflater, container, false)
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (activity as MainActivity?)!!.setActionBar(binding.submissionDetailsToolbar, false)
  }

  @Deprecated("Deprecated in Java")
  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.submission_details_menu, menu)
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setHasOptionsMenu(true)
  }

  private fun onUpdate(submission: Loadable<Submission>) {
    when (submission.state) {
      LoadState.LOADED -> submission.value().ifPresent { showSubmission(it) }
      LoadState.ERROR -> {
        // TODO: Replace w/error view?
        Timber.e("Failed to load submission")
        popups.showError()
      }
    }
  }

  private fun showSubmission(submission: Submission) {
    binding.submissionDetailsLayout.removeAllViews()
    for (task in submission.job.tasksSorted) {
      addTask(task, submission)
    }
  }

  private fun addTask(task: Task, submission: Submission) {
    val binding = SubmissionDetailsTaskBindingImpl.inflate(layoutInflater)
    binding.task = task
    binding.lifecycleOwner = this
    this.binding.submissionDetailsLayout.addView(binding.root)
    submission.responses.getResponse(task.id).ifPresent { taskData: TaskData ->
      if (task.type === Task.Type.PHOTO) {
        binding.taskValue.visibility = View.GONE
        addPhotoTask(binding.root as ViewGroup, taskData)
      } else {
        binding.taskValue.text = taskData.getDetailsText()
      }
    }
  }

  private fun addPhotoTask(container: ViewGroup, taskData: TaskData) {
    val photoFieldViewModel = viewModelFactory.create(PhotoTaskViewModel::class.java)
    photoFieldViewModel.setResponse(Optional.of(taskData))
    val photoFieldBinding = PhotoTaskBinding.inflate(layoutInflater)
    photoFieldBinding.lifecycleOwner = this
    photoFieldBinding.viewModel = photoFieldViewModel
    container.addView(photoFieldBinding.root)
  }

  @Deprecated("Deprecated in Java")
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val args = submissionDetailsFragmentArgs
    val surveyId = args.surveyId
    val locationOfInterestId = args.locationOfInterestId
    val submissionId = args.submissionId
    if (item.itemId == R.id.edit_submission_menu_item) {
      navigator.navigate(
        SubmissionDetailsFragmentDirections.editSubmission(
          surveyId,
          locationOfInterestId,
          submissionId
        )
      )
    } else if (item.itemId == R.id.delete_submission_menu_item) {
      AlertDialog.Builder(requireActivity())
        .setTitle(R.string.submission_delete_confirmation_dialog_title)
        .setMessage(R.string.submission_delete_confirmation_dialog_message)
        .setPositiveButton(R.string.delete_button_label) { _: DialogInterface?, _: Int ->
          viewModel
            .deleteCurrentSubmission(surveyId, locationOfInterestId, submissionId)
            .`as`(autoDisposable<Any>(this))
            .subscribe { navigator.navigateUp() }
        }
        .setNegativeButton(R.string.cancel_button_label) { _: DialogInterface?, _: Int -> }
        .create()
        .show()
    } else {
      return false
    }
    return true
  }

  private val submissionDetailsFragmentArgs: SubmissionDetailsFragmentArgs
    get() = SubmissionDetailsFragmentArgs.fromBundle(requireArguments())
}
