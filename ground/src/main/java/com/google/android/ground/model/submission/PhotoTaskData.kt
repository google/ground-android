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
package com.google.android.ground.model.submission

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import kotlinx.serialization.Serializable

/** Represents a single photo associated with a given [LocationOfInterest] and [Job]. */
@Serializable
class PhotoTaskData : TaskData {
  val surveyId: String
  val path: String

  constructor(path: String, surveyId: String) {
    val filePattern = Regex("^[a-zA-Z0-9._ -]+\\.(png|jpg)$")
    val filename = path.split("/").last()
    require(filename.matches(filePattern)) { "Invalid photo file name" }

    this.path = path
    this.surveyId = surveyId
  }

  val filename: String
    get() = path.split("/").last()

  override fun getDetailsText(): String = path

  override fun isEmpty(): Boolean = path.isEmpty()

  override fun toString(): String = path
}
