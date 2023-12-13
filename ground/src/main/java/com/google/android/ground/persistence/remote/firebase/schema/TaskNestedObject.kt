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

package com.google.android.ground.persistence.remote.firebase.schema

import com.google.firebase.firestore.IgnoreExtraProperties

/** Firestore representation of a data collection task. */
@IgnoreExtraProperties
data class TaskNestedObject(
  val index: Int? = null,
  val type: String? = null,
  val cardinality: String? = null,
  val label: String? = null,
  val options: Map<String, OptionNestedObject>? = null,
  val required: Boolean? = null,
  val isAddLoiTask: Boolean? = false
)
