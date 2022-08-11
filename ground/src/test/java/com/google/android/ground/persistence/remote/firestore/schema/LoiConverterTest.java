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

import static com.google.android.ground.model.TestModelBuilders.newGeoPointPolygonVertices;
import static com.google.android.ground.model.TestModelBuilders.newSurvey;
import static com.google.android.ground.model.TestModelBuilders.newTask;
import static java8.util.J8Arrays.stream;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.google.android.ground.model.Survey;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.MultipleChoice.Cardinality;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoiConverterTest {

  @Mock private DocumentSnapshot featureDocumentSnapshot;

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

  private Survey survey;
  private Map<String, Object> noVerticesGeometry;

  private void setUpTestSurvey(String jobId, Task... tasks) {
    ImmutableMap.Builder<String, Task> taskMap = ImmutableMap.builder();
    stream(tasks).forEach(task -> taskMap.put(task.getId(), task));

    Job job = new Job(jobId, "jobName", taskMap.build());
    survey = newSurvey().putJob(job).build();
  }

  @Test
  public void testToFeature_nullFeature() {
    setUpTestGeometry();
    setUpTestSurvey(
        "job001",
        newTask("task1"),
        newTask("task2", Task.Type.MULTIPLE_CHOICE, MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build()),
        newTask("task3", Task.Type.MULTIPLE_CHOICE),
        newTask("task4", Task.Type.PHOTO));
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
        newTask("task1"),
        newTask("task2", Task.Type.MULTIPLE_CHOICE, MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build()),
        newTask("task3", Task.Type.MULTIPLE_CHOICE),
        newTask("task4", Task.Type.PHOTO));
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

  private void setUpTestGeometry() {
    Map<String, Object> geometry = new HashMap<>();
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
