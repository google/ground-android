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

package com.google.android.ground.persistence.remote.firestore.schema;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.job.Job.Builder;

/** Converts between Firestore documents and {@link Job} instances. */
class JobConverter {

  static Job toJob(String id, JobNestedObject obj) {
    Builder builder = Job.newBuilder();
    builder.setId(id).setName(obj.getName());
    if (obj.getTasks() != null) {
      stream(obj.getTasks().entrySet())
          .map(it -> TaskConverter.toTask(it.getKey(), it.getValue()))
          .forEach(it -> it.ifPresent(builder::addTask));
    }
    return builder.build();
  }
}
