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

package com.google.android.gnd;

import android.app.Activity;
import android.os.StrictMode;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import com.akaita.java.rxjava2debug.RxJava2Debug;
import com.google.android.gnd.repository.GndDataRepository;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import javax.inject.Inject;

// TODO: When implementing background data sync service, we'll need to inject a Service here; we
// should then extend DaggerApplication instead. If MultiDex is still needed, we can install it
// without extending MultiDexApplication.
public class GndApplication extends MultiDexApplication implements HasActivityInjector {
  private static final String TAG = GndApplication.class.getSimpleName();

  @Inject GndDataRepository dataRepository;
  @Inject DispatchingAndroidInjector<Activity> activityInjector;

  @Override
  public void onCreate() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "DEBUG build config active; enabling debug tooling");
      setStrictMode();
    }

    super.onCreate();

    // Root of dependency injection.
    DaggerGndApplicationComponent.builder().create(this).inject(this);

    // Enable RxJava assembly stack collection for more useful stack traces.
    RxJava2Debug.enableRxJava2AssemblyTracking(new String[] {getClass().getPackage().getName()});
  }

  @Override
  public AndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }

  private void setStrictMode() {
    StrictMode.setThreadPolicy(
      new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

    StrictMode.setVmPolicy(
      new StrictMode.VmPolicy.Builder()
        .detectLeakedSqlLiteObjects()
        .penaltyLog()
        .penaltyDeath()
        .build());
  }
}
