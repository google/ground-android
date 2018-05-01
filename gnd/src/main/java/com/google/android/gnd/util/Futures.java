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

import static java8.util.function.Functions.identity;
import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gms.tasks.Task;

import java.util.List;

import java8.util.concurrent.CompletableFuture;
import java8.util.function.Function;

public abstract class Futures {
  private Futures() {}

  public static <V> CompletableFuture<V> fromTask(Task<V> task) {
    return fromTask(task, identity());
  }

  public static <V, X> CompletableFuture<X> fromTask(Task<V> task, Function<V, X> mappingFunction) {
    CompletableFuture<X> future = new CompletableFuture();
    task.addOnSuccessListener(t -> future.complete(mappingFunction.apply(t)));
    task.addOnFailureListener(e -> future.completeExceptionally(e));
    return future;
  }

  public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futures) {
    CompletableFuture<Void> all =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
    return all.thenApply(v -> stream(futures).map(ft -> ft.join()).collect(toList()));
  }
}
