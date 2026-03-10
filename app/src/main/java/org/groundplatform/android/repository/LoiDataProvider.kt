/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.repository

import javax.inject.Inject
import org.groundplatform.domain.usecases.LoiDataProviderInterface

class LoiDataProvider
@Inject
constructor(private val locationOfInterestRepository: LocationOfInterestRepository) :
  LoiDataProviderInterface {
  override suspend fun get(surveyId: String, loiId: String): LoiDataProviderInterface.LoiData? {
    val loi = locationOfInterestRepository.getOfflineLoi(surveyId, loiId)
    return loi?.let {
      LoiDataProviderInterface.LoiData(geometry = it.geometry, properties = it.properties)
    }
  }
}
