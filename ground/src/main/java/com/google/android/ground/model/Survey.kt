/*
 * Copyright 2018 Google LLC
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
package com.google.android.ground.model

import com.google.android.ground.model.basemap.TileOverlaySource
import com.google.android.ground.model.job.Job
import java8.util.Optional

/** Configuration, schema, and ACLs for a single survey. */
data class Survey(
  val id: String,
  val title: String,
  val description: String,
  val jobMap: Map<String, Job>,
  val tileOverlaySources: List<TileOverlaySource> = listOf(),
  val acl: Map<String, String> = mapOf()
) {
  val jobs: Collection<Job>
    get() = jobMap.values

  fun getJob(jobId: String): Optional<Job> = Optional.ofNullable(jobMap[jobId])
}
