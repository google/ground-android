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

package com.google.android.ground.util;

import com.google.common.collect.ImmutableList;
import java8.util.stream.Collector;
import java8.util.stream.Collectors;

/**
 * Custom collector for compatibility between {@link Collector} compat class and Guava {@link
 * ImmutableList}.
 */
public abstract class ImmutableListCollector {
  private static final Collector<Object, ?, ImmutableList<Object>> TO_IMMUTABLE_LIST =
      Collectors.of(
          ImmutableList::builder,
          ImmutableList.Builder::add,
          (a, b) -> {
            throw new UnsupportedOperationException();
          },
          ImmutableList.Builder::build);

  /** Do not instantiate. */
  private ImmutableListCollector() {}

  /**
   * Returns a {@link Collector} that accumulates the input elements into a new ImmutableList, in
   * encounter order.
   */
  public static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() {
    return (Collector) TO_IMMUTABLE_LIST;
  }
}
