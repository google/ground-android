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

package com.google.android.gnd.vo;

import java8.util.Optional;

/** A user-provided response to a single {@link Field}. */
public interface Response {

  String getSummaryText(Field field);

  String getDetailsText(Field field);

  static String toString(Optional<Response> response) {
    return response.map(Response::toString).orElse("");
  }

  boolean isEmpty();
}
