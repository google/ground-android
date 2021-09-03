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

package com.google.android.gnd.persistence.local;

import com.google.common.truth.Truth;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import org.junit.Test;

public class DateTimeResponseTest {

  public final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
  public final DateFormat dateFormat =
      new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

  @Test
  public void testTimeFormat() {
    Date originalDate = new Date();
    String timeString = timeFormat.format(originalDate);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
    LocalTime lt = LocalTime.parse(timeString, formatter);
    String formattedTime = lt.format(formatter);
    Truth.assertThat(timeString).isEqualTo(formattedTime);
  }

  @Test
  public void testDateFormat() {
    Date originalDate = new Date();
    String dateString = dateFormat.format(originalDate);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    LocalDate ld = LocalDate.parse(dateString, formatter);
    String formattedDate = ld.format(formatter);
    Truth.assertThat(dateString).isEqualTo(formattedDate);
  }
}
