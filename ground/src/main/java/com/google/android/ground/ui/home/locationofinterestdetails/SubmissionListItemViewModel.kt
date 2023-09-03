/*
 * Copyright 2019 Google LLC
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

import android.app.Application
import android.text.format.DateFormat
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.model.submission.Submission
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.common.base.Preconditions
import java8.util.function.Consumer
import javax.inject.Inject

class SubmissionListItemViewModel
@Inject
internal constructor(private val application: Application) :
  AbstractViewModel(), View.OnClickListener {

  @JvmField val userName: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()
  @JvmField val modifiedDate: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()
  @JvmField val modifiedTime: @Hot(replays = true) MutableLiveData<String> = MutableLiveData()
  private var submissionCallback: Consumer<Submission?>? = null
  private val selectedSubmission: @Hot(replays = true) MutableLiveData<Submission> =
    MutableLiveData()

  override fun onClick(view: View) {
    Preconditions.checkNotNull(submissionCallback, "submissionCallback is null")
    submissionCallback!!.accept(selectedSubmission.value)
  }

  fun setSubmission(submission: Submission) {
    selectedSubmission.postValue(submission)
    val (createdBy, creationTime) = submission.created
    userName.value = createdBy.displayName
    modifiedDate.value = DateFormat.getMediumDateFormat(application).format(creationTime)
    modifiedTime.value = DateFormat.getTimeFormat(application).format(creationTime)
  }

  fun setSubmissionCallback(submissionCallback: Consumer<Submission?>?) {
    this.submissionCallback = submissionCallback
  }
}
