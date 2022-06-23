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

package com.google.android.gnd.model.job;

import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.task.Task;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;

@AutoValue
public abstract class Job {
  public abstract String getId();

  public abstract String getName();

  public abstract Optional<Task> getTask();

  public Optional<Task> getTask(String taskId) {
    return getTask().filter(task -> task.getId().equals(taskId));
  }

  /** Returns the list of feature types the current user may add to this job. */
  public abstract ImmutableList<FeatureType> getUserCanAdd();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_Job.Builder()
        .setTask(Optional.empty())
        .setUserCanAdd(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setName(String newName);

    public abstract Builder setTask(Optional<Task> task);

    public abstract Builder setUserCanAdd(ImmutableList<FeatureType> userCanAdd);

    public Builder setTask(Task task) {
      return setTask(Optional.of(task));
    }

    public abstract Job build();
  }
}
