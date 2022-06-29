package com.google.android.gnd.ui.home.mapcontainer;

import static com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.DEFAULT_FEATURE_ZOOM_LEVEL;

import androidx.annotation.NonNull;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.map.CameraPosition;
import java8.util.Optional;

public class CameraUpdate {

    private final Point center;
    private final Optional<Float> zoomLevel;
    private final boolean allowZoomOut;

     CameraUpdate(Point center, Optional<Float> zoomLevel, boolean allowZoomOut) {
      this.center = center;
      this.zoomLevel = zoomLevel;
      this.allowZoomOut = allowZoomOut;
    }

    static CameraUpdate pan(Point center) {
      return new CameraUpdate(center, Optional.empty(), false);
    }

    static CameraUpdate panAndZoomIn(Point center) {
      return new CameraUpdate(center, Optional.of(DEFAULT_FEATURE_ZOOM_LEVEL), false);
    }

    static CameraUpdate panAndZoom(CameraPosition cameraPosition) {
      return new CameraUpdate(
          cameraPosition.getTarget(), Optional.of(cameraPosition.getZoomLevel()), true);
    }

    Point getCenter() {
      return center;
    }

    Optional<Float> getZoomLevel() {
      return zoomLevel;
    }

    boolean allowZoomOut() {
      return allowZoomOut;
    }

    @NonNull
    @Override
    public String toString() {
      if (zoomLevel.isPresent()) {
        return "Pan + zoom";
      } else {
        return "Pan";
      }
    }
  }
