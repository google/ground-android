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

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.ground.BR
import com.google.android.ground.BuildConfig
import com.google.android.ground.R
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.databinding.PhotoTaskFragBinding
import com.google.android.ground.repository.UserMediaRepository
import com.google.android.ground.rx.RxAutoDispose.autoDisposable
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.ui.common.AbstractFragment
import com.google.android.ground.ui.editsubmission.PhotoResult
import com.google.android.ground.ui.editsubmission.PhotoTaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import timber.log.Timber

/** Fragment allowing the user to capture a photo to complete a task. */
@AndroidEntryPoint
class PhotoTaskFragment : AbstractFragment(), TaskFragment<PhotoTaskViewModel> {
  @Inject lateinit var userMediaRepository: UserMediaRepository
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope
  @Inject lateinit var permissionsManager: PermissionsManager

  private val dataCollectionViewModel: DataCollectionViewModel by
    hiltNavGraphViewModels(R.id.data_collection)
  override lateinit var viewModel: PhotoTaskViewModel
  override var position by Delegates.notNull<Int>()
  private lateinit var selectPhotoLauncher: ActivityResultLauncher<String>
  private lateinit var capturePhotoLauncher: ActivityResultLauncher<Uri>
  private var hasRequestedPermissionsOnResume = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      position = savedInstanceState.getInt(TaskFragment.POSITION)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)

    viewModel = dataCollectionViewModel.getTaskViewModel(position) as PhotoTaskViewModel
    selectPhotoLauncher =
      registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.onSelectPhotoResult(uri)
      }
    capturePhotoLauncher =
      registerForActivityResult(ActivityResultContracts.TakePicture()) { result: Boolean ->
        viewModel.onCapturePhotoResult(result)
      }

    val binding = PhotoTaskFragBinding.inflate(inflater, container, false)

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

  override fun onResume() {
    super.onResume()

    if (!hasRequestedPermissionsOnResume) {
      obtainCapturePhotoPermissions()
      hasRequestedPermissionsOnResume = true
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(TASK_WAITING_FOR_PHOTO, viewModel.getTaskWaitingForPhoto())
    outState.putString(CAPTURED_PHOTO_PATH, viewModel.getCapturedPhotoPath())
    outState.putInt(TaskFragment.POSITION, position)
  }

  private fun observeSelectPhotoClicks() {
    viewModel.getTakePhotoClicks().`as`(autoDisposable(viewLifecycleOwner)).subscribe {
      onTakePhoto()
    }
  }

  private fun observePhotoResults() {
    viewModel
      .getLastPhotoResult()
      .`as`(autoDisposable<PhotoResult>(viewLifecycleOwner))
      .subscribe { photoResult -> viewModel.onPhotoResult(photoResult) }
  }

  private fun obtainCapturePhotoPermissions(onPermissionsGranted: () -> Unit = {}) {
    externalScope.launch {
      try {
        permissionsManager.obtainPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE).await()

        permissionsManager.obtainPermission(Manifest.permission.CAMERA).await()

        onPermissionsGranted()
      } catch (_: PermissionDeniedException) {
        // TODO: Handle permission denied case
      }
    }
  }

  private fun onTakePhoto() {
    // TODO(#1600): Launch intent is not invoked if the permission is not granted by default.
    obtainCapturePhotoPermissions { launchPhotoCapture(viewModel.task.id) }
  }

  private fun launchPhotoCapture(taskId: String) {
    val photoFile = userMediaRepository.createImageFile(taskId)
    val uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID, photoFile)
    viewModel.setTaskWaitingForPhoto(taskId)
    viewModel.setCapturedPhotoPath(photoFile.absolutePath)
    capturePhotoLauncher.launch(uri)
    Timber.d("Capture photo intent sent")
  }

  companion object {
    /** Key used to store field ID waiting for photo taskData across activity re-creation. */
    private const val TASK_WAITING_FOR_PHOTO = "dataCollectionPhotoFieldId"

    /** Key used to store captured photo Uri across activity re-creation. */
    private const val CAPTURED_PHOTO_PATH = "dataCollectionCapturedPhotoPath"
  }
}
