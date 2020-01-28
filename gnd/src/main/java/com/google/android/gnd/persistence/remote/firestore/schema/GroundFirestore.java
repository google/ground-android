/*
 * Copyright 2018 Google LLC
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

import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.model.observation.ObservationMutation;
import com.google.android.gnd.persistence.remote.RemoteDataEvent;
import com.google.android.gnd.persistence.remote.firestore.FeatureDoc;
import com.google.android.gnd.persistence.remote.firestore.ObservationDoc;
import com.google.android.gnd.persistence.remote.firestore.ProjectDoc;
import com.google.android.gnd.persistence.remote.firestore.base.FluentCollectionReference;
import com.google.android.gnd.persistence.remote.firestore.base.FluentDocumentReference;
import com.google.android.gnd.persistence.remote.firestore.base.FluentFirestore;
import com.google.android.gnd.persistence.remote.firestore.converters.QuerySnapshotConverter;
import com.google.common.collect.ImmutableList;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Object representation of Ground Firestore database. */
@Singleton
public class GroundFirestore extends FluentFirestore {
  private static final String PROJECTS = "projects";
  private static final String FEATURES = "features";
  private static final String RECORDS = "records";

  @Inject
  GroundFirestore(FirebaseFirestore db) {
    super(db);
  }

  public ProjectsCollectionReference projects() {
    return new ProjectsCollectionReference(db().collection(PROJECTS));
  }

  public static class ProjectsCollectionReference extends FluentCollectionReference {
    private static final String ACL_FIELD = "acl";
    private static final String READ_ACCESS = "r";

    protected ProjectsCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public ProjectDocumentReference project(String id) {
      return new ProjectDocumentReference(reference().document(id));
    }

    public Single<List<Project>> getReadable(User user) {
      return runQuery(
          reference().whereArrayContains(FieldPath.of(ACL_FIELD, user.getEmail()), READ_ACCESS),
          ProjectDoc::toObject);
    }
  }

  public static class ProjectDocumentReference extends FluentDocumentReference {
    protected ProjectDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public FeaturesCollectionReference features() {
      return new FeaturesCollectionReference(reference().collection(FEATURES));
    }

    public RecordsCollectionReference records() {
      return new RecordsCollectionReference(reference().collection(RECORDS));
    }

    public Maybe<Project> get() {
      return RxFirestore.getDocument(reference()).map(ProjectDoc::toObject);
    }
  }

  public static class FeaturesCollectionReference extends FluentCollectionReference {
    protected FeaturesCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public FeatureDocumentReference feature(String id) {
      return new FeatureDocumentReference(reference().document(id));
    }

    public Flowable<RemoteDataEvent<Feature>> observe(Project project) {
      return RxFirestore.observeQueryRef(reference())
          .flatMapIterable(
              featureQuerySnapshot ->
                  QuerySnapshotConverter.toEvents(
                      featureQuerySnapshot,
                      featureDocSnapshot -> FeatureDoc.toObject(project, featureDocSnapshot)));
    }
  }

  public static class FeatureDocumentReference extends FluentDocumentReference {
    protected FeatureDocumentReference(DocumentReference ref) {
      super(ref);
    }

    /** Appends the operation described by the specified mutation to the provided write batch. */
    public void addMutationToBatch(FeatureMutation mutation, User user, WriteBatch batch) {
      switch (mutation.getType()) {
        case CREATE:
        case UPDATE:
          merge(FeatureDoc.toMap(mutation, user), batch);
          break;
        case DELETE:
          // TODO: Implement me!
          break;
        default:
          throw new IllegalArgumentException("Unknown mutation type " + mutation.getType());
      }
    }
  }

  public static class RecordsCollectionReference extends FluentCollectionReference {
    protected RecordsCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public RecordDocumentReference record(String id) {
      return new RecordDocumentReference(reference().document(id));
    }

    public Single<ImmutableList<Observation>> recordsByFeatureId(Feature feature) {
      return RxFirestore.getCollection(byFeatureId(feature.getId()))
          .map(
              querySnapshot ->
                  stream(querySnapshot.getDocuments())
                      .map(
                          recordDoc ->
                              ObservationDoc.toObject(feature, recordDoc.getId(), recordDoc))
                      .collect(toImmutableList()))
          .toSingle(ImmutableList.of());
    }

    private Query byFeatureId(String featureId) {
      return reference().whereEqualTo(FieldPath.of(ObservationDoc.FEATURE_ID), featureId);
    }
  }

  public static class RecordDocumentReference extends FluentDocumentReference {
    protected RecordDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public Maybe<Observation> get(Feature feature) {
      return RxFirestore.getDocument(reference())
          .map(doc -> ObservationDoc.toObject(feature, doc.getId(), doc));
    }

    /** Appends the operation described by the specified mutation to the provided write batch. */
    public void addMutationToBatch(ObservationMutation mutation, User user, WriteBatch batch) {
      switch (mutation.getType()) {
        case CREATE:
        case UPDATE:
          merge(ObservationDoc.toMap(mutation, user), batch);
          break;
        case DELETE:
          // TODO: Implement me!
          break;
        default:
          throw new IllegalArgumentException("Unknown mutation type " + mutation.getType());
      }
    }
  }
}
