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
import com.google.android.gnd.model.TermsOfService;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class FakeData {
  public static final TermsOfService TERMS_OF_SERVICE =
      TermsOfService.builder()
          .setId("TERMS_OF_SERVICE")
          .setText("Fake Terms of Service text")
          .build();

  public static final Layer LAYER_WITH_NO_FORM =
      Layer.newBuilder()
          .setId("LAYER_WITH_NO_FORM")
          .setName("Layer with no form")
          .setDefaultStyle(Style.builder().setColor("#00ff00").build())
          .setContributorsCanAdd(ImmutableList.of(FeatureType.POINT))
          .build();

  public static final User USER =
      User.builder().setId("user_id").setEmail("user@gmail.com").setDisplayName("User").build();

  public static final User USER_2 =
      User.builder().setId("user_id_2").setEmail("user2@gmail.com").setDisplayName("User2").build();

  public static final Project PROJECT_WITH_LAYER_AND_NO_FORM =
      Project.newBuilder()
          .setId("PROJECT_WITH_LAYER_AND_NO_FORM")
          .setTitle("Layers and forms")
          .setDescription("Project with layer and no form")
          .putLayer(LAYER_WITH_NO_FORM.getId(), LAYER_WITH_NO_FORM)
          .setAcl(ImmutableMap.of(FakeData.USER.getEmail(), "contributor"))
          .build();

  public static final PointFeature POINT_FEATURE =
      PointFeature.newBuilder()
          .setId("feature id")
          .setProject(PROJECT_WITH_LAYER_AND_NO_FORM)
          .setLayer(LAYER_WITH_NO_FORM)
          .setPoint(Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build())
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .build();

  public static final ImmutableList<Point> VERTICES =
      ImmutableList.of(
          Point.newBuilder().setLatitude(0.0).setLongitude(0.0).build(),
          Point.newBuilder().setLatitude(10.0).setLongitude(10.0).build(),
          Point.newBuilder().setLatitude(20.0).setLongitude(20.0).build());

  public static final PolygonFeature POLYGON_FEATURE =
      PolygonFeature.builder()
          .setId("feature id")
          .setProject(PROJECT_WITH_LAYER_AND_NO_FORM)
          .setLayer(LAYER_WITH_NO_FORM)
          .setVertices(VERTICES)
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .build();

  public static final GeoJsonFeature GEO_JSON_FEATURE =
      GeoJsonFeature.newBuilder()
          .setId("feature id")
          .setProject(PROJECT_WITH_LAYER_AND_NO_FORM)
          .setLayer(LAYER_WITH_NO_FORM)
          .setGeoJsonString("some data string")
          .setCreated(AuditInfo.now(USER))
          .setLastModified(AuditInfo.now(USER))
          .build();
}
