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

package com.google.android.gnd.system;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.RequiresPermission;
import com.google.android.gnd.GndApplication;
import com.google.android.gnd.rx.RxCompletable;
import io.reactivex.Completable;
import java.net.ConnectException;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Abstracts access to network state. */
@Singleton
public class NetworkManager {
  private final GndApplication application;

  @Inject
  public NetworkManager(GndApplication application) {
    this.application = application;
  }

  /** Returns true iff the device has internet connectivity, false otherwise. */
  @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
  private static boolean isNetworkAvailable(Context context) {
    ConnectivityManager cm =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnected();
  }

  /**
   * Returns a Completable that completes immediately on subscribe if network is available, or fails
   * in error if not.
   */
  public static Completable requireActiveNetwork(Context context) {
    return RxCompletable.completeOrError(() -> isNetworkAvailable(context), ConnectException.class);
  }

  public boolean isNetworkAvailable() {
    return isNetworkAvailable(application);
  }
}
