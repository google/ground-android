/*
 * Copyright 2020 Google LLC
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

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.remote.firestore.base.FluentCollectionReference;
import com.google.android.gnd.rx.annotations.Cold;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.List;

public class ProjectsCollectionReference extends FluentCollectionReference {
  private static final String ACL_FIELD = "acl";
  private static final String OWNER_ROLE = "owner";
  private static final String MANAGER_ROLE = "manager";
  private static final String CONTRIBUTOR_ROLE = "contributor";
  private static final String VIEWER_ROLE = "viewer";
  private static final List<String> VALID_ROLES =
      Arrays.asList(OWNER_ROLE, MANAGER_ROLE, CONTRIBUTOR_ROLE, VIEWER_ROLE);

  ProjectsCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public ProjectDocumentReference project(String id) {
    return new ProjectDocumentReference(reference().document(id));
  }

  @Cold
  public Single<List<Project>> getReadable(User user) {
    return runQuery(
        reference().whereIn(FieldPath.of(ACL_FIELD, user.getEmail()), VALID_ROLES),
        ProjectConverter::toProject);
  }
}
