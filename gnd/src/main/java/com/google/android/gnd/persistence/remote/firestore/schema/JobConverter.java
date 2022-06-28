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

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.util.Localization.getLocalizedMessage;

import com.google.android.gnd.model.job.Job;
import timber.log.Timber;

/**
 * Converts between Firestore documents and {@link Job} instances.
 */
class JobConverter {

  static Job toJob(String id, JobNestedObject obj) {
    Job.Builder builder = Job.newBuilder();
    builder.setId(id).setName(getLocalizedMessage(obj.getName()));
    if (obj.getTasks() != null && !obj.getTasks().isEmpty()) {
      if (obj.getTasks().size() > 1) {
        Timber.e("Multiple forms not supported");
      }
      String taskId = obj.getTasks().keySet().iterator().next();
      builder.setTask(TaskConverter.toTask(taskId, obj.getTasks().get(taskId)));
    }
    return builder.build();
  }
}
