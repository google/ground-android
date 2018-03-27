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

package com.google.gnd.util;

import java.util.List;
import java.util.Map;

import java8.util.function.Function;
import java8.util.stream.Collectors;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

public abstract class Streams {
  private Streams() {}

  public static <X, Y> List<Y> map(List<X> in, Function<X, Y> mappingFunction) {
    return stream(in).map(mappingFunction).collect(toList());
  }

  public static <T, K, V> Map<K, V> toMap(
      List<T> values, Function<T, K> keyFunction, Function<T, V> valueFunction) {
    return stream(values).collect(Collectors.toMap(keyFunction, valueFunction));
  }
}
