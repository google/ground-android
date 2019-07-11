package com.cocoahero.android.gmaps.addons.osm;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.android.gms.maps.model.UrlTileProvider;

public class OpenStreetMapTileProvider extends UrlTileProvider {
    
    // ------------------------------------------------------------------------
    // Private Constants
    // ------------------------------------------------------------------------
    
    private static final String FORMAT = "http://tile.openstreetmap.org/%d/%d/%d.png";
    
    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public OpenStreetMapTileProvider() {
        super(256, 256);
    }
    
    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    @Override
    public URL getTileUrl(int x, int y, int z) {
        try {
            return new URL(String.format(FORMAT, z, x, y));
        }
        catch (MalformedURLException e) {
            return null;
        }
    }

}
