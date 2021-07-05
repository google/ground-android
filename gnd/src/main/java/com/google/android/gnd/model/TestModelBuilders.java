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

import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PointFeature;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Field.Type;
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
  public static Project.Builder newProject() {
    return Project.newBuilder().setId("").setTitle("").setDescription("");
  }

  public static User.Builder newUser() {
    return User.builder().setId("").setEmail("").setDisplayName("");
  }

  public static AuditInfo.Builder newAuditInfo() {
    return AuditInfo.builder().setClientTimestamp(new Date(0)).setUser(newUser().build());
  }

  public static Point.Builder newPoint() {
    return Point.newBuilder().setLatitude(0).setLongitude(0);
  }

  public static PointFeature.Builder newPointFeature() {
    return PointFeature.newBuilder()
        .setId("")
        .setProject(newProject().build())
        .setPoint(newPoint().build())
        .setCreated(newAuditInfo().build())
        .setLastModified(newAuditInfo().build());
  }

  public static TermsOfService.Builder newTermsOfService() {
    return TermsOfService.builder()
        .setId("")
        .setText("");
  }

  public static Layer.Builder newLayer() {
    return Layer.newBuilder().setId("").setName("").setDefaultStyle(newStyle().build());
  }

  public static Style.Builder newStyle() {
    return Style.builder().setColor("");
  }

  public static Form.Builder newForm() {
    return Form.newBuilder().setId("");
  }

  public static Field.Builder newField() {
    return Field.newBuilder()
        .setId("")
        .setIndex(0)
        .setType(Type.TEXT_FIELD)
        .setLabel("")
        .setRequired(false);
  }
}
