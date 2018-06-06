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

package com.google.android.gnd.repository;

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.repository.Record.Value;
import java8.util.stream.Collectors;

public class RecordSummary {
  private static final CharSequence CODE_SEPARATOR = ",";
  private Record record;
  private Form form;

  public RecordSummary(Form form, Record record) {
    this.form = form;
    this.record = record;
  }

  public Record getRecord() {
    return record;
  }

  public Form getForm() {
    return form;
  }

  public static String toSummaryText(Value value) {
    switch (value.getTypeCase()) {
      case TEXT:
        return value.getText();
      case NUMBER:
        // TODO: int vs float? Format correctly.
        return Float.toString(value.getNumber());
      case CHOICES:
        return stream(value.getChoices().getCodesList())
          .collect(Collectors.joining(CODE_SEPARATOR));
      case TYPE_NOT_SET:
      default:
        return "";
    }
  }
}
