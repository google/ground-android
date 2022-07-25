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

import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.ground.model.task.Step;
import com.google.android.ground.model.task.Task;
import com.google.common.collect.ImmutableList;
import java.util.Map;

/** Converts between Firestore nested objects and {@link Task} instances. */
class TaskConverter {

  static Task toTask(String taskId, TaskNestedObject obj) {
    return Task.newBuilder().setId(taskId).setSteps(toList(obj.getSteps())).build();
  }

  private static ImmutableList<Step> toList(Map<String, StepNestedObject> steps) {
    return stream(steps.entrySet())
        .map(e -> StepConverter.toStep(e.getKey(), e.getValue()))
        .collect(toImmutableList());
  }
}
