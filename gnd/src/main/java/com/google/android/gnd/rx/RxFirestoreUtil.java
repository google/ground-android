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

package com.google.android.gnd.rx;

import static java8.util.stream.StreamSupport.stream;

import android.support.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;

public abstract class RxFirestoreUtil {
  /** Container for static helper methods. Do not instantiate. */
  private RxFirestoreUtil() {}

  /**
   * Applies the provided mapping function to each document in the specified query snapshot, if
   * present. If no results are present, completes with an empty list.
   */
  public static <T> Single<List<T>> mapToSingle(
      Maybe<QuerySnapshot> result, Function<DocumentSnapshot, T> mappingFunction) {
    return result
        .map(
            querySnapshot ->
                documentStream(querySnapshot).map(mappingFunction).collect(Collectors.toList()))
        .toSingle(Collections.emptyList());
  }

  @NonNull
  public static Stream<DocumentSnapshot> documentStream(QuerySnapshot querySnapshot) {
    return stream(querySnapshot.getDocuments());
  }
}
