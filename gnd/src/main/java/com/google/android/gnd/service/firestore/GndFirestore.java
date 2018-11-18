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

import static java8.util.stream.StreamSupport.stream;

import com.google.android.gnd.service.DatastoreEvent;
import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Project;
import com.google.android.gnd.vo.Record;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collections;
import java.util.List;
import java8.util.function.Function;
import java8.util.stream.Collectors;

public class GndFirestore extends AbstractFluentFirestore {
  public static final String PROJECTS = "projects";
  public static final String PLACES = "features";
  public static final String RECORDS = "records";

  public GndFirestore(FirebaseFirestore db) {
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
          ProjectDoc::toProto);
    }
  }

  public static class ProjectDocumentReference extends FluentDocumentReference {
    protected ProjectDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public FeaturesCollectionReference places() {
      return new FeaturesCollectionReference(ref.collection(PLACES));
    }

    public RecordsCollectionReference records() {
      return new RecordsCollectionReference(ref.collection(RECORDS));
    }

    public Maybe<Project> get() {
      return RxFirestore.getDocument(ref).map(ProjectDoc::toProto);
    }
  }

  public static class FeaturesCollectionReference extends FluentCollectionReference {
    protected FeaturesCollectionReference(CollectionReference ref) {
      super(ref);
    }

    public FeatureDocumentReference feature(String id) {
      return new FeatureDocumentReference(ref.document(id));
    }

    public Single<Place> add(Place place) {
      return add(PlaceDoc.fromProto(place))
          .map(docRef -> place.toBuilder().setId(docRef.getId()).build());
    }

    public Flowable<DatastoreEvent<Place>> observe(Project project) {
      return super.observe()
          .flatMapIterable(
              placeQuerySnapshot ->
                  toDatastoreEvents(
                      placeQuerySnapshot,
                      placeDocSnapshot -> PlaceDoc.toProto(project, placeDocSnapshot)));
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

    public Single<List<Record>> getByFeature(Place feature) {
      return toSingleList(
          RxFirestore.getCollection(ref().whereEqualTo(FieldPath.of("featureId"), feature.getId())),
          doc -> RecordDoc.toProto(feature, doc.getId(), doc));
    }
  }

  public static class RecordDocumentReference extends FluentDocumentReference {
    protected RecordDocumentReference(DocumentReference ref) {
      super(ref);
    }

    public Maybe<Record> get(Place place) {
      return RxFirestore.getDocument(ref).map(doc -> RecordDoc.toProto(place, doc.getId(), doc));
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
}
