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

package com.google.android.ground.model.job;

import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.ground.model.locationofinterest.LocationOfInterestType;
import com.google.android.ground.model.task.Task;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java8.util.Comparators;
import java8.util.Optional;

@AutoValue
public abstract class Job {
  public abstract String getId();

  public abstract String getName();

  public abstract ImmutableMap<String, Task> getTasks();

  public ImmutableList<Task> getTasksSorted() {
    return stream(getTasks().values())
        .sorted(Comparators.comparing(e -> e.getIndex()))
        .collect(toImmutableList());
  }

  public Optional<Task> getTask(String id) {
    return Optional.ofNullable(getTasks().get(id));
  }

  /** Returns the list of location of interest types the current user may add to this job. */
  public abstract ImmutableList<LocationOfInterestType> getUserCanAdd();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_Job.Builder().setTask(Optional.empty()).setUserCanAdd(ImmutableList.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setName(String newName);

    public abstract ImmutableMap.Builder<String, Task> tasksBuilder();

    public abstract Builder setUserCanAdd(ImmutableList<LocationOfInterestType> userCanAdd);

    public abstract Job build();

    public void addTask(Task task) {
      tasksBuilder().put(task.getId(), task);
    }
  }
}
