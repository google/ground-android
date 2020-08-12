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

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.persistence.local.room.LocalDatabase;
import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.android.gnd.persistence.remote.FakeRemoteStorageManager;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteStorageManager;
import com.google.android.gnd.persistence.remote.RemoteStorageModule;
import com.google.android.gnd.persistence.uuid.FakeUuidGenerator;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.system.auth.AuthenticationModule;
import com.google.android.gnd.system.auth.FakeAuthenticationManager;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ApplicationComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

@UninstallModules({AuthenticationModule.class, RemoteStorageModule.class,
  LocalDatabaseModule.class})
@HiltAndroidTest
public class AddFeatureTest {

  // Ensures that the Hilt component is initialized before running the ActivityScenarioRule.
  public @Rule(order = 0)
  HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  // Sets the preferences so no login is required and an active project is selected.
  public @Rule(order = 1)
  SetPreferencesRule preferencesRule = new SetPreferencesRule();

  public @Rule(order = 2)
  ActivityScenarioRule scenarioRule =
    new ActivityScenarioRule(MainActivity.class);

  @Module
  @InstallIn(ApplicationComponent.class)
  abstract class TestDependenciesModule {

    @Binds
    @Singleton
    abstract AuthenticationManager bindAuthenticationManager(
      FakeAuthenticationManager authenticationManager);

    @Binds
    @Singleton
    abstract RemoteDataStore bindRemoteDataStore(FakeRemoteDataStore remoteDataStore);

    @Binds
    @Singleton
    abstract RemoteStorageManager bindRemoteStorageManager(
      FakeRemoteStorageManager remoteStorageManager);

    @Binds
    @Singleton
    abstract OfflineUuidGenerator offlineUuidGenerator(FakeUuidGenerator uuidGenerator);
  }

  @InstallIn(ApplicationComponent.class)
  @Module
  public class LocalDatabaseModule {

    @Provides
    @Singleton
    LocalDatabase localDatabase(@ApplicationContext Context context) {
      return Room.inMemoryDatabaseBuilder(context, LocalDatabase.class).build();
    }
  }

  // Given: a logged in user - with an active project with no map markers
  // When: they tap on the centre of the map
  // Then: nothing happens (the feature fragment is not displayed)
  @Test
  public void tappingCrosshairOnEmptyMapDoesNothing() throws InterruptedException {
    ActivityScenario<MainActivity> scenario = scenarioRule.getScenario();
    onView(withId(R.id.map_crosshairs)).perform(click());
    //Thread.sleep(2000);
    onView(withId(R.id.feature_title)).check(matches(withText("")));
  }

  // Given: a logged in user - with an active project which doesn't direct the user to add an
  // observation when adding a feature.
  // When: they tap the "Add feature" FAB - choose a feature type
  // Then: the observation marker is displayed on the map screen
  @Test
  public void addFeatureWithNoFields() throws InterruptedException {

    ActivityScenario<MainActivity> scenario = scenarioRule.getScenario();
    onView(withId(R.id.add_feature_btn)).perform(click());

    // With the feature type list click on the first one
    onData(allOf(is(instanceOf(String.class)), is("Layer no fields"))).perform(click());

    //Thread.sleep(2000);

    onView(withId(R.id.map_crosshairs)).perform(click());

    // TODO: figure out how to remove this
    //  it's probably because the feature fragment animation does not block the test and the
    //  feature title isn't updated until the animation has completed (and it isn't disabled by the
    //  normal `transition_animation_scale 0` etc commands).
    Thread.sleep(100);

    onView(withId(R.id.feature_title)).check(matches(withText("Layer no fields")));
  }
}
