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

import com.google.android.gnd.model.observation.DateResponse;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.common.truth.Truth;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import org.junit.Test;

public class DateTimeResponseTest {

  // Sat, 23 Oct 2021 07:30:45 (Local Time)
  private static final Instant EXPECTED_INSTANT =
      LocalDate.of(2021, Month.OCTOBER, 23)
          .atTime(7, 30, 45)
          .atZone(ZoneId.systemDefault())
          .toInstant();

  private Date getExpectedDate() {
    return Date.from(EXPECTED_INSTANT);
  }

  @Test
  public void testTimeResponse_textDetails() {
    Truth.assertThat(new TimeResponse(getExpectedDate()).getDetailsText()).isEqualTo("07:30");
  }

  @Test
  public void testDateResponse_textDetails() {
    Truth.assertThat(new DateResponse(getExpectedDate()).getDetailsText()).isEqualTo("2021-10-23");
  }
}
