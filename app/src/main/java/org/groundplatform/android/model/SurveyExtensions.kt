/*
 * Copyright 2025 Google LLC
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

/**
 * Checks if a survey is usable for data collection. A survey is considered usable if it has at
 * least one predefined LOI or at least one job that allows ad hoc LOIs.
 */
fun Survey.isUsable(loiCount: Int = 0): Boolean {
  // If there are predefined LOIs, the survey is usable
  if (loiCount > 0) {
    return true
  }

  // If there's at least one job that allows ad hoc LOIs, the survey is usable
  return jobs.any { job ->
    job.strategy == Job.DataCollectionStrategy.AD_HOC ||
      job.strategy == Job.DataCollectionStrategy.MIXED
  }
}
