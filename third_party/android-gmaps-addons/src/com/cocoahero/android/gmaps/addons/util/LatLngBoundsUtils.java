package com.cocoahero.android.gmaps.addons.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;

public class LatLngBoundsUtils {

    /**
     * Calculates the center coordinate of a {@link LatLngBounds}.
     * 
     * @param bounds A {@link LatLngBounds} instance.
     * @return the center coordinate of the given bounds.
     */
    public static LatLng getCenter(LatLngBounds bounds) {
        double n = bounds.northeast.latitude;
        double e = bounds.northeast.longitude;
        double s = bounds.southwest.latitude;
        double w = bounds.southwest.longitude;

        double lat = ((n + s) / 2.0);
        double lon = ((e + w) / 2.0);

        return new LatLng(lat, lon);
    }

    /**
     * Creates a {@link PolygonOptions} instance configured to visualize a
     * {@link LatLngBounds}.
     * 
     * @param bounds A {@link LatLngBounds} instance.
     * @return a {@link PolygonOptions} instance configured to visualize a
     *         {@link LatLngBounds}.
     */
    public static PolygonOptions toPolygonOptions(LatLngBounds bounds) {
        double n = bounds.northeast.latitude;
        double e = bounds.northeast.longitude;
        double s = bounds.southwest.latitude;
        double w = bounds.southwest.longitude;

        LatLng ne = new LatLng(n, e);
        LatLng nw = new LatLng(n, w);
        LatLng sw = new LatLng(s, w);
        LatLng se = new LatLng(s, e);

        PolygonOptions opts = new PolygonOptions();

        opts.add(ne);
        opts.add(nw);
        opts.add(sw);
        opts.add(se);

        return opts;
    }

}
