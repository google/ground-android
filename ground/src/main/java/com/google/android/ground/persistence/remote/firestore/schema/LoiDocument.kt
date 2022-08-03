/*
 * Copyright 2020 Google LLC
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

package com.google.android.ground.persistence.remote.firestore.schema

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.IgnoreExtraProperties

/** LOI entity stored in Firestore.  */
@IgnoreExtraProperties
data class LoiDocument(
    val jobId: String?,
    val customId: String?,
    val caption: String?,
    val location: GeoPoint?,
    val geoJson: String?,
    val geometry: Map<String, Any>?,
    val created: AuditInfoNestedObject?,
    val lastModified: AuditInfoNestedObject?
)
