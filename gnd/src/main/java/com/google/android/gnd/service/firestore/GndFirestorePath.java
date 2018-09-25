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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class GndFirestorePath extends FirestorePath {
  public static final String PROJECTS = "projects";
  public static final String PLACE_TYPES = "featureTypes";
  public static final String FORMS = "forms";
  public static final String PLACES = "features";
  public static final String RECORDS = "records";

  private GndFirestorePath(FirebaseFirestore db) {
    super(db);
  }

  public static GndFirestorePath db(FirebaseFirestore db) {
    return new GndFirestorePath(db);
  }

  public ProjectsRef projects() {
    return new ProjectsRef().setRef(collection(PROJECTS));
  }

  public ProjectRef project(String id) {
    return projects().project(id);
  }

  public static ProjectRef project(DocumentSnapshot projectDocSnapshot) {
    return project(projectDocSnapshot.getReference());
  }

  public static ProjectRef project(DocumentReference projectDocRef) {
    return new ProjectRef().setRef(projectDocRef);
  }

  public static PlaceRef place(DocumentSnapshot placeDocSnapshot) {
    return place(placeDocSnapshot.getReference());
  }

  public static PlaceRef place(DocumentReference placeDocRef) {
    return new PlaceRef().setRef(placeDocRef);
  }

  public static PlaceTypeRef placeType(DocumentSnapshot placeTypeDocSnapshot) {
    return placeType(placeTypeDocSnapshot.getReference());
  }

  public static PlaceTypeRef placeType(DocumentReference placeTypeDocRef) {
    return new PlaceTypeRef().setRef(placeTypeDocRef);
  }

  public static class ProjectsRef extends FluentCollectionReference {
    public ProjectRef project(String id) {
      return new ProjectRef().setRef(document(id));
    }

    public Query whereCanRead(User user) {
      return ref().whereArrayContains(FieldPath.of("acl", user.getEmail()), "r");
    }
  }

  public static class ProjectRef extends FluentDocumentReference {
    public PlacesRef places() {
      return new PlacesRef().setRef(collection(PLACES));
    }

    public PlaceRef place(String id) {
      return places().place(id);
    }

    public PlaceTypesRef placeTypes() {
      return new PlaceTypesRef().setRef(collection(PLACE_TYPES));
    }

    public PlaceTypeRef placeType(String id) {
      return placeTypes().placeType(id);
    }
  }

  public static class PlacesRef extends FluentCollectionReference {
    public PlaceRef place(String id) {
      return new PlaceRef().setRef(document(id));
    }
  }

  public static class PlaceTypesRef extends FluentCollectionReference {

    public PlaceTypeRef placeType(String id) {
      return new PlaceTypeRef().setRef(document(id));
    }
  }

  public static class PlaceTypeRef extends FluentDocumentReference {
    public FormsRef forms() {
      return new FormsRef().setRef(collection(FORMS));
    }
  }

  public static class FormsRef extends FluentCollectionReference {
    public FluentDocumentReference form(String id) {
      return new FluentDocumentReference().setRef(document(id));
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
  }
}
