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

import com.google.android.ground.model.basemap.BaseMap
import com.google.android.ground.model.job.Job
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java8.util.Optional

/** Configuration, schema, and ACLs for a single survey. */
data class Survey
@JvmOverloads
constructor(
  val id: String,
  val title: String,
  val description: String,
  val jobMap: ImmutableMap<String, Job>,
  val baseMaps: ImmutableList<BaseMap> = ImmutableList.of(),
  val acl: ImmutableMap<String, String> = ImmutableMap.of()
) {
  val jobs: ImmutableList<Job>
    get() = jobMap.values.asList()

  fun getJob(jobId: String): Optional<Job> {
    return Optional.of(jobMap[jobId])
  }
}
