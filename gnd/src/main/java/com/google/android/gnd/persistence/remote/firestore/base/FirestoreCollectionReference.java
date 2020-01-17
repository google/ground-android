package com.google.android.gnd.persistence.remote.firestore.base;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import durdinapps.rxfirebase2.RxFirestore;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java8.util.function.Function;

public class FirestoreCollectionReference {
  protected final CollectionReference ref;

  protected FirestoreCollectionReference(CollectionReference ref) {
    this.ref = ref;
  }

  /**
   * Returns a Completable that completes immediately on subscribe if network is available, or
   * fails in error if not.
   */
  protected Completable requireActiveNetwork() {
    return FirestoreUtil.requireActiveNetwork(ref.getFirestore());
  }

  /**
   * Runs the specified query, returning a Single containing a List of values created by applying
   * the mappingFunction to all results. Fails immediately with an error if an active network is
   * not available.
   */
  protected <T> Single<List<T>> runQuery(
      Query query, Function<DocumentSnapshot, T> mappingFunction) {
    return requireActiveNetwork()
        .andThen(
          FirestoreUtil.toSingleList(RxFirestore.getCollection(query), mappingFunction));
  }

  public CollectionReference ref() {
    return ref;
  }

  @Override
  public String toString() {
    return ref.getPath();
  }
}
