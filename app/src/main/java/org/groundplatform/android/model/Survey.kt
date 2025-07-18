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
package org.groundplatform.android.model

import org.groundplatform.android.model.job.Job
import org.groundplatform.android.proto.Survey

/**
 * Configuration, schema, and Access Control Lists (ACLs) for a single survey.
 *
 * @param id Unique identifier for the survey.
 * @param title The title of the survey.
 * @param description A detailed description of the survey.
 * @param jobMap A mapping from job IDs to their respective job details.
 * @param acl A map defining user roles and permissions for accessing the survey.
 * @param dataSharingTerms Terms governing how data collected through this survey can be shared.
 * @param dataVisibility Specifies the visibility level for Locations of Interest (LOIs) associated
 *   with this survey.
 */
data class Survey(
  val id: String,
  val title: String,
  val description: String,
  val jobMap: Map<String, Job>,
  val acl: Map<String, String> = mapOf(),
  val dataSharingTerms: Survey.DataSharingTerms? = null,
  val generalAccess: Survey.GeneralAccess?,
  val dataVisibility: Survey.DataVisibility? = null,
) {
  val jobs: Collection<Job>
    get() = jobMap.values

  fun getJob(jobId: String): Job? = jobMap[jobId]

  /**
   * Returns the role assigned to the specified email, or throws an error if the user is not found
   * or if the role is not recognized.
   */
  fun getRole(email: String): Role {
    val roleString = acl[email] ?: error("ACL not found for email $email in survey $title")
    return Role.valueOf(roleString.uppercase())
  }
}
