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

import static com.google.android.gnd.model.TestModelBuilders.newTestAuditInfo;
import static com.google.android.gnd.model.TestModelBuilders.newTestFeature;
import static com.google.android.gnd.model.TestModelBuilders.newTestForm;
import static com.google.android.gnd.model.TestModelBuilders.newTestLayer;
import static com.google.android.gnd.model.TestModelBuilders.newTestProject;
import static com.google.android.gnd.model.TestModelBuilders.newTestUser;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.model.observation.Observation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;
import java8.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ObservationConverterTest {

  @Mock private DocumentSnapshot snapshot;

  @Before
  public void setUp() {}

  @Test
  public void testToObservation() {

    ImmutableList<Element> elements = ImmutableList.of();
    Form form = newTestForm().setId("form001").setElements(elements).build();
    Layer layer = newTestLayer().setId("layer001").setForm(form).build();
    Project project = newTestProject().putLayer("layer001", layer).build();
    Feature feature =
        newTestFeature().setId("feature001").setProject(project).setLayer(layer).build();

    ObservationDocument doc =
        new ObservationDocument(
            "feature001",
            "form001",
            new AuditInfoNestedObject(
                new UserNestedObject("user1", null, null),
                new Timestamp(new Date(100)),
                new Timestamp(new Date(101))),
            new AuditInfoNestedObject(
                new UserNestedObject("user2", null, null),
                new Timestamp(new Date(200)),
                new Timestamp(new Date(201))),
            ImmutableMap.of());

    when(snapshot.getId()).thenReturn("observation123");
    when(snapshot.toObject(ObservationDocument.class)).thenReturn(doc);

    Observation actual = ObservationConverter.toObservation(feature, snapshot);

    Observation expected =
        Observation.newBuilder()
            .setId("observation123")
            .setProject(project)
            .setFeature(feature)
            .setForm(form)
            .setCreated(
                newTestAuditInfo()
                    .setUser(newTestUser().setId("user1").build())
                    .setClientTimestamp(new Date(100))
                    .setServerTimestamp(Optional.of(new Date(101)))
                    .build())
            .setLastModified(
                newTestAuditInfo()
                    .setUser(newTestUser().setId("user2").build())
                    .setClientTimestamp(new Date(200))
                    .setServerTimestamp(Optional.of(new Date(201)))
                    .build())
            .build();
    assertThat(actual).isEqualTo(expected);
  }
}
