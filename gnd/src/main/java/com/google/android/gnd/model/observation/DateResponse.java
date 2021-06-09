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

package com.google.android.gnd.model.observation;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.form.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java8.util.Optional;

/** A user provided response to a date {@link Field}. */
public class DateResponse implements Response {

  private Date epochTime;

  public DateResponse(Date time) {
    this.epochTime = time;
  }

  public Date getDate() {
    return epochTime;
  }

  @Override
  public String getSummaryText(Field field) {
    return getDetailsText(field);
  }

  @Override
  public String getDetailsText(Field field) {
    return convertToDateFormat(epochTime);
  }

  public static String convertToDateFormat(Date date) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    return format.format(date);
  }

  @Override
  public boolean isEmpty() {
    return epochTime.getTime() == 0L;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DateResponse) {
      return epochTime == ((DateResponse) obj).epochTime;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return String.valueOf(epochTime).hashCode();
  }

  @NonNull
  @Override
  public String toString() {
    return String.valueOf(epochTime);
  }


  public static Optional<Response> fromDate(Date date) {
    return date.getTime() == 0L ? Optional.empty() : Optional.of(new DateResponse(date));
  }
}
