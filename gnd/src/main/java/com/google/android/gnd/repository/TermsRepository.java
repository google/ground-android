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

import com.google.android.gnd.model.Terms;
import com.google.android.gnd.persistence.local.LocalDataStore;
import com.google.android.gnd.persistence.local.LocalValueStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Cold;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Coordinates persistence and retrieval of {@link Terms} instances from remote, local, and in
 * memory data stores. For more details on this pattern and overall architecture, see
 * https://developer.android.com/jetpack/docs/guide.
 */
@Singleton
public class TermsRepository {

  private static final long LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS = 30;

  private final RemoteDataStore remoteDataStore;

  @Inject
  public TermsRepository(
      RemoteDataStore remoteDataStore) {
    this.remoteDataStore = remoteDataStore;

  }

  @Cold
  public Flowable<Loadable<Terms>> getProjectTerms() {
    return loadTermsFromRemote()
        .doOnSubscribe(__ -> Timber.d("Loading terms from remote"))
        .doOnError(err -> Timber.e(err, "Failed to load terms from remote"))
        .toFlowable()
        .compose(Loadable::loadingOnceAndWrap);
  }

  @Cold
  private Single<Terms> loadTermsFromRemote() {
    return remoteDataStore
        .loadTerms()
        .timeout(LOAD_REMOTE_PROJECT_SUMMARIES_TIMEOUT_SECS, TimeUnit.SECONDS);
  }
}
