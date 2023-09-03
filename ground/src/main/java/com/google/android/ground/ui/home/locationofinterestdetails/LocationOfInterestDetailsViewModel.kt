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

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import com.google.android.ground.Config.DEFAULT_LOI_ZOOM_LEVEL
import com.google.android.ground.R
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.model.mutation.SubmissionMutation
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.MarkerIconFactory
import com.google.android.ground.ui.common.LocationOfInterestHelper
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.util.DrawableUtil
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import java8.util.Optional
import javax.inject.Inject

@SharedViewModel
class LocationOfInterestDetailsViewModel
@Inject
constructor(
  markerIconFactory: MarkerIconFactory,
  drawableUtil: DrawableUtil,
  locationOfInterestHelper: LocationOfInterestHelper,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val submissionRepository: SubmissionRepository
) : ViewModel() {

  private val selectedLocationOfInterest: @Hot FlowableProcessor<Optional<LocationOfInterest>> =
    BehaviorProcessor.createDefault(Optional.empty())

  val isUploadPendingIconVisible: LiveData<Boolean>
  val markerBitmap: Bitmap
  val subtitle: LiveData<String>
  val title: LiveData<String>

  init {
    markerBitmap =
      markerIconFactory.getMarkerBitmap(
        drawableUtil.getColor(R.color.md_theme_onSurfaceVariant),
        DEFAULT_LOI_ZOOM_LEVEL,
        false
      )
    title =
      selectedLocationOfInterest
        .map { locationOfInterest: Optional<LocationOfInterest>? ->
          locationOfInterestHelper.getLabel(locationOfInterest!!)
        }
        .toLiveData()
    subtitle =
      selectedLocationOfInterest
        .map { locationOfInterest: Optional<LocationOfInterest>? ->
          locationOfInterestHelper.getSubtitle(locationOfInterest!!)
        }
        .toLiveData()
    val locationOfInterestMutations =
      selectedLocationOfInterest.switchMap {
        selectedLocationOfInterest: Optional<LocationOfInterest> ->
        getIncompleteLocationOfInterestMutationsOnceAndStream(selectedLocationOfInterest)
      }
    val submissionMutations =
      selectedLocationOfInterest.switchMap {
        selectedLocationOfInterest: Optional<LocationOfInterest> ->
        getIncompleteSubmissionMutationsOnceAndStream(selectedLocationOfInterest)
      }
    isUploadPendingIconVisible =
      Flowable.combineLatest(locationOfInterestMutations, submissionMutations) {
          f: List<LocationOfInterestMutation>,
          o: List<SubmissionMutation> ->
          f.isNotEmpty() && o.isNotEmpty()
        }
        .toLiveData()
  }

  private fun getIncompleteLocationOfInterestMutationsOnceAndStream(
    selectedLocationOfInterest: Optional<LocationOfInterest>
  ): Flowable<List<LocationOfInterestMutation>> =
    selectedLocationOfInterest
      .map {
        locationOfInterestRepository.getIncompleteLocationOfInterestMutationsOnceAndStream(it.id)
      }
      .orElse(Flowable.just(listOf()))

  private fun getIncompleteSubmissionMutationsOnceAndStream(
    selectedLocationOfInterest: Optional<LocationOfInterest>
  ): Flowable<List<SubmissionMutation>> =
    selectedLocationOfInterest
      .map { (id, surveyId): LocationOfInterest ->
        submissionRepository.getIncompleteSubmissionMutationsOnceAndStream(surveyId, id)
      }
      .orElse(Flowable.just(listOf()))

  /**
   * Returns a LiveData that immediately emits the selected LOI (or empty) on if none selected to
   * each new observer.
   */
  fun getSelectedLocationOfInterestOnceAndStream(): LiveData<Optional<LocationOfInterest>> =
    selectedLocationOfInterest.toLiveData()

  fun onLocationOfInterestSelected(locationOfInterest: Optional<LocationOfInterest>) {
    selectedLocationOfInterest.onNext(locationOfInterest)
  }
}
