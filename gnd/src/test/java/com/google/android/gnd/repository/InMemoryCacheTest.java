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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.layer.Layer;
import org.junit.Before;
import org.junit.Test;

public class InMemoryCacheTest {

  private InMemoryCache inMemoryCache;

  private static final Project FAKE_PROJECT =
      Project.newBuilder()
          .setId("foo project id")
          .setTitle("foo title")
          .setDescription("foo description")
          .build();;

  private Feature FAKE_FEATURE =
      Feature.newBuilder()
          .setId("foo feature id")
          .setProject(Project.newBuilder().build())
          .setLayer(Layer.newBuilder().build())
          .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
          .build();

  @Before
  public void setUp() {
    inMemoryCache = new InMemoryCache();
  }

  @Test
  public void getFeatures_EmptyIfNoneAdded() {
    assertEquals(0, inMemoryCache.getFeatures().size());
  }

  @Test
  public void putFeature() {
    inMemoryCache.putFeature(FAKE_FEATURE);
    assertEquals(1, inMemoryCache.getFeatures().size());
    assertEquals(FAKE_FEATURE, inMemoryCache.getFeatures().asList().get(0));
  }

  @Test
  public void removeFeature() {
    inMemoryCache.putFeature(FAKE_FEATURE);
    assertEquals(1, inMemoryCache.getFeatures().size());
    inMemoryCache.removeFeature(FAKE_FEATURE.getId());
    assertEquals(0, inMemoryCache.getFeatures().size());
  }

  @Test
  public void getActiveProject_NullIfUnset() {
    assertNull(inMemoryCache.getActiveProject());
  }

  @Test
  public void setActiveProject() {
    inMemoryCache.setActiveProject(FAKE_PROJECT);
    assertEquals(FAKE_PROJECT, inMemoryCache.getActiveProject());
  }

  @Test
  public void setActiveProject_ClearsFeatures() {
    inMemoryCache.putFeature(FAKE_FEATURE);
    inMemoryCache.setActiveProject(FAKE_PROJECT);
    assertEquals(0, inMemoryCache.getFeatures().size());
  }

  @Test
  public void clearActiveProject() {
    inMemoryCache.setActiveProject(FAKE_PROJECT);
    inMemoryCache.clearActiveProject();
    assertNull(inMemoryCache.getActiveProject());
  }

  @Test
  public void clearActiveProject_ClearsFeatures() {
    inMemoryCache.putFeature(FAKE_FEATURE);
    inMemoryCache.clearActiveProject();
    assertEquals(0, inMemoryCache.getFeatures().size());
  }
}
