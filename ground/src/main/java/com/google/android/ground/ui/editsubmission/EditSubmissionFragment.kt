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
package com.google.android.ground.ui.editsubmission

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.google.android.ground.MainActivity
import com.google.android.ground.R
import com.google.android.ground.databinding.EditSubmissionFragBinding
import com.google.android.ground.model.job.Job
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.common.BackPressListener
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(AbstractFragment::class)
class EditSubmissionFragment : Hilt_EditSubmissionFragment(), BackPressListener {

  /** String constant keys used for persisting state in {@see Bundle} objects. */
  private object BundleKeys {
    /** Key used to store unsaved responses across activity re-creation. */
    const val RESTORED_RESPONSES = "restoredResponses"
  }

  private val taskViewModels: MutableList<AbstractTaskViewModel> = ArrayList()

  @Inject lateinit var navigator: Navigator
  @Inject lateinit var popups: EphemeralPopups
  @Inject lateinit var schedulers: Schedulers
  @Inject lateinit var userMediaRepository: UserMediaRepository

  private lateinit var viewModel: EditSubmissionViewModel
  private lateinit var binding: EditSubmissionFragBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = getViewModel(EditSubmissionViewModel::class.java)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = EditSubmissionFragBinding.inflate(inflater, container, false)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel
    binding.fragment = this
    binding.saveSubmissionBtn.setOnClickListener { view: View -> onSaveClick(view) }
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val toolbar = binding.editSubmissionToolbar
    (activity as MainActivity?)!!.setActionBar(toolbar, R.drawable.ic_close_black_24dp)
    toolbar.setNavigationOnClickListener { onCloseButtonClick() }
    // Observe state changes.
    viewModel.job.observe(viewLifecycleOwner) { job: Job -> rebuildForm(job) }

    // Initialize view model.
    val args: Bundle? = arguments
    if (savedInstanceState != null) {
      args?.putSerializable(
        BundleKeys.RESTORED_RESPONSES,
        savedInstanceState.getSerializable(BundleKeys.RESTORED_RESPONSES)
      )
    }
    viewModel.initialize(EditSubmissionFragmentArgs.fromBundle(args ?: bundleOf()))
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putSerializable(BundleKeys.RESTORED_RESPONSES, viewModel.draftResponses)
  }

  private fun onSaveClick(view: View) {
    hideKeyboard(view)
    viewModel.onSaveClick(validationErrors)
  }

  private fun hideKeyboard(view: View) {
    if (activity != null) {
      val inputMethodManager =
        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }

  private val validationErrors: Map<String, String>
    get() {
      val errors = HashMap<String, String>()
      for (fieldViewModel in taskViewModels) {
        val error = fieldViewModel.validate()
        if (error != null) {
          errors[fieldViewModel.task.id] = error
        }
      }
      return errors
    }

  private fun rebuildForm(job: Job) {
    val formLayout = binding.editSubmissionLayout
    formLayout.removeAllViews()
    taskViewModels.clear()
  }

  override fun onBack(): Boolean {
    if (viewModel.hasUnsavedChanges()) {
      showUnsavedChangesDialog()
      return true
    }
    return false
  }

  private fun onCloseButtonClick() {
    if (viewModel.hasUnsavedChanges()) {
      showUnsavedChangesDialog()
    } else {
      navigator.navigateUp()
    }
  }

  private fun showUnsavedChangesDialog() {
    AlertDialog.Builder(requireContext())
      .setMessage(R.string.unsaved_changes)
      .setPositiveButton(R.string.discard_changes) { _, _ -> navigator.navigateUp() }
      .setNegativeButton(R.string.continue_editing) { _, _ -> }
      .create()
      .show()
  }
}
