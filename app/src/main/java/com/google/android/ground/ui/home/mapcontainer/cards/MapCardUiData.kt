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
package com.google.android.ground.ui.home.mapcontainer.cards

import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest

/** Data classes used to populate the Map cards (either an Loi card, or a Suggest Loi card). */
sealed class MapCardUiData(open val hasWriteAccess: Boolean, open val survey: Survey?)

data class LoiCardUiData(
  override val hasWriteAccess: Boolean,
  override val survey: Survey?,
  val loi: LocationOfInterest,
) : MapCardUiData(hasWriteAccess, survey)

data class AddLoiCardUiData(
  override val hasWriteAccess: Boolean,
  override val survey: Survey?,
  val job: Job,
) : MapCardUiData(hasWriteAccess, survey)
