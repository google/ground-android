package com.google.android.gnd.model.basemap;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Area {
    public enum State {
        PENDING,
        IN_PROGRESS,
        DOWNLOADED,
        FAILED
    }

    public abstract String getId();

    public abstract State getState();

    public abstract LatLngBounds getBounds();

    public static Builder newBuilder() {
        return new AutoValue_Area.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        abstract LatLngBounds getBounds();

        public abstract Builder setBounds(LatLngBounds bounds);

        public abstract Builder setState(State state);

        abstract Builder setId(String id);

        abstract Area autoBuild();

        public Area build() {
            setId(getBounds().toString());
            return autoBuild();
        }
    }
}
