/*
 * Copyright 2023 Google LLC
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
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.LocationController
import com.google.android.ground.ui.map.MapController
import com.google.android.ground.ui.map.MapLocationOfInterest
import com.google.android.ground.util.toImmutableSet
import com.google.common.collect.ImmutableSet
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

class PolygonDrawingMapViewModel(
  locationController: LocationController,
  mapController: MapController
) : BaseMapViewModel(locationController, mapController) {
  val mapLocationsOfInterest: LiveData<ImmutableSet<MapLocationOfInterest>>

  /** Temporary set of [MapLocationOfInterest] used for displaying on map during add/edit flows. */
  private val unsavedMapLocationsOfInterest:
    @Hot
    PublishProcessor<ImmutableSet<MapLocationOfInterest>> =
    PublishProcessor.create()

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    TODO("Not yet implemented")
  }

  fun setUnsavedMapLocationsOfInterest(locationsOfInterest: ImmutableSet<MapLocationOfInterest>) =
    unsavedMapLocationsOfInterest.onNext(locationsOfInterest)

  init {
    mapLocationsOfInterest =
      LiveDataReactiveStreams.fromPublisher(
        Flowable.combineLatest(
            listOf(
              unsavedMapLocationsOfInterest.startWith(ImmutableSet.of<MapLocationOfInterest>())
            )
          ) { concatLocationsOfInterestSets(it) }
          .distinctUntilChanged()
      )
  }

  companion object {
    private fun concatLocationsOfInterestSets(
      objects: Array<Any>
    ): ImmutableSet<MapLocationOfInterest> =
      listOf(*objects).flatMap { it as ImmutableSet<MapLocationOfInterest> }.toImmutableSet()
  }
}
