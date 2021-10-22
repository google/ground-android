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

package com.google.android.gnd.system;

import android.content.res.Resources;
import androidx.annotation.StringRes;
import com.google.android.gnd.R;
import com.google.firebase.firestore.FirebaseFirestoreException;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.FlowableProcessor;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Collects exceptions from lower-level components (like LocalDataStore, RemoteDataStore, etc.) and
 * converts them into user-visible strings.
 */
@Singleton
public class ApplicationErrorManager {

  private final FlowableProcessor<String> exceptionProcessor = BehaviorProcessor.create();
  private final Resources resources;

  @Inject
  ApplicationErrorManager(Resources resources) {
    this.resources = resources;
  }

  private void publishMessage(@StringRes int msgId) {
    String message = resources.getString(msgId);
    Timber.e("Error: %s", message);
    exceptionProcessor.onNext(message);
  }

  /**
   * Checks whether the exception should be handled globally. If yes, then converts it into an
   * user-visible string and notifies {@link #exceptionProcessor}. Callers shouldn't handle the
   * consumed exceptions locally.
   *
   * @param throwable Exception to be handled.
   * @return true if the exception is consumed otherwise false.
   */
  public boolean handleException(Throwable throwable) {
    int msgId = -1;

    if (throwable instanceof FirebaseFirestoreException) {
      switch (((FirebaseFirestoreException) throwable).getCode()) {
        case PERMISSION_DENIED:
          msgId = R.string.permission_denied_error;
          break;
        case UNAVAILABLE:
          msgId = R.string.config_load_error;
          break;
        default:
          msgId = -1;
          break;
      }
    }

    if (msgId != -1) {
      publishMessage(msgId);
    }

    return msgId != -1;
  }

  public Flowable<String> getExceptions() {
    return exceptionProcessor;
  }
}
