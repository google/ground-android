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

typealias SubmissionDataNestedMap = Map<String, Any>

/** Submission entity stored in Firestore. */
@IgnoreExtraProperties
data class SubmissionDocument(
  val loiId: String? = null,
  val jobId: String? = null,
  val created: AuditInfoNestedObject? = null,
  val lastModified: AuditInfoNestedObject? = null,
  // TODO(#2058): Remove after dev databases are updated to use `data` field.
  val data: SubmissionDataNestedMap? = null,
  val responses: SubmissionDataNestedMap? = null,
)
