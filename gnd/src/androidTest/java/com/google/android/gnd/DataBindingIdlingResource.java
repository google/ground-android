/*
 * Copyright 2020 Google LLC
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

import android.view.View;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingResource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Used to make Espresso work with DataBinding. Without it the tests will be flaky because
 * DataBinding uses Choreographer class to synchronize its view updates hence using this to monitor
 * a launched fragment in fragment scenario will make Espresso wait before doing additional checks
 */
public class DataBindingIdlingResource implements IdlingResource {
  // Give it a unique id to work around an Espresso bug where you cannot register/unregister
  // an idling resource with the same name.
  private static final String ID = UUID.randomUUID().toString();
  // List of registered callbacks
  private static final List<IdlingResource.ResourceCallback> IDLING_CALLBACKS = new ArrayList<>();
  // Holds whether isIdle was called and the result was false. We track this to avoid calling
  // onTransitionToIdle callbacks if Espresso never thought we were idle in the first place.
  private boolean wasNotIdle;
  private FragmentActivity activity;

  @Override
  public String getName() {
    return String.format("DataBinding %s", ID);
  }

  @Override
  public boolean isIdleNow() {
    boolean idle = false;
    for (ViewDataBinding b : getBindings()) {
      if (b == null) {
        continue;
      }
      if (!b.hasPendingBindings()) {
        idle = true;
        break;
      }
    }
    if (idle) {
      if (wasNotIdle) {
        // Notify observers to avoid Espresso race detector.
        for (ResourceCallback cb : IDLING_CALLBACKS) {
          cb.onTransitionToIdle();
        }
      }
      wasNotIdle = false;
    } else {
      wasNotIdle = true;
      // Check next frame.
      if (activity != null) {
        activity.findViewById(android.R.id.content).postDelayed(this::isIdleNow, 16);
      }
    }
    return idle;
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback callback) {
    IDLING_CALLBACKS.add(callback);
  }

  /** Sets the activity from an [ActivityScenario] to be used from [DataBindingIdlingResource]. */
  public <T extends FragmentActivity> void monitorActivity(ActivityScenario<T> activityScenario) {
    activityScenario.onActivity(this::monitorActivity);
  }

  public <T extends FragmentActivity> void monitorActivity(T activity) {
    this.activity = activity;
  }

  public <T extends Fragment> void monitorFragment(T fragment) {
    activity = fragment.requireActivity();
  }

  @Nullable
  private ViewDataBinding getBinding(View view) {
    return DataBindingUtil.getBinding(view);
  }

  private List<ViewDataBinding> getBindings() {
    List<Fragment> fragments =
        activity == null
            ? Collections.emptyList()
            : activity.getSupportFragmentManager().getFragments();

    List<ViewDataBinding> bindings = new ArrayList<>();
    for (Fragment f : fragments) {
      if (f.getView() == null) {
        continue;
      }
      bindings.add(getBinding(f.getView()));
      for (Fragment cf : f.getChildFragmentManager().getFragments()) {
        if (cf.getView() == null) {
          continue;
        }
        bindings.add(getBinding(cf.getView()));
        for (Fragment cf2 : cf.getChildFragmentManager().getFragments()) {
          if (cf2.getView() == null) {
            continue;
          }
          bindings.add(getBinding(cf2.getView()));
        }
      }
    }
    return bindings;
  }
}
