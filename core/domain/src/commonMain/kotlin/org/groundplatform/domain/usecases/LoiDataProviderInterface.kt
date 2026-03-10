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
package org.groundplatform.domain.usecases

import org.groundplatform.domain.model.geometry.Geometry
import org.groundplatform.domain.model.locationofinterest.LoiProperties

// TODO: Extract LocationOfInterestRepositoryInterface to domain module
// This is a temporary abstraction while the repository interface is still not fully decoupled from
// Android-specific types
interface LoiDataProviderInterface {
  /** Returns the Geometry for an existing LOI, or null if not found. */
  suspend fun get(surveyId: String, loiId: String): LoiData?

  data class LoiData(val geometry: Geometry, val properties: LoiProperties)
}
