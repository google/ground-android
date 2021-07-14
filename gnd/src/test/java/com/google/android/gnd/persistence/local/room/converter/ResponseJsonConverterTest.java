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

package com.google.android.gnd.persistence.local.room.converter;


import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.observation.DateResponse;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.rx.SchedulersModule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import java.util.Date;
import java8.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

public class ResponseJsonConverterTest {

  //Date represented in YYYY-MM-DDTHH:mmZ Format from 1632501600000L milliseconds.
  private static final String DATE_STRING = "2021-09-21T07:00+0000";
  //Date represented in milliseconds for date: 2021-09-24T16:40+0000.
  private static final Date DATE = new Date(1632207600000L);

  @Test
  public void testDateToIsoString() {
    String dateToIsoString = ResponseJsonConverter.dateToIsoString(DATE);
    assertThat(DATE_STRING).isEqualTo(dateToIsoString);
  }

  @Test
  public void testIsoStringToDate() {
    Date stringToDate = ResponseJsonConverter.isoStringToDate(DATE_STRING);
    assertThat(DATE).isEqualTo(stringToDate);
  }

  @Test
  public void testDateToIso_mismatchDate() {
    Date currentDate = new Date();
    Date stringToDate = ResponseJsonConverter.isoStringToDate(DATE_STRING);
    boolean checkMismatchDate = currentDate.equals(stringToDate);
    assertThat(checkMismatchDate).isEqualTo(false);
  }

  @Test
  public void testResponseToObject_dateResponse() {
    Object response = ResponseJsonConverter.toJsonObject(new DateResponse(DATE));
    assertThat(response).isInstanceOf(Object.class);
  }

  @Test
  public void testResponseToObject_timeResponse() {
    Object response = ResponseJsonConverter.toJsonObject(new TimeResponse(DATE));
    assertThat(response).isInstanceOf(Object.class);
  }

  @Test
  public void testObjectToResponse_dateResponse() {
    Object dateObject = ResponseJsonConverter.toJsonObject(new DateResponse(DATE));
    Optional<Response> response = ResponseJsonConverter.toResponse(Field
        .newBuilder()
        .setId("1")
        .setLabel("date")
        .setIndex(0)
        .setRequired(true)
        .setType(Type.DATE)
        .build(), dateObject);
    assertThat(((DateResponse) response.get()).getDate()).isEqualTo(DATE);
  }

  @Test
  public void testObjectToResponse_timeResponse() {
    Object timeObject = ResponseJsonConverter.toJsonObject(new TimeResponse(DATE));
    Optional<Response> response = ResponseJsonConverter.toResponse(Field
        .newBuilder()
        .setId("2")
        .setLabel("time")
        .setIndex(1)
        .setRequired(true)
        .setType(Type.TIME)
        .build(), timeObject);
    assertThat(((TimeResponse) response.get()).getTime()).isEqualTo(DATE);
  }
}
