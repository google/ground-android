package com.google.android.gnd.repository;

import com.google.android.gnd.persistence.local.LocalValueStore;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MapsRepository {

  private final LocalValueStore localValueStore;

  @Inject
  public MapsRepository(LocalValueStore localValueStore) {
    this.localValueStore = localValueStore;
  }

  public void saveMapType(int type) {
    localValueStore.saveMapType(type);
  }

  public int getSavedMapType(int defaultType) {
    return localValueStore.getSavedMapType(defaultType);
  }
}
