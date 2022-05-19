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

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.model.submission.DateResponse;
import com.google.android.gnd.model.submission.TimeResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import org.junit.Test;

public class DateTimeResponseTest {

  @Test
  public void testTimeResponse_textDetails() {
    Instant instant = LocalDate.now().atTime(7, 30, 45).atZone(ZoneId.systemDefault()).toInstant();
    assertThat(new TimeResponse(Date.from(instant)).getDetailsText()).isEqualTo("07:30");
  }

  @Test
  public void testDateResponse_textDetails() {
    Instant instant =
        LocalDate.of(2021, Month.OCTOBER, 23)
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant();
    assertThat(new DateResponse(Date.from(instant)).getDetailsText()).isEqualTo("2021-10-23");
  }
}
