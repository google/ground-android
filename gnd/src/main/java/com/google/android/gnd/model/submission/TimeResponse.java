/*
 * Copyright 2021 Google LLC
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

import androidx.annotation.NonNull;
import com.google.android.gnd.model.task.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java8.util.Optional;

/**
 * A user-provided time {@link Field} response.
 */
public class TimeResponse implements Response {

  private static final long serialVersionUID = 1L;

  // TODO(#752): Use device localization preferences.
  private final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
  private final Date time;

  public TimeResponse(Date time) {
    this.time = time;
  }

  public Date getTime() {
    return time;
  }

  @Override
  public String getSummaryText() {
    return getDetailsText();
  }

  @Override
  public String getDetailsText() {
    synchronized (timeFormat) {
      return timeFormat.format(time);
    }
  }

  @Override
  public boolean isEmpty() {
    return time.getTime() == 0L;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TimeResponse) {
      return time == ((TimeResponse) obj).time;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return String.valueOf(time).hashCode();
  }

  @NonNull
  @Override
  public String toString() {
    return String.valueOf(time);
  }

  public static Optional<Response> fromDate(Date time) {
    return time.getTime() == 0L ? Optional.empty() : Optional.of(new TimeResponse(time));
  }
}
