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

package com.google.android.gnd.service.firestore;

import static java8.util.stream.Collectors.toList;
import static java8.util.stream.StreamSupport.stream;

import android.util.Log;
import com.google.android.gnd.service.DataStoreEvent;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SnapshotMetadata;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;

/** Object representation of Ground Firestore database. */
public class GndFirestore extends AbstractFluentFirestore {
  private static final String TAG = GndFirestore.class.getSimpleName();

  private static final String PROJECTS = "projects";
  private static final String FEATURES = "features";
  private static final String RECORDS = "records";

  GndFirestore(FirebaseFirestore db) {
    super(db);
  }

  public ProjectsCollectionReference projects() {
    return new ProjectsCollectionReference(db.collection(PROJECTS));
  }

  public static class ProjectsCollectionReference extends FluentCollectionReference {
    private static final String ACL_FIELD = "acl";
    private static final String READ_ACCESS = "r";

    protected ProjectsCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public ProjectDocumentReference project(String id) {
      return new ProjectDocumentReference(ref.document(id));
    }

    public Single<List<Project>> getReadable(User user) {
      return toSingleList(
          RxFirestore.getCollection(
              ref.whereArrayContains(FieldPath.of(ACL_FIELD, user.getEmail()), READ_ACCESS)),
          ProjectDoc::toObject);
    }
  }

  public static class ProjectDocumentReference extends FluentDocumentReference {
    protected ProjectDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public FeaturesCollectionReference features() {
      return new FeaturesCollectionReference(ref.collection(FEATURES));
    }

    public RecordsCollectionReference records() {
      return new RecordsCollectionReference(ref.collection(RECORDS));
    }

    public Maybe<Project> get() {
      return RxFirestore.getDocument(ref).map(ProjectDoc::toObject);
    }
  }

  public static class FeaturesCollectionReference extends FluentCollectionReference {
    protected FeaturesCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public FeatureDocumentReference feature(String id) {
      return new FeatureDocumentReference(ref.document(id));
    }

    public Single<Feature> add(Feature feature) {
      return RxFirestore.addDocument(ref, FeatureDoc.fromObject(feature))
          .map(docRef -> feature.toBuilder().setRemoteId(docRef.getId()).build());
    }

    public Flowable<DataStoreEvent<Feature>> observe(Project project) {
      return RxFirestore.observeQueryRef(ref)
          .flatMapIterable(
              featureQuerySnapshot ->
                  toDataStoreEvents(
                      featureQuerySnapshot,
                      featureDocSnapshot -> FeatureDoc.toObject(project, featureDocSnapshot)));
    }
  }

  public static class FeatureDocumentReference extends FluentDocumentReference {
    protected FeatureDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public RecordsCollectionReference records() {
      return new RecordsCollectionReference(ref.collection(RECORDS));
    }
  }

  public static class RecordsCollectionReference extends FluentCollectionReference {
    protected RecordsCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public RecordDocumentReference record(String id) {
      return new RecordDocumentReference(ref.document(id));
    }

    public Single<List<Record>> getByFeature(Feature feature) {
      return toSingleList(
          RxFirestore.getCollection(
              ref().whereEqualTo(FieldPath.of("featureId"), feature.getRemoteId())),
          doc -> RecordDoc.toObject(feature, doc.getId(), doc));
    }
  }

  public static class RecordDocumentReference extends FluentDocumentReference {
    protected RecordDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public Maybe<Record> get(Feature feature) {
      return RxFirestore.getDocument(ref).map(doc -> RecordDoc.toObject(feature, doc.getId(), doc));
    }
  }

  /**
   * Applies the provided mapping function to each document in the specified query snapshot, if
   * present. If no results are present, completes with an empty list.
   */
  private static <T> Single<List<T>> toSingleList(
      Maybe<QuerySnapshot> result, Function<DocumentSnapshot, T> mappingFunction) {
    return result
        .map(
            querySnapshot ->
                stream(querySnapshot.getDocuments())
                    .map(mappingFunction)
                    .collect(Collectors.toList()))
        .toSingle(Collections.emptyList());
  }

  private static <T> Iterable<DataStoreEvent<T>> toDataStoreEvents(
      QuerySnapshot snapshot, Function<DocumentSnapshot, T> converter) {
    DataStoreEvent.Source source = getSource(snapshot.getMetadata());
    return stream(snapshot.getDocumentChanges())
        .map(dc -> toDataStoreEvent(dc, source, converter))
        .filter(DataStoreEvent::isValid)
        .collect(toList());
  }

  private static <T> DataStoreEvent<T> toDataStoreEvent(
      DocumentChange dc, DataStoreEvent.Source source, Function<DocumentSnapshot, T> converter) {
    Log.v(TAG, dc.getDocument().getReference().getPath() + " " + dc.getType());
    try {
      String id = dc.getDocument().getId();
      switch (dc.getType()) {
        case ADDED:
          return DataStoreEvent.loaded(id, source, converter.apply(dc.getDocument()));
        case MODIFIED:
          return DataStoreEvent.modified(id, source, converter.apply(dc.getDocument()));
        case REMOVED:
          return DataStoreEvent.removed(id, source);
      }
    } catch (DataStoreException e) {
      Log.d(TAG, "Data store error:", e);
    }
    return DataStoreEvent.invalidResponse();
  }

  private static DataStoreEvent.Source getSource(SnapshotMetadata metadata) {
    return metadata.hasPendingWrites()
        ? DataStoreEvent.Source.LOCAL_DATA_STORE
        : DataStoreEvent.Source.REMOTE_DATA_STORE;
  }
}
