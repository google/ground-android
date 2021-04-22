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

package com.google.android.gnd.model;

import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.layer.Style;
import java.util.Date;

/**
 * Helper methods for building model objects for use in tests. Method return builders with required
 * fields set to placeholder values. Rather than depending on these values, tests that test for
 * specific values should explicitly set them in relevant test methods or during test setup.
 */
public class TestModelBuilders {
  public static Project.Builder newTestProject() {
    return Project.newBuilder().setId("").setTitle("").setDescription("");
  }

  public static User.Builder newTestUser() {
    return User.builder().setId("").setEmail("").setDisplayName("");
  }

  public static AuditInfo.Builder newTestAuditInfo() {
    return AuditInfo.builder().setClientTimestamp(new Date(0)).setUser(newTestUser().build());
  }

  public static Point.Builder newTestPoint() {
    return Point.newBuilder().setLatitude(0).setLongitude(0);
  }

  public static Feature.Builder newTestFeature() {
    return Feature.newBuilder()
        .setId("")
        .setProject(newTestProject().build())
        .setPoint(newTestPoint().build())
        .setCreated(newTestAuditInfo().build())
        .setLastModified(newTestAuditInfo().build());
  }

  public static Layer.Builder newTestLayer() {
    return Layer.newBuilder().setId("").setName("").setDefaultStyle(newTestStyle().build());
  }

  public static Style.Builder newTestStyle() {
    return Style.builder().setColor("");
  }

  public static Form.Builder newTestForm() {
    return Form.newBuilder().setId("");
  }
}
