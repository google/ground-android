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
import android.util.Log;
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.akaita.java.rxjava2debug.RxJava2Debug;
import com.facebook.stetho.Stetho;
import com.google.android.gnd.inject.GndWorkerFactory;
import com.google.android.gnd.repository.DataRepository;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import javax.inject.Inject;

// TODO: When implementing background data sync service, we'll need to inject a Service here; we
// should then extend DaggerApplication instead. If MultiDex is still needed, we can install it
// without extending MultiDexApplication.
public class GndApplication extends MultiDexApplication implements HasActivityInjector {
  private static final String TAG = GndApplication.class.getSimpleName();

  @Inject DataRepository dataRepository;
  @Inject DispatchingAndroidInjector<Activity> activityInjector;
  @Inject GndWorkerFactory workerFactory;

  @Override
  public void onCreate() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "DEBUG build config active; enabling debug tooling");
      setStrictMode();

      /*
       * Debug bridge for Android applications. Enables network and database debugging for the app
       * accessible under chrome://inspect in Chrome desktop browser.
       */
      Stetho.initializeWithDefaults(this);
    }

    super.onCreate();

    // Root of dependency injection.
    DaggerGndApplicationComponent.builder().create(this).inject(this);

    // Enable RxJava assembly stack collection for more useful stack traces.
    RxJava2Debug.enableRxJava2AssemblyTracking(new String[] {getClass().getPackage().getName()});

    // Set custom worker factory that allow Workers to use Dagger injection.
    // TODO(github.com/google/dagger/issues/1183): Remove once Workers support injection.
    WorkManager.initialize(
        this, new Configuration.Builder().setWorkerFactory(workerFactory).build());
  }

  @Override
  public AndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }

  private void setStrictMode() {
    StrictMode.setThreadPolicy(
        new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());

    StrictMode.setVmPolicy(
        new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().penaltyLog().build());
  }
}
