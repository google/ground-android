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

package com.google.android.gnd.repository;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import org.junit.Before;
import org.junit.Test;

public class InMemoryCacheTest {

  private InMemoryCache inMemoryCache;

  private static final User FAKE_USER =
      User.builder().setId("id").setDisplayName("name").setEmail("email").build();

  private static final Style FAKE_STYLE = Style.builder().setColor("#000000").build();

  private static final Layer FAKE_LAYER = Layer.newBuilder()
      .setId("id")
      .setName("name")
      .setDefaultStyle(FAKE_STYLE).build();

  private static final Feature FAKE_FEATURE =
      Feature.newBuilder()
          .setId("foo feature id")
          .setProject(Project.newBuilder()
              .setId("project id")
              .setTitle("project title")
              .setDescription("project description")
              .build())
          .setLayer(FAKE_LAYER)
          .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
          .setCreated(AuditInfo.now(FAKE_USER))
          .setLastModified(AuditInfo.now(FAKE_USER))
          .build();

  @Before
  public void setUp() {
    inMemoryCache = new InMemoryCache();
  }

  @Test
  public void getFeatures_emptyIfNoneAdded() {
    assertThat(inMemoryCache.getFeatures()).isEmpty();
  }

  @Test
  public void putFeature() {
    inMemoryCache.putFeature(FAKE_FEATURE);

    assertThat(inMemoryCache.getFeatures()).containsExactly(FAKE_FEATURE);
  }

  @Test
  public void removeFeature() {
    inMemoryCache.putFeature(FAKE_FEATURE);
    inMemoryCache.removeFeature(FAKE_FEATURE.getId());

    assertThat(inMemoryCache.getFeatures()).isEmpty();
  }

  @Test
  public void clear_clearsFeatures() {
    inMemoryCache.putFeature(FAKE_FEATURE);
    inMemoryCache.clear();

    assertThat(inMemoryCache.getFeatures()).isEmpty();
  }
}
