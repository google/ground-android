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

import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.model.submission.TaskData
import com.google.android.ground.model.submission.TaskDataDelta
import com.google.android.ground.model.task.Task
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.datacollection.tasks.photo.PhotoResult
import com.google.android.ground.ui.util.BitmapUtil
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.io.IOException
import java.io.Serializable
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

class EditSubmissionViewModel
@Inject
internal constructor(
  private val resources: Resources,
  private val submissionRepository: SubmissionRepository, // States.
  private val bitmapUtil: BitmapUtil
) : AbstractViewModel() {

  // Injected dependencies.
  /** True if submission is currently being loaded, otherwise false. */
  @JvmField val isLoading: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData(false)

  /** True if submission is currently being saved, otherwise false. */
  val isSaving: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData(false)

  /** Job definition, loaded when view is initialized. */
  val job: LiveData<Job>

  /** Toolbar title, based on whether user is adding new or editing existing submission. */
  private val toolbarTitle: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()

  /** Current task responses. */
  private val responses: MutableMap<String, TaskData> = HashMap()

  /** Arguments passed in from view on initialize(). */
  private val viewArgs: @Hot(replays = true) FlowableProcessor<EditSubmissionFragmentArgs> =
    BehaviorProcessor.create()
  // TODO(#1146): Reduce duplicate photo capture logic between here and PhotoTaskViewModel.
  //  Note: This may be unnecessary since the EditSubmissionFragment may be removed altogether in
  //  favor of an "Edit" mode for the Data Collection Flow.
  /**
   * Emits the last photo task id updated and either its photo result, or empty if removed. The last
   * value is emitted on each subscription because {@see #onPhotoResult} is called before
   * subscribers are created.
   */
  private val lastPhotoResult: Subject<PhotoResult> = BehaviorSubject.create()

  /** "Save" button clicks. */
  private val saveClicks: @Hot PublishProcessor<Nil> = PublishProcessor.create()

  /** Outcome of user clicking "Save". */
  private val saveResults: Observable<SaveResult?>

  /** Task validation errors, updated when existing for loaded and when responses change. */
  private var validationErrors: Map<String, String>? = null
  // Events.
  /** Submission state loaded when view is initialized. */
  private var originalSubmission: Submission? = null

  /** True if the submission is being added, false if editing an existing one. */
  private var isNew = false

  /**
   * Task id waiting for a photo taskData. As only 1 photo result is returned at a time, we can
   * directly map it 1:1 with the task waiting for a photo taskData.
   */
  private var taskWaitingForPhoto: String? = null

  /**
   * Full path of the captured photo in local storage. In case of selecting a photo from storage,
   * URI is returned. But when capturing a photo using camera, we need to pass a valid URI and the
   * result returns true/false based on whether the operation passed or not. As only 1 photo result
   * is returned at a time, we can directly map it 1:1 with the path of the captured photo.
   */
  private var capturedPhotoPath: String? = null

  init {
    job =
      viewArgs
        .switchMapSingle { viewArgs: EditSubmissionFragmentArgs -> onInitialize(viewArgs) }
        .toLiveData()
    saveResults = saveClicks.toObservable().switchMapSingle { onSave() }
  }

  fun getToolbarTitle(): LiveData<String> {
    return toolbarTitle
  }

  val surveyId: String?
    get() = if (originalSubmission == null) null else originalSubmission!!.surveyId
  val submissionId: String?
    get() = if (originalSubmission == null) null else originalSubmission!!.id

  fun initialize(args: EditSubmissionFragmentArgs) {
    viewArgs.onNext(args)
  }

  private fun getResponse(taskId: String): TaskData? {
    return responses[taskId]
  }

  /**
   * Update the current value of a taskData. Called what tasks are initialized and on each
   * subsequent change.
   */
  fun setResponse(task: Task, newResponse: Optional<TaskData>) {
    newResponse.ifPresentOrElse({ r: TaskData -> responses[task.id] = r }) {
      responses.remove(task.id)
    }
  }

  fun onSaveClick(validationErrors: Map<String, String>?) {
    this.validationErrors = validationErrors
    saveClicks.onNext(Nil.NIL)
  }

  private fun onInitialize(viewArgs: EditSubmissionFragmentArgs): Single<Job> {
    isLoading.value = true
    isNew = isAddSubmissionRequest(viewArgs)
    val submissionSingle: Single<Submission> =
      if (isNew) {
        toolbarTitle.value = resources.getString(R.string.add_submission_toolbar_title)
        createSubmission(viewArgs)
      } else {
        toolbarTitle.setValue(resources.getString(R.string.edit_submission))
        loadSubmission(viewArgs)
      }
    val restoredResponses: HashMap<String, TaskData>? =
      viewArgs.restoredResponses as? HashMap<String, TaskData>
    return submissionSingle
      .doOnSuccess { loadedSubmission: Submission ->
        onSubmissionLoaded(loadedSubmission, restoredResponses)
      }
      .map(Submission::job)
  }

  private fun onSubmissionLoaded(
    submission: Submission,
    restoredResponses: Map<String, TaskData>?
  ) {
    Timber.v("Submission loaded")
    originalSubmission = submission
    responses.clear()
    if (restoredResponses == null) {
      val taskDataMap = submission.responses
      for (taskId in taskDataMap.taskIds()) {
        val taskData = taskDataMap.getResponse(taskId)
        if (taskData != null) {
          responses[taskId] = taskData
        }
      }
    } else {
      Timber.v("Restoring responses from bundle")
      responses.putAll(restoredResponses)
    }
    isLoading.postValue(false)
  }

  private fun createSubmission(args: EditSubmissionFragmentArgs): Single<Submission> {
    return submissionRepository
      .createSubmission(args.surveyId, args.locationOfInterestId)
      .onErrorResumeNext { throwable: Throwable -> onError(throwable) }
  }

  private fun loadSubmission(args: EditSubmissionFragmentArgs): Single<Submission> {
    return submissionRepository
      .getSubmission(args.surveyId, args.locationOfInterestId, args.submissionId)
      .onErrorResumeNext { throwable: Throwable -> onError(throwable) }
  }

  private fun onSave(): Single<SaveResult?> {
    if (originalSubmission == null) {
      Timber.e("Save attempted before submission loaded")
      return Single.just(SaveResult.NO_CHANGES_TO_SAVE)
    }
    if (hasValidationErrors()) {
      return Single.just(SaveResult.HAS_VALIDATION_ERRORS)
    }
    return if (!hasUnsavedChanges()) {
      Single.just(SaveResult.NO_CHANGES_TO_SAVE)
    } else save()
  }

  private fun <T> onError(throwable: Throwable): Single<T> {
    // TODO: Refactor and stream to UI.
    Timber.e(throwable, "Error")
    return Single.never()
  }

  private fun save(): Single<SaveResult?> {
    return if (originalSubmission == null) {
      Single.error(IllegalStateException("Submission is null"))
    } else
      submissionRepository
        .createOrUpdateSubmission(originalSubmission!!, responseDeltas, isNew)
        .doOnSubscribe { isSaving.postValue(true) }
        .doOnComplete { isSaving.postValue(false) }
        .toSingleDefault(SaveResult.SAVED)
  }

  private val responseDeltas: List<TaskDataDelta>
    get() {
      if (originalSubmission == null) {
        Timber.e("TaskData diff attempted before submission loaded")
        return listOf()
      }
      val result: MutableList<TaskDataDelta> = ArrayList()
      val originalResponses = originalSubmission!!.responses
      Timber.v("Responses:\n Before: %s \nAfter:  %s", originalResponses, responses)
      for ((taskId, _, type) in originalSubmission!!.job.tasksSorted) {
        val originalResponse = originalResponses.getResponse(taskId)
        val currentResponse = getResponse(taskId)
        if (currentResponse != null && currentResponse == originalResponse) {
          continue
        }
        result.add(TaskDataDelta(taskId, type, currentResponse))
      }
      Timber.v("Deltas: %s", result)
      return result
    }

  fun hasUnsavedChanges(): Boolean {
    return responseDeltas.isNotEmpty()
  }

  private fun hasValidationErrors(): Boolean {
    return validationErrors != null && validationErrors!!.isNotEmpty()
  }

  val draftResponses: Serializable
    get() = HashMap(responses)

  fun getLastPhotoResult(): Observable<PhotoResult> {
    return lastPhotoResult
  }

  fun onSelectPhotoResult(uri: Uri?) {
    if (uri == null) {
      Timber.v("Select photo failed or canceled")
      return
    }
    if (taskWaitingForPhoto == null) {
      Timber.e("Photo captured but no task waiting for the result")
      return
    }
    try {
      onPhotoResult(PhotoResult(taskWaitingForPhoto!!, bitmapUtil.fromUri(uri)))
      Timber.v("Select photo result returned")
    } catch (e: IOException) {
      Timber.e(e, "Error getting photo selected from storage")
    }
  }

  fun onCapturePhotoResult(result: Boolean) {
    if (!result) {
      Timber.v("Capture photo failed or canceled")
      // TODO: Cleanup created file if it exists.
      return
    }
    if (taskWaitingForPhoto == null) {
      Timber.e("Photo captured but no task waiting for the result")
      return
    }
    if (capturedPhotoPath == null) {
      Timber.e("Photo captured but no path available to read the result")
      return
    }
    onPhotoResult(PhotoResult(taskWaitingForPhoto!!, /* bitmap=*/ null, capturedPhotoPath))
    Timber.v("Photo capture result returned")
  }

  private fun onPhotoResult(result: PhotoResult) {
    capturedPhotoPath = null
    taskWaitingForPhoto = null
    lastPhotoResult.onNext(result)
  }

  /** Possible outcomes of user clicking "Save". */
  enum class SaveResult {
    HAS_VALIDATION_ERRORS,
    NO_CHANGES_TO_SAVE,
    SAVED
  }

  companion object {
    private fun isAddSubmissionRequest(args: EditSubmissionFragmentArgs): Boolean {
      return args.submissionId.isEmpty()
    }
  }
}