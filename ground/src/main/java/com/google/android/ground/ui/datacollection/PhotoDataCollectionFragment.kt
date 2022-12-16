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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.ground.BR
import com.google.android.ground.BuildConfig
import com.google.android.ground.databinding.EditSubmissionBottomSheetBinding
import com.google.android.ground.databinding.PhotoDataCollectionFragBinding
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.rx.RxAutoDispose.autoDisposable
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.editsubmission.AddPhotoDialogAdapter
import com.google.android.ground.ui.editsubmission.AddPhotoDialogAdapter.PhotoStorageResource
import com.google.android.ground.ui.editsubmission.PhotoResult
import com.google.android.ground.ui.editsubmission.PhotoTaskViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import timber.log.Timber

/** Fragment allowing the user to capture a photo to complete a task. */
@AndroidEntryPoint
class PhotoDataCollectionFragment
constructor(
  private val task: Task,
  private val viewModel: PhotoTaskViewModel,
  private val dataCollectionViewModel: DataCollectionViewModel,
  private val userMediaRepository: UserMediaRepository
) : AbstractFragment() {
  private lateinit var selectPhotoLauncher: ActivityResultLauncher<String>
  private lateinit var capturePhotoLauncher: ActivityResultLauncher<Uri>

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    selectPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
      viewModel.onSelectPhotoResult(
        uri
      )
    }
    capturePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
      viewModel.onCapturePhotoResult(
        result
      )
    }

    val binding = PhotoDataCollectionFragBinding.inflate(inflater, container, false)

    binding.lifecycleOwner = this
    binding.setVariable(BR.viewModel, viewModel)
    binding.setVariable(BR.dataCollectionViewModel, dataCollectionViewModel)

    viewModel.setEditable(true)
    viewModel.setSurveyId(dataCollectionViewModel.surveyId)
    viewModel.setSubmissionId(dataCollectionViewModel.submissionId)
    observeSelectPhotoClicks()
    observePhotoResults()

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.setTaskWaitingForPhoto(savedInstanceState?.getString(TASK_WAITING_FOR_PHOTO))
    viewModel.setCapturedPhotoPath(savedInstanceState?.getString(CAPTURED_PHOTO_PATH))
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(TASK_WAITING_FOR_PHOTO, viewModel.getTaskWaitingForPhoto());
    outState.putString(CAPTURED_PHOTO_PATH, viewModel.getCapturedPhotoPath());
  }

  private fun observeSelectPhotoClicks() {
    viewModel.getShowDialogClicks().observe(viewLifecycleOwner) {
      onShowPhotoSelectorDialog()
    }
  }

  private fun observePhotoResults() {
    viewModel
      .getLastPhotoResult()
      .`as`(autoDisposable<PhotoResult>(viewLifecycleOwner))
      .subscribe { photoResult ->
        viewModel.onPhotoResult(
          photoResult
        )
      }
  }

  // TODO(jsunde): Try to factor this logic out and share between here and EditSubmissionFragment
  private fun onShowPhotoSelectorDialog() {
    val addPhotoBottomSheetBinding: EditSubmissionBottomSheetBinding =
      EditSubmissionBottomSheetBinding.inflate(layoutInflater)
    //    addPhotoBottomSheetBinding.setViewModel(viewModel)
    //    addPhotoBottomSheetBinding.setTask(task)
    val bottomSheetDialog = BottomSheetDialog(requireContext())
    bottomSheetDialog.setContentView(addPhotoBottomSheetBinding.root)
    bottomSheetDialog.setCancelable(true)
    bottomSheetDialog.show()
    val recyclerView: RecyclerView = addPhotoBottomSheetBinding.recyclerView
    recyclerView.setHasFixedSize(true)
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = AddPhotoDialogAdapter { type: Int ->
      bottomSheetDialog.dismiss()
      onSelectPhotoClick(type, task.id)
    }
  }

  private fun onSelectPhotoClick(type: Int, fieldId: String) {
    when (type) {
      PhotoStorageResource.PHOTO_SOURCE_CAMERA ->
        // TODO: Launch intent is not invoked if the permission is not granted by default.
        viewModel
          .obtainCapturePhotoPermissions()
          .andThen(Completable.fromAction { launchPhotoCapture(fieldId) })
          .`as`(autoDisposable<Any>(viewLifecycleOwner))
          .subscribe()
      PhotoStorageResource.PHOTO_SOURCE_STORAGE ->
        // TODO: Launch intent is not invoked if the permission is not granted by default.
        viewModel
          .obtainSelectPhotoPermissions()
          .andThen(Completable.fromAction { launchPhotoSelector(fieldId) })
          .`as`(autoDisposable<Any>(viewLifecycleOwner))
          .subscribe()
      else -> throw IllegalArgumentException("Unknown type: $type")
    }
  }

  private fun launchPhotoCapture(taskId: String) {
    val photoFile = userMediaRepository.createImageFile(taskId)
    val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID, photoFile)
    viewModel.setTaskWaitingForPhoto(taskId)
    viewModel.setCapturedPhotoPath(photoFile.absolutePath)
    capturePhotoLauncher.launch(uri)
    Timber.d("Capture photo intent sent")
  }

  private fun launchPhotoSelector(taskId: String) {
    viewModel.setTaskWaitingForPhoto(taskId)
    selectPhotoLauncher.launch("image/*")
    Timber.d("Select photo intent sent")
  }

  companion object {
    /** Key used to store field ID waiting for photo taskData across activity re-creation.  */
    private const val TASK_WAITING_FOR_PHOTO = "dataCollectionPhotoFieldId"

    /** Key used to store captured photo Uri across activity re-creation.  */
    private const val CAPTURED_PHOTO_PATH = "dataCollectionCapturedPhotoPath"
  }
}
