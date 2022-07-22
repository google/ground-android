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

package com.google.android.ground.persistence.remote.firestore.schema;

import static com.google.android.ground.model.TestModelBuilders.newAuditInfo;
import static com.google.android.ground.model.TestModelBuilders.newField;
import static com.google.android.ground.model.TestModelBuilders.newGeoPointPolygonVertices;
import static com.google.android.ground.model.TestModelBuilders.newJob;
import static com.google.android.ground.model.TestModelBuilders.newPolygonOfInterest;
import static com.google.android.ground.model.TestModelBuilders.newPolygonVertices;
import static com.google.android.ground.model.TestModelBuilders.newSurvey;
import static com.google.android.ground.model.TestModelBuilders.newTask;
import static com.google.android.ground.model.TestModelBuilders.newUser;
import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static java8.util.J8Arrays.stream;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.geometry.Polygon;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.task.Field;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.MultipleChoice.Cardinality;
import com.google.android.ground.model.task.Step;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.remote.DataStoreException;
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
public class LoiConverterTest {

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

  private Job job;
  private Survey survey;
  private LocationOfInterest locationOfInterest;
  private Map<String, Object> geometry;
  private Map<String, Object> noVerticesGeometry;

  private void setUpTestSurvey(String jobId, String taskId, Field... fields) {
    Task task =
        newTask()
            .setId(taskId)
            .setSteps(stream(fields).map(Step::ofField).collect(toImmutableList()))
            .build();
    job = newJob().setId(jobId).setTask(task).build();
    survey = newSurvey().putJob(job).build();
  }

  @Test
  public void testToFeature() {
    setUpTestGeometry();
    setUpTestSurvey(
        "job001",
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
        new LoiDocument(
            /* jobId */
            "job001",
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

    assertThat(toLocationOfInterest()).isEqualTo(locationOfInterest);
  }

  @Test
  public void testToFeature_nullFeature() {
    setUpTestGeometry();
    setUpTestSurvey(
        "job001",
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
        new LoiDocument(
            /* jobId */
            "job001",
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

    assertThrows(DataStoreException.class, () -> toLocationOfInterest());
  }

  @Test
  public void testToFeature_zeroVertices() {
    setUpTestGeometry();
    setUpTestSurvey(
        "job001",
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
        new LoiDocument(
            /* jobId */
            "job001",
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

    assertThrows(DataStoreException.class, () -> toLocationOfInterest());
  }

  private void setUpTestFeature(String featureId) {
    locationOfInterest =
        newPolygonOfInterest()
            .setCreated(AUDIT_INFO_1)
            .setLastModified(AUDIT_INFO_2)
            .setGeometry(new Polygon(newPolygonVertices()))
            .setId(featureId)
            .setSurvey(survey)
            .setJob(job)
            .build();
  }

  private void setUpTestGeometry() {
    geometry = new HashMap<>();
    geometry.put(LoiConverter.GEOMETRY_COORDINATES, newGeoPointPolygonVertices());
    geometry.put(LoiConverter.GEOMETRY_TYPE, LoiConverter.POLYGON_TYPE);

    noVerticesGeometry = new HashMap<>();
    noVerticesGeometry.put(LoiConverter.GEOMETRY_COORDINATES, null);
    noVerticesGeometry.put(LoiConverter.GEOMETRY_TYPE, LoiConverter.POLYGON_TYPE);
  }

  /** Mock submission document snapshot to return the specified id and object representation. */
  private void mockFeatureDocumentSnapshot(String id, LoiDocument doc) {
    when(featureDocumentSnapshot.getId()).thenReturn(id);
    when(featureDocumentSnapshot.toObject(LoiDocument.class)).thenReturn(doc);
  }

  private LocationOfInterest toLocationOfInterest() {
    return LoiConverter.toLoi(survey, featureDocumentSnapshot);
  }
}
