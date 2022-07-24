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

import androidx.annotation.Nullable;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Collections;
import java.util.Map;
import java8.util.Objects;

/** Firestore representation of task definitions. */
@IgnoreExtraProperties
class TaskNestedObject {

  @Nullable private Map<String, StepNestedObject> steps;

  @SuppressWarnings("unused")
  public TaskNestedObject() {}

  @SuppressWarnings("unused")
  TaskNestedObject(@Nullable Map<String, StepNestedObject> steps) {
    this.steps = steps;
  }

  public Map<String, StepNestedObject> getSteps() {
    return Objects.requireNonNullElse(steps, Collections.emptyMap());
  }
}
