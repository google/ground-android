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
package com.google.android.ground.ui.home.mapcontainer

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Geometry
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.map.gms.toLatLng
import com.google.common.collect.ImmutableSet
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject

/** Provides data for displaying cards for visible LOIs at the bottom of the screen. */
class LoiCardSource
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
) {

  private val cameraBoundsSubject: @Hot Subject<LatLngBounds> = PublishSubject.create()
  val locationsOfInterest: LiveData<List<LocationOfInterest>>

  init {
    val loiStream = getAllLocationsOfInterest()

    locationsOfInterest =
      LiveDataReactiveStreams.fromPublisher(
        getCameraBoundUpdates()
          .flatMap { bounds -> loiStream.map { it.toLoiCardsWithinBounds(bounds) } }
          .distinctUntilChanged()
      )
  }

  fun onCameraBoundsUpdated(newBounds: LatLngBounds?) {
    newBounds?.let { cameraBoundsSubject.onNext(it) }
  }

  /** Returns a flowable of [LatLngBounds] whenever camera moves. */
  private fun getCameraBoundUpdates(): Flowable<LatLngBounds> =
    cameraBoundsSubject.toFlowable(BackpressureStrategy.LATEST).distinctUntilChanged()

  /** Returns a flowable of all [LocationOfInterest] for the selected [Survey]. */
  private fun getAllLocationsOfInterest(): Flowable<ImmutableSet<LocationOfInterest>> =
    surveyRepository.activeSurvey
      .switchMap { survey ->
        survey
          .map { locationOfInterestRepository.getLocationsOfInterestOnceAndStream(it) }
          .orElse(Flowable.just(ImmutableSet.of()))
      }
      .distinctUntilChanged()

  /** Filters all [LocationOfInterest] within [bounds]. */
  private fun ImmutableSet<LocationOfInterest>.toLoiCardsWithinBounds(
    bounds: LatLngBounds
  ): List<LocationOfInterest> = this.filter { isGeometryWithinBounds(it.geometry, bounds) }

  /** Returns true if the provided [geometry] is within [bounds]. */
  private fun isGeometryWithinBounds(geometry: Geometry, bounds: LatLngBounds): Boolean =
    geometry.vertices.any { bounds.contains(it.toLatLng()) }
}
