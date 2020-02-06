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

import androidx.annotation.NonNull;
import com.google.android.gnd.model.form.Field;
import java8.util.Optional;

/** A user provided response to a text {@link Field}. */
public class TextResponse implements Response {

  private String text;

  public TextResponse(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  @Override
  public String getSummaryText(Field field) {
    return text;
  }

  @Override
  public String getDetailsText(Field field) {
    return text;
  }

  @Override
  public boolean isEmpty() {
    return text.trim().isEmpty();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TextResponse) {
      return text.equals(((TextResponse) obj).text);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return text.hashCode();
  }

  @NonNull
  @Override
  public String toString() {
    return text;
  }

  public static Optional<Response> fromString(String text) {
    return text.isEmpty() ? Optional.empty() : Optional.of(new TextResponse(text));
  }
}
