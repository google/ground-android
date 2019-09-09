package com.google.android.gnd.ui.map;

import com.google.android.gnd.model.feature.Feature;
import com.google.common.collect.ImmutableSet;

import io.reactivex.Observable;

public interface ExtentSelector extends MapProvider.MapAdapter {

  Observable<Extent> getExtentSelections();

  void updateExtentSelections(ImmutableSet<Extent> extents);

  void renderExtentSelectionLayer();

  // By default, we assume extent selectors don't care about markers.
  default Observable<MapMarker> getMarkerClicks() {
    return Observable.empty();
  }

  default void updateMarkers(ImmutableSet<Feature> features) {
    // Do nothing.
  }

  // By default, we assume the selector UI shouldn't render offline tiles, as these might clash with
  // selections.
  default void renderOfflineTiles() {
    // Do nothing.
  }
}
