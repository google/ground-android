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

import androidx.annotation.Nullable;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

/** Firestore representation of map layers. */
@IgnoreExtraProperties
class JobNestedObject {
  @Nullable private Map<String, String> name;
  @Nullable private Map<String, TaskNestedObject> tasks;

  @SuppressWarnings("unused")
  public JobNestedObject() {}

  @SuppressWarnings("unused")
  JobNestedObject(
      @Nullable Map<String, String> name, @Nullable Map<String, TaskNestedObject> tasks) {
    this.name = name;
    this.tasks = tasks;
  }

  @Nullable
  public Map<String, String> getName() {
    return name;
  }

  @Nullable
  public Map<String, TaskNestedObject> getTasks() {
    return tasks;
  }
}
