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
package com.google.android.ground.ui.map

/**
 * Indicates the type of a geometric model object. Used to interpret other objects (e.g. map
 * features) back into model objects.
 */
enum class FeatureType {
  UNKNOWN,
  LOCATION_OF_INTEREST,
  USER_POINT,
  USER_POLYGON,
}

fun Feature.isLocationOfInterest(): Boolean = tag.type == FeatureType.LOCATION_OF_INTEREST.ordinal
