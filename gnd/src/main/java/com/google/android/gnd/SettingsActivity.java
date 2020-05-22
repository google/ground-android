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

import static com.google.android.gnd.util.Debug.logLifecycleEvent;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.google.android.gnd.databinding.SettingsActivityBinding;
import com.google.android.gnd.ui.settings.SettingsFragment;
import dagger.android.support.DaggerAppCompatActivity;
import javax.inject.Singleton;

@Singleton
public class SettingsActivity extends DaggerAppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    logLifecycleEvent(this);
    super.onCreate(savedInstanceState);

    SettingsActivityBinding binding = SettingsActivityBinding.inflate(getLayoutInflater());
    binding.setLifecycleOwner(this);
    setContentView(binding.getRoot());

    setSupportActionBar(binding.settingsToolbar);

    binding.settingsToolbar.setNavigationOnClickListener(v -> finish());

    getSupportActionBar().setDisplayShowTitleEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);

    ViewCompat.setOnApplyWindowInsetsListener(
        getWindow().getDecorView().getRootView(),
        (v, insets) -> {
          binding.settingsToolbar.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
          return insets;
        });

    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.settings_container, new SettingsFragment())
        .commit();
  }
}
