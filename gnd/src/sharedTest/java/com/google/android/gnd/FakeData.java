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

package com.google.android.gnd;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.common.collect.ImmutableList;
import java8.util.Optional;

public class FakeData {

  public static final User TEST_USER =
      User.builder().setId("user_id").setEmail("user@gmail.com").setDisplayName("User").build();

  public static final Layer TEST_LAYER =
      Layer.newBuilder()
          .setId("layer id")
          .setName("heading title")
          .setDefaultStyle(Style.builder().setColor("000").build())
          .setForm(Optional.empty())
          .build();

  public static final Project TEST_PROJECT =
      Project.newBuilder()
          .setId("project id")
          .setTitle("project 1")
          .setDescription("foo description")
          .putLayer("layer id", TEST_LAYER)
          .build();

  public static final PointFeature TEST_POINT_FEATURE =
      PointFeature.newBuilder()
          .setId("feature id")
          .setProject(TEST_PROJECT)
          .setLayer(TEST_LAYER)
          .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
          .setCreated(AuditInfo.now(TEST_USER))
          .setLastModified(AuditInfo.now(TEST_USER))
          .build();

  public static final PolygonFeature TEST_POLYGON_FEATURE =
      PolygonFeature.builder()
          .setId("feature id")
          .setProject(TEST_PROJECT)
          .setLayer(TEST_LAYER)
          .setVertices(
              ImmutableList.of(
                  Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build(),
                  Point.newBuilder().setLatitude(10.0).setLongitude(10.0).build(),
                  Point.newBuilder().setLatitude(20.0).setLongitude(20.0).build()))
          .setCreated(AuditInfo.now(TEST_USER))
          .setLastModified(AuditInfo.now(TEST_USER))
          .build();
}
