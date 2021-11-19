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

package com.google.android.gnd.util;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java8.util.function.Function;
import java8.util.function.Predicate;
import java8.util.stream.Collector;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

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

  /**
   * Returns a function for use with RxJava Observables that maps a function over the contents of an
   * immutable list and recollects the results back into an immutable list.
   *
   * <p>This eliminates list handling boilerplate when lists are used as rx stream values.
   */
  public static <E, T>
      io.reactivex.functions.Function<Collection<T>, ImmutableList<E>> mapAndRecollect(
          Function<T, E> func) {
    return x -> StreamSupport.stream(x).map(func).collect(toImmutableList());
  }

  /**
   * Returns a function for use with RxJava Observables that filters a list using a provided
   * function and recollects the results back into an immutable list.
   *
   * <p>This eliminates list handling boilerplate when lists are used as rx stream values.
   */
  public static <T>
      io.reactivex.functions.Function<Collection<T>, ImmutableList<T>> filterAndRecollect(
          Predicate<T> func) {
    return x -> StreamSupport.stream(x).filter(func).collect(toImmutableList());
  }
}
