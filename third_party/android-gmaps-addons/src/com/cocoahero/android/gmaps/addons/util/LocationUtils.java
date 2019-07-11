package com.cocoahero.android.gmaps.addons.util;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class LocationUtils {

    /**
     * Converts the given {@link Location} to a {@link LatLng}.
     * 
     * @param location A {@link Location} instance.
     * @return a {@link LatLng} with matching latitude and longitude of the
     *         given {@link Location}.
     */
    public static LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

}
