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
import com.google.android.gnd.GndApplication;
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
  public boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager =
        (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }
}
