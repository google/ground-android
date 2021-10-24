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

package com.google.android.gnd.repository;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gnd.HiltTestWithRobolectricRunner;
import dagger.hilt.android.testing.HiltAndroidTest;
import javax.inject.Inject;
import org.junit.Test;

@HiltAndroidTest
public class MapsRepositoryTest extends HiltTestWithRobolectricRunner {

  @Inject MapsRepository mapsRepository;

  @Test
  public void testGetMapType_defaultValue() {
    assertThat(mapsRepository.getSavedMapType()).isEqualTo(GoogleMap.MAP_TYPE_NORMAL);
  }

  @Test
  public void testGetMapType() {
    mapsRepository.saveMapType(GoogleMap.MAP_TYPE_HYBRID);
    assertThat(mapsRepository.getSavedMapType()).isEqualTo(GoogleMap.MAP_TYPE_HYBRID);
  }
}
