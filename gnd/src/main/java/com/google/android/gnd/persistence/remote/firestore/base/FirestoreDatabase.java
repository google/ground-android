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

package com.google.android.gnd.persistence.remote.firestore.base;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import com.google.android.gnd.rx.RxCompletable;
import com.google.android.gnd.system.NetworkManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.net.ConnectException;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;

/** Base class for representing Firestore databases as object hierarchies. */
public abstract class FirestoreDatabase {
  protected final FirebaseFirestore db;

  protected FirestoreDatabase(FirebaseFirestore db) {
    this.db = db;
  }

  // TOOD: Wrap in fluent version of WriteBatch.
  public WriteBatch batch() {
    return db.batch();
  }
}
