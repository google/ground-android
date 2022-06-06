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

package com.google.android.gnd.model.submission;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.task.Task;
import com.google.auto.value.AutoValue;

/**
 * Represents a single instance of data collected about a specific {@link Feature}.
 */
@AutoValue
public abstract class Submission {

  public abstract String getId();

  public abstract Survey getSurvey();

  public abstract Feature getFeature();

  public abstract Task getTask();

  /**
   * Returns the user and time audit info pertaining to the creation of this submission.
   */
  public abstract AuditInfo getCreated();

  /**
   * Returns the user and time audit info pertaining to the last modification of this submission.
   */
  public abstract AuditInfo getLastModified();

  public abstract ResponseMap getResponses();

  public static Builder newBuilder() {
    return new AutoValue_Submission.Builder().setResponses(ResponseMap.builder().build());
  }

  public abstract Submission.Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String newId);

    public abstract Builder setSurvey(Survey survey);

    public abstract Builder setFeature(Feature feature);

    public abstract Builder setTask(Task task);

    public abstract Builder setCreated(AuditInfo newCreated);

    public abstract Builder setLastModified(AuditInfo newLastModified);

    public abstract Builder setResponses(ResponseMap responses);

    public abstract Submission build();
  }
}
