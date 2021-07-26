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

package com.google.android.gnd.persistence.remote.firestore.schema;

import static com.google.android.gnd.model.TestModelBuilders.newAuditInfo;
import static com.google.android.gnd.model.TestModelBuilders.newField;
import static com.google.android.gnd.model.TestModelBuilders.newForm;
import static com.google.android.gnd.model.TestModelBuilders.newGeoPointPolygonVertices;
import static com.google.android.gnd.model.TestModelBuilders.newLayer;
import static com.google.android.gnd.model.TestModelBuilders.newPolygonFeature;
import static com.google.android.gnd.model.TestModelBuilders.newPolygonVertices;
import static com.google.android.gnd.model.TestModelBuilders.newProject;
import static com.google.android.gnd.model.TestModelBuilders.newUser;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java8.util.J8Arrays.stream;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.remote.DataStoreException;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java8.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FeatureConverterTest {

  @Mock private DocumentSnapshot featureDocumentSnapshot;

  private static final AuditInfo AUDIT_INFO_1 =
      newAuditInfo()
          .setUser(newUser().setId("user1").build())
          .setClientTimestamp(new Date(100))
          .setServerTimestamp(Optional.of(new Date(101)))
          .build();

  private static final AuditInfo AUDIT_INFO_2 =
      newAuditInfo()
          .setUser(newUser().setId("user2").build())
          .setClientTimestamp(new Date(200))
          .setServerTimestamp(Optional.of(new Date(201)))
          .build();

  private static final AuditInfoNestedObject AUDIT_INFO_1_NESTED_OBJECT =
      new AuditInfoNestedObject(
          new UserNestedObject("user1", null, null),
          new Timestamp(new Date(100)),
          new Timestamp(new Date(101)));

  private static final AuditInfoNestedObject AUDIT_INFO_2_NESTED_OBJECT =
      new AuditInfoNestedObject(
          new UserNestedObject("user2", null, null),
          new Timestamp(new Date(200)),
          new Timestamp(new Date(201)));

  private Layer layer;
  private Project project;
  private Feature feature;
  private Map<String, Object> geometry;
  private Map<String, Object> noVerticesGeometry;

  private void setUpTestProject(String layerId, String formId, Field... fields) {
    Form form =
        newForm()
            .setId(formId)
            .setElements(stream(fields).map(Element::ofField).collect(toImmutableList()))
            .build();
    layer = newLayer().setId(layerId).setForm(form).build();
    project = newProject().putLayer(layerId, layer).build();
  }

  @Test
  public void testToFeature() {
    setUpTestGeometry();
    setUpTestProject(
        "layer001",
        "form001",
        newField().setId("field1").setType(Field.Type.TEXT_FIELD).build(),
        newField()
            .setId("field2")
            .setType(Field.Type.MULTIPLE_CHOICE)
            .setMultipleChoice(
                MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build())
            .build(),
        newField().setId("field3").setType(Field.Type.MULTIPLE_CHOICE).build(),
        newField().setId("field4").setType(Field.Type.PHOTO).build());
    setUpTestFeature("feature123");
    mockFeatureDocumentSnapshot(
        "feature123",
        new FeatureDocument(
            /* layerId */
            "layer001",
            /* customId */
            null,
            /* caption */
            null,
            /* location */
            null,
            /* geoJson */
            null,
            /* geometry */
            geometry,
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT));

    assertThat(toFeature()).isEqualTo(feature);
  }

  @Test
  public void testToFeature_nullFeature() {
    setUpTestGeometry();
    setUpTestProject(
        "layer001",
        "form001",
        newField().setId("field1").setType(Field.Type.TEXT_FIELD).build(),
        newField()
            .setId("field2")
            .setType(Field.Type.MULTIPLE_CHOICE)
            .setMultipleChoice(
                MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build())
            .build(),
        newField().setId("field3").setType(Field.Type.MULTIPLE_CHOICE).build(),
        newField().setId("field4").setType(Field.Type.PHOTO).build());
    setUpTestFeature("feature123");
    mockFeatureDocumentSnapshot(
        "feature001",
        new FeatureDocument(
            /* layerId */
            "layer001",
            /* customId */
            null,
            /* caption */
            null,
            /* location */
            null,
            /* geoJson */
            null,
            /* geometry */
            null,
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT));

    assertThrows(DataStoreException.class, () -> toFeature());
  }

  @Test
  public void testToFeature_zeroVertices() {
    setUpTestGeometry();
    setUpTestProject(
        "layer001",
        "form001",
        newField().setId("field1").setType(Field.Type.TEXT_FIELD).build(),
        newField()
            .setId("field2")
            .setType(Field.Type.MULTIPLE_CHOICE)
            .setMultipleChoice(
                MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build())
            .build(),
        newField().setId("field3").setType(Field.Type.MULTIPLE_CHOICE).build(),
        newField().setId("field4").setType(Field.Type.PHOTO).build());
    setUpTestFeature("feature123");
    mockFeatureDocumentSnapshot(
        "feature001",
        new FeatureDocument(
            /* layerId */
            "layer001",
            /* customId */
            null,
            /* caption */
            null,
            /* location */
            null,
            /* geoJson */
            null,
            /* geometry */
            noVerticesGeometry,
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT));

    assertThrows(DataStoreException.class, () -> toFeature());
  }

  private void setUpTestFeature(String featureId) {
    feature =
        newPolygonFeature()
            .setCreated(AUDIT_INFO_1)
            .setLastModified(AUDIT_INFO_2)
            .setVertices(newPolygonVertices())
            .setId(featureId)
            .setProject(project)
            .setLayer(layer)
            .build();
  }

  private void setUpTestGeometry() {
    geometry = new HashMap<>();
    geometry.put(FeatureConverter.GEOMETRY_COORDINATES, newGeoPointPolygonVertices());
    geometry.put(FeatureConverter.GEOMETRY_TYPE, FeatureConverter.POLYGON_TYPE);

    noVerticesGeometry = new HashMap<>();
    noVerticesGeometry.put(FeatureConverter.GEOMETRY_COORDINATES, null);
    noVerticesGeometry.put(FeatureConverter.GEOMETRY_TYPE, FeatureConverter.POLYGON_TYPE);
  }

  /** Mock observation document snapshot to return the specified id and object representation. */
  private void mockFeatureDocumentSnapshot(String id, FeatureDocument doc) {
    when(featureDocumentSnapshot.getId()).thenReturn(id);
    when(featureDocumentSnapshot.toObject(FeatureDocument.class)).thenReturn(doc);
  }

  private Feature toFeature() {
    return FeatureConverter.toFeature(project, featureDocumentSnapshot);
  }
}
