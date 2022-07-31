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

import static com.google.android.ground.util.Enums.toEnum;

import com.google.android.ground.model.task.Task;
import java8.util.Objects;
import java8.util.Optional;
import timber.log.Timber;

/** Converts between Firestore nested objects and {@link Task} instances. */
class TaskConverter {

  static Optional<Task> toTask(String id, TaskNestedObject em) {
    Task.Type type = toEnum(Task.Type.class, em.getType());
    if (type == Task.Type.UNKNOWN) {
      Timber.d("Unsupported task type: " + em.getType());
      return Optional.empty();
    }
    Task.Builder task = Task.newBuilder();
    task.setType(type);
    if (type == Task.Type.MULTIPLE_CHOICE) {
      task.setMultipleChoice(MultipleChoiceConverter.toMultipleChoice(em));
    }
    task.setRequired(em.getRequired() != null && em.getRequired());
    task.setId(id);
    // Default index to -1 to degrade gracefully on older dev db instances and surveys.
    task.setIndex(Objects.requireNonNullElse(em.getIndex(), -1));
    task.setLabel(em.getLabel());
    return Optional.of(task.build());
  }
}
