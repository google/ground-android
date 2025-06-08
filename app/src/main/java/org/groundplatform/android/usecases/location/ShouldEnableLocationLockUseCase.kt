/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.usecases.location

import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import javax.inject.Inject

/** Use case to determine if location lock should be enabled for a survey. */
class ShouldEnableLocationLockUseCase
@Inject
constructor(
  private val loiRepository: LocationOfInterestRepository,
  private val mapStateRepository: MapStateRepository,
) {

  /** Returns true if location lock should be enabled for the survey. */
  suspend operator fun invoke(surveyId: String): Boolean {
    if (loiRepository.hasValidLois(surveyId)) return false
    if (mapStateRepository.getCameraPosition(surveyId) != null) return false
    return true
  }
}
