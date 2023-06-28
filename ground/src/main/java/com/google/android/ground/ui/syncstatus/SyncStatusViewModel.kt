/*
 * Copyright 2021 Google LLC
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
package com.google.android.ground.ui.syncstatus

import android.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.Mutation
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MutationRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.AbstractViewModel
import io.reactivex.Flowable
import io.reactivex.Single
import java8.util.Optional
import javax.inject.Inject

/**
 * View model for the offline area manager fragment. Handles the current list of downloaded areas.
 */
class SyncStatusViewModel
@Inject
internal constructor(
  private val mutationRepository: MutationRepository,
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository
) : AbstractViewModel() {

  val mutations: LiveData<List<Pair<LocationOfInterest, Mutation>>>

  init {
    mutations = mutationsOnceAndStream.switchMap { loadLocationsOfInterestAndPair(it) }.toLiveData()
  }

  private val mutationsOnceAndStream: Flowable<List<Mutation>>
    get() =
      surveyRepository.activeSurveyFlowable.switchMap { survey: Optional<Survey> ->
        survey
          .map { mutationRepository.getMutationsOnceAndStream(it) }
          .orElse(Flowable.just(listOf()))
      }

  // TODO: Replace with kotlin coroutine
  private fun loadLocationsOfInterestAndPair(
    mutations: List<Mutation>
  ): Flowable<List<Pair<LocationOfInterest, Mutation>>> =
    Single.merge(mutations.map { loadLocationOfInterestAndPair(it) }).toList().toFlowable()

  // TODO: Replace with kotlin coroutine
  private fun loadLocationOfInterestAndPair(
    mutation: Mutation
  ): Single<Pair<LocationOfInterest, Mutation>> =
    locationOfInterestRepository
      .getOfflineLocationOfInterest(mutation.surveyId, mutation.locationOfInterestId)
      .map { locationOfInterest: LocationOfInterest -> Pair.create(locationOfInterest, mutation) }
}
