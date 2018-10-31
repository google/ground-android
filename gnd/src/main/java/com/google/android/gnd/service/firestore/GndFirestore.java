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

import com.google.android.gnd.system.AuthenticationManager.User;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Maybe;

public class GndFirestore extends AbstractFluentFirestore {
  public static final String PROJECTS = "projects";
  public static final String PLACES = "features";
  public static final String RECORDS = "records";

  public GndFirestore(FirebaseFirestore db) {
    super(db);
  }

  public ProjectsRef projects() {
    return new ProjectsRef().setRef(collection(PROJECTS));
  }

  public ProjectRef project(String id) {
    return projects().project(id);
  }

  public static class ProjectsRef extends FluentCollectionReference {
    public ProjectRef project(String id) {
      return new ProjectRef().setRef(document(id));
    }

    public Maybe<QuerySnapshot> getReadable(User user) {
      return RxFirestore.getCollection(
          ref().whereArrayContains(FieldPath.of("acl", user.getEmail()), "r"));
    }
  }

  public static class ProjectRef extends FluentDocumentReference {
    public PlacesRef places() {
      return new PlacesRef().setRef(collection(PLACES));
    }

    public PlaceRef place(String id) {
      return places().place(id);
    }

    public RecordsRef records() {
      return new RecordsRef().setRef(collection(RECORDS));
    }
  }

  public static class PlacesRef extends FluentCollectionReference {
    public PlaceRef place(String id) {
      return new PlaceRef().setRef(document(id));
    }
  }

  public static class PlaceRef extends FluentDocumentReference {
    public RecordsRef records() {
      return new RecordsRef().setRef(collection(RECORDS));
    }

    public FluentDocumentReference record(String id) {
      return records().record(id);
    }
  }

  public static class RecordsRef extends FluentCollectionReference {
    public FluentDocumentReference record(String id) {
      return new FluentDocumentReference().setRef(document(id));
    }

    public Maybe<QuerySnapshot> getByFeatureId(String featureId) {
      return RxFirestore.getCollection(ref().whereEqualTo(FieldPath.of("featureId"), featureId));
    }
  }
}
