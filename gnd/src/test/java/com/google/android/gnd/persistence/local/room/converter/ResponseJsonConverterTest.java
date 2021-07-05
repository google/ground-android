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


import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
import com.google.android.gnd.model.observation.DateResponse;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.TimeResponse;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.common.truth.Truth;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;
import dagger.hilt.android.testing.UninstallModules;
import java.util.Date;
import java8.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@HiltAndroidTest
@UninstallModules({SchedulersModule.class, LocalDatabaseModule.class})
@Config(application = HiltTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class ResponseJsonConverterTest {

  public String dateString = "2021-09-24T22:10+0530";
  public Date date = new Date(1632501600000L);

  @Test
  public void testIsoStringToDate() {
    String dateToIsoString = ResponseJsonConverter.dateToIsoString(new Date(1632501600000L));
    Truth.assertThat(dateString).isEqualTo(dateToIsoString);
  }

  @Test
  public void testDateToIso() {
    Date stringToDate = ResponseJsonConverter.isoStringToDate(dateString);
    Truth.assertThat(date).isEqualTo(stringToDate);
  }

  @Test
  public void testDateToIso_mismatchDate() {
    Date currentDate = new Date();
    Date stringToDate = ResponseJsonConverter.isoStringToDate(dateString);
    boolean checkMismatchDate = currentDate.equals(stringToDate);
    Truth.assertThat(checkMismatchDate).isEqualTo(false);
  }

  @Test
  public void responseToObject_dateResponse() {
    Object response = ResponseJsonConverter.toJsonObject(new DateResponse(date));
    Truth.assertThat(response).isInstanceOf(Object.class);
  }

  @Test
  public void responseToObject_timeResponse() {
    Object response = ResponseJsonConverter.toJsonObject(new TimeResponse(date));
    Truth.assertThat(response).isInstanceOf(Object.class);
  }

  @Test
  public void objectToResponse_dateResponse() {
    Object dateObject = ResponseJsonConverter.toJsonObject(new DateResponse(date));
    Optional<Response> response = ResponseJsonConverter.toResponse(Field
        .newBuilder()
        .setId("1")
        .setLabel("date")
        .setIndex(0)
        .setRequired(true)
        .setType(Type.DATE)
        .build(), dateObject);
    Truth.assertThat(response.get()).isInstanceOf(DateResponse.class);
  }

  @Test
  public void objectToResponse_timeResponse() {
    Object timeObject = ResponseJsonConverter.toJsonObject(new TimeResponse(date));
    Optional<Response> response = ResponseJsonConverter.toResponse(Field
        .newBuilder()
        .setId("2")
        .setLabel("time")
        .setIndex(1)
        .setRequired(true)
        .setType(Type.TIME)
        .build(), timeObject);
    Truth.assertThat(response.get()).isInstanceOf(TimeResponse.class);
  }
}
