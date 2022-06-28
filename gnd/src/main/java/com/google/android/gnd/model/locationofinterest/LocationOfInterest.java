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

package com.google.android.gnd.model.locationofinterest;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.job.Job;
import com.google.android.gnd.model.mutation.LocationOfInterestMutation;
import com.google.android.gnd.model.mutation.Mutation.SyncStatus;
import com.google.android.gnd.model.mutation.Mutation.Type;
import java.util.Date;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;

/** Base class for user-defined locations of interest shown on map. */
public abstract class LocationOfInterest<B extends LocationOfInterest.Builder> {
  public boolean isPoint() {
    return this instanceof PointOfInterest;
  }

  public boolean isGeoJson() {
    return this instanceof GeoJsonLocationOfInterest;
  }

  public boolean isPolygon() {
    return this instanceof PolygonOfInterest;
  }

  @NonNull
  public abstract String getId();

  public abstract Survey getSurvey();

  public abstract Job getJob();

  @Nullable
  public abstract String getCustomId();

  @Nullable
  public abstract String getCaption();

  /**
   * Returns the user and time audit info pertaining to the creation of this location of interest.
   */
  public abstract AuditInfo getCreated();

  /**
   * Returns the user and time audit info pertaining to the last modification of this location of
   * interest.
   */
  public abstract AuditInfo getLastModified();

  @OverridingMethodsMustInvokeSuper
  public LocationOfInterestMutation toMutation(Type type, String userId) {
    return LocationOfInterestMutation.builder()
        .setType(type)
        .setSyncStatus(SyncStatus.PENDING)
        .setSurveyId(getSurvey().getId())
        .setLocationOfInterestId(getId())
        .setJobId(getJob().getId())
        .setUserId(userId)
        .setClientTimestamp(new Date())
        .build();
  }

  public abstract static class Builder<T extends LocationOfInterest.Builder> {
    // TODO: Use newFoo or foo consistently.
    public abstract T setId(@NonNull String newId);

    public abstract T setSurvey(@NonNull Survey survey);

    public abstract T setJob(@NonNull Job newJob);

    public abstract T setCustomId(@Nullable String newCustomId);

    public abstract T setCaption(@Nullable String newCaption);

    public abstract T setCreated(@NonNull AuditInfo newCreated);

    public abstract T setLastModified(@NonNull AuditInfo newLastModified);
  }
}
