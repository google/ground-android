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

package com.google.android.gnd.rx;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import java8.util.Optional;
import java8.util.function.Predicate;

/** Custom RxJava operators. Apply to stream using the <code>compose()</code> method. */
public class RxTransformers {

  private RxTransformers() {}

  /**
   * Applies the provided mapper iff the predicate evaluates to true. When the predicate is true,
   * upstream items are only emitted once the Completable provided by the mapper completes. When
   * false, this operator has no effect and values are emitted immediately.
   */
  public static <T> ObservableTransformer<T, T> switchMapIf(
      Predicate<T> predicate, Function<T, Completable> mapper) {
    return upstream ->
        upstream.switchMap(
            t -> {
              if (predicate.test(t)) {
                return mapper.apply(t).andThen(Observable.just(t));
              } else {
                return Observable.just(t);
              }
            });
  }

  /**
   * Applies the provided extractor to retrieve an Optional from items emitted in the stream, and
   * applies the mapper iff the Optional value is present. When the Optional is present, upstream
   * items are only emitted once the Completable provided by the mapper completes. When the returned
   * Optional is empty, this operator has no effect and values are emitted immediately.
   */
  public static <T, R> ObservableTransformer<T, T> switchMapIfPresent(
      java8.util.function.Function<T, Optional<R>> optionalExtractor,
      Function<R, Completable> mapper) {
    return switchMapIf(
        t -> optionalExtractor.apply(t).isPresent(),
        t -> mapper.apply(optionalExtractor.apply(t).get()));
  }
}
