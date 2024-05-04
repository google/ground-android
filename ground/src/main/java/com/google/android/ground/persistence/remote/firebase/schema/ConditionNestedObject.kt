/*
 * Copyright 2024 Google LLC
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

/** Firestore representation of a task condition. */
@IgnoreExtraProperties
data class ConditionNestedObject(
  val matchType: String? = null,
  val expressions: List<ExpressionNestedObject>? = null,
)

/** Firestore representation of a task condition expression. */
@IgnoreExtraProperties
data class ExpressionNestedObject(
  val expressionType: String? = null,
  val taskId: String? = null,
  val optionIds: List<String>? = null,
)
