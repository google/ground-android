package com.google.android.gnd.persistence.remote.firestore.schema;

import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.User;
import com.google.android.gnd.persistence.remote.firestore.ProjectDoc;
import com.google.android.gnd.persistence.remote.firestore.base.FirestoreCollectionReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldPath;
import io.reactivex.Single;
import java.util.List;

public class ProjectCollectionReference extends FirestoreCollectionReference {

  public static final String NAME = "projects";
  private static final String ACL_FIELD = "acl";
  private static final String READ_ACCESS = "r";

  protected ProjectCollectionReference(CollectionReference ref) {
    super(ref);
  }

  public ProjectDocumentReference project(String id) {
    return new ProjectDocumentReference(ref.document(id));
  }


  // TODO: Rename to something more self-descriptive.
  public Single<List<Project>> getReadable(User user) {
    return runQuery(
        ref.whereArrayContains(FieldPath.of(ACL_FIELD, user.getEmail()), READ_ACCESS),
        ProjectDoc::toObject);
  }
}
