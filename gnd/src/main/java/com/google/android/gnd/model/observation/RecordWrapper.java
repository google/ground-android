/*
 * Copyright 2019 Google LLC
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

import static com.google.android.gnd.system.AuthenticationManager.User.ANONYMOUS;

import androidx.annotation.NonNull;
import com.google.android.gnd.system.AuthenticationManager;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class RecordWrapper {

  private static final String DATE_FORMAT = "MMM dd, yyyy";
  private static final String TIME_FORMAT = "hh:mm:ss aaa";

  private final SimpleDateFormat dateFormat;
  private final SimpleDateFormat timeFormat;

  @NonNull public Record record;

  public RecordWrapper(@NonNull Record record) {
    this.record = record;

    dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
    timeFormat = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
  }

  @NonNull
  public Record getRecord() {
    return record;
  }

  public AuthenticationManager.User getModifiedBy() {
    return record.getModifiedBy() == null ? ANONYMOUS : record.getModifiedBy();
  }

  public String getLastModifiedDate() {
    return record.getServerTimestamps() == null
        ? ""
        : dateFormat.format(record.getServerTimestamps().getModified());
  }

  public String getLastModifiedTime() {
    return record.getServerTimestamps() == null
        ? ""
        : timeFormat.format(record.getServerTimestamps().getModified());
  }
}
