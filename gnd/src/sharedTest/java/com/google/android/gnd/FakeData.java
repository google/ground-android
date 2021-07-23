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

  public static final String PROJECT_ID_WITH_LAYER_AND_NO_FORM = "FAKE_PROJECT_ID";
  public static final String PROJECT_ID_WITH_NO_LAYERS = "FAKE_PROJECT_ID with no layers";
  public static final String PROJECT_TITLE = "Fake project title";
  public static final String PROJECT_DESCRIPTION = "Fake project description";
  public static final String TERMS_OF_SERVICE_ID = "TERMS_ID";
  public static final String TERMS_OF_SERVICE = "Fake Terms of Service";
  public static final String LAYER_NO_FORM_ID = "LAYER_NO_FORM_ID";
  public static final String LAYER_NO_FORM_NAME = "Fake name for layer with no form";
  public static final String LAYER_NO_FORM_COLOR = "#00ff00";

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
