package com.google.android.gnd.ui.map;

import com.google.android.gnd.model.feature.Feature;
import com.google.common.collect.ImmutableSet;

import io.reactivex.Observable;

// TODO: After G4G, revisit this design/object hierarchy--an additional interface may not be
// necessary.
public interface ExtentSelector extends MapProvider.MapAdapter {

  Observable<Extent> getExtentSelections();

  // TODO: Simplify this? Instead of consuming a set of extents, can we update extents individually?
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
