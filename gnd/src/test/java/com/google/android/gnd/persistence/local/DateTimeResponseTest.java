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

import static com.google.android.gnd.model.observation.DateResponse.DATE_FORMAT;
import static com.google.android.gnd.model.observation.TimeResponse.TIME_FORMAT;

import com.google.android.gnd.rx.SchedulersModule;
import com.google.common.truth.Truth;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

public class DateTimeResponseTest {

  @Test
  public void testTimeFormat() {
    Date originalDate = new Date();
    String timeString = TIME_FORMAT.format(originalDate);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
    LocalTime lt = LocalTime.parse(timeString, formatter);
    String formattedTime = lt.format(formatter);
    Truth.assertThat(timeString).isEqualTo(formattedTime);
  }

  @Test
  public void testDateFormat() {
    Date originalDate = new Date();
    String dateString = DATE_FORMAT.format(originalDate);
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());
    LocalDate ld = LocalDate.parse(dateString, formatter);
    String formattedDate = ld.format(formatter);
    Truth.assertThat(dateString).isEqualTo(formattedDate);
  }
}
