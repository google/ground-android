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
import static com.google.android.ground.model.TestModelBuilders.newPointOfInterest;
import static com.google.android.ground.model.TestModelBuilders.newSurvey;
import static com.google.android.ground.model.TestModelBuilders.newTask;
import static com.google.common.truth.Truth.assertThat;
import static java8.util.J8Arrays.stream;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.google.android.ground.model.AuditInfo;
import com.google.android.ground.model.Survey;
import com.google.android.ground.model.User;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.model.locationofinterest.LocationOfInterest;
import com.google.android.ground.model.submission.MultipleChoiceResponse;
import com.google.android.ground.model.submission.ResponseMap;
import com.google.android.ground.model.submission.Submission;
import com.google.android.ground.model.submission.TextResponse;
import com.google.android.ground.model.task.MultipleChoice;
import com.google.android.ground.model.task.MultipleChoice.Cardinality;
import com.google.android.ground.model.task.Task;
import com.google.android.ground.persistence.remote.DataStoreException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;
import java8.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SubmissionConverterTest {

  @Mock private DocumentSnapshot submissionDocumentSnapshot;

  private Job job;
  private Survey survey;
  private LocationOfInterest locationOfInterest;

  private static final AuditInfo AUDIT_INFO_1 =
      newAuditInfo()
          .setUser(new User("user1", "", ""))
          .setClientTimestamp(new Date(100))
          .setServerTimestamp(Optional.of(new Date(101)))
          .build();

  private static final AuditInfo AUDIT_INFO_2 =
      newAuditInfo()
          .setUser(new User("user2", "", ""))
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

  private static final String SUBMISSION_ID = "submission123";

  @Test
  public void testToSubmission() {
    setUpTestSurvey(
        "job001",
        newTask().setId("task1").setType(Task.Type.TEXT_FIELD).build(),
        newTask()
            .setId("task2")
            .setType(Task.Type.MULTIPLE_CHOICE)
            .setMultipleChoice(
                MultipleChoice.newBuilder().setCardinality(Cardinality.SELECT_ONE).build())
            .build(),
        newTask().setId("task3").setType(Task.Type.MULTIPLE_CHOICE).build(),
        newTask().setId("task4").setType(Task.Type.PHOTO).build());
    setUpTestFeature("feature001");
    mockSubmissionDocumentSnapshot(
        SUBMISSION_ID,
        new SubmissionDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "task001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of(
                "task1",
                "Text response",
                "task2",
                ImmutableList.of("option2"),
                "task3",
                ImmutableList.of("optionA", "optionB"),
                "task4",
                "Photo URL")));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId(SUBMISSION_ID)
                .setSurvey(survey)
                .setLocationOfInterest(locationOfInterest)
                .setJob(job)
                .setResponses(
                    ResponseMap.builder()
                        .putResponse("task1", new TextResponse("Text response"))
                        .putResponse(
                            "task2", new MultipleChoiceResponse(null, ImmutableList.of("option2")))
                        .putResponse(
                            "task3",
                            new MultipleChoiceResponse(
                                null, ImmutableList.of("optionA", "optionB")))
                        .putResponse("task4", new TextResponse("Photo URL"))
                        .build())
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  @Test
  public void testToSubmission_mismatchedFeatureId() {
    setUpTestSurvey("job001", newTask().setId("task1").setType(Task.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockSubmissionDocumentSnapshot(
        SUBMISSION_ID,
        new SubmissionDocument(
            /* featureId */
            "feature999",
            /* taskId */
            "task001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("task1", "")));

    assertThrows(DataStoreException.class, this::toSubmission);
  }

  @Test
  public void testToSubmission_nullResponses() {
    setUpTestSurvey("job001", newTask().setId("task1").setType(Task.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockSubmissionDocumentSnapshot(
        SUBMISSION_ID,
        new SubmissionDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "task001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            null));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId(SUBMISSION_ID)
                .setSurvey(survey)
                .setLocationOfInterest(locationOfInterest)
                .setJob(job)
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  @Test
  public void testToSubmission_emptyTextResponse() {
    setUpTestSurvey("job001", newTask().setId("task1").setType(Task.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockSubmissionDocumentSnapshot(
        SUBMISSION_ID,
        new SubmissionDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "task001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("task1", "")));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId(SUBMISSION_ID)
                .setSurvey(survey)
                .setLocationOfInterest(locationOfInterest)
                .setJob(job)
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  @Test
  public void testToSubmission_emptyMultipleChoiceResponse() {
    setUpTestSurvey("job001", newTask().setId("task1").setType(Task.Type.MULTIPLE_CHOICE).build());
    setUpTestFeature("feature001");
    mockSubmissionDocumentSnapshot(
        SUBMISSION_ID,
        new SubmissionDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "task001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("task1", ImmutableList.of())));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId(SUBMISSION_ID)
                .setSurvey(survey)
                .setLocationOfInterest(locationOfInterest)
                .setJob(job)
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  private void setUpTestSurvey(String jobId, Task... tasks) {
    ImmutableMap.Builder<String, Task> taskMap = ImmutableMap.builder();
    stream(tasks).forEach(task -> taskMap.put(task.getId(), task));

    job = new Job(jobId, "jobName", taskMap.build());
    survey = newSurvey().putJob(job).build();
  }

  @Test
  public void testToSubmission_unknownFieldType() {
    setUpTestSurvey(
        "job001",
        newTask().setId("task1").setType(Task.Type.UNKNOWN).build(),
        newTask().setId("task2").setType(Task.Type.TEXT_FIELD).build());
    setUpTestFeature("feature001");
    mockSubmissionDocumentSnapshot(
        SUBMISSION_ID,
        new SubmissionDocument(
            /* featureId */
            "feature001",
            /* taskId */
            "task001",
            /* created */
            AUDIT_INFO_1_NESTED_OBJECT,
            /* lastModified */
            AUDIT_INFO_2_NESTED_OBJECT,
            /* responses */
            ImmutableMap.of("task1", "Unknown", "task2", "Text response")));

    assertThat(toSubmission())
        .isEqualTo(
            Submission.newBuilder()
                .setId(SUBMISSION_ID)
                .setSurvey(survey)
                .setLocationOfInterest(locationOfInterest)
                .setJob(job)
                .setResponses(
                    // Field "task1" with unknown field type ignored.
                    ResponseMap.builder()
                        .putResponse("task2", new TextResponse("Text response"))
                        .build())
                .setCreated(AUDIT_INFO_1)
                .setLastModified(AUDIT_INFO_2)
                .build());
  }

  private void setUpTestFeature(String featureId) {
    locationOfInterest =
        newPointOfInterest().setId(featureId).setSurvey(survey).setJob(job).build();
  }

  /** Mock submission document snapshot to return the specified id and object representation. */
  private void mockSubmissionDocumentSnapshot(String id, SubmissionDocument doc) {
    when(submissionDocumentSnapshot.getId()).thenReturn(id);
    when(submissionDocumentSnapshot.toObject(SubmissionDocument.class)).thenReturn(doc);
  }

  private Submission toSubmission() {
    return SubmissionConverter.toSubmission(locationOfInterest, submissionDocumentSnapshot);
  }
}
