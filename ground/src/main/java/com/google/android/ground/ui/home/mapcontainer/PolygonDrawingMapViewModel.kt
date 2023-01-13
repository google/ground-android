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
import com.google.android.ground.ui.map.*
import com.google.android.ground.util.toImmutableSet
import com.google.common.collect.ImmutableSet
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor

class PolygonDrawingMapViewModel(
  locationController: LocationController,
  mapController: MapController
) : BaseMapViewModel(locationController, mapController) {
  // Set of polygon map [Features] drawn by the user.
  val userPolygonFeatures: LiveData<ImmutableSet<Feature>>

  /** Temporary set of [Feature]s used for displaying on map during add/edit flows. */
  private val unsavedUserPolygonFeatures: @Hot PublishProcessor<ImmutableSet<Feature>> =
    PublishProcessor.create()

  override fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    TODO("Not yet implemented")
  }

  /** Set the current unsaved user drawn polygon map [Feature]s. */
  fun setUnsavedUserPolygonFeatures(locationsOfInterest: ImmutableSet<Feature>) =
    unsavedUserPolygonFeatures.onNext(locationsOfInterest)

  init {
    userPolygonFeatures =
      LiveDataReactiveStreams.fromPublisher(
        Flowable.combineLatest(
            listOf(unsavedUserPolygonFeatures.startWith(ImmutableSet.of<Feature>()))
          ) {
            concatLocationsOfInterestSets(it)
          }
          .distinctUntilChanged()
      )
  }

  companion object {
    private fun concatLocationsOfInterestSets(objects: Array<Any>): ImmutableSet<Feature> =
      listOf(*objects).flatMap { it as ImmutableSet<Feature> }.toImmutableSet()
  }
}
