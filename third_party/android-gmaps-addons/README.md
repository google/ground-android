# Android Google Maps API v2 Add-ons
A collection of add-ons for use with the Android Google Maps API v2 framework.

## Requirements
* Android SDK 8 or Higher
* Google Play Services Library

## Installation
1. Ensure your Android application project is set up to use the Google Maps API v2. Follow [these](https://developers.google.com/maps/documentation/android/start#installing_the_google_maps_android_v2_api) instructions if you need help.
2. Download or `git clone` this library into your application project.
3. Import the library into your Eclipse workspace.
4. Add the newly imported Android Library Project as a dependency to your application project.

## Sample Usage

### Online MapBox Map Layer
If you have a map layer created on MapBox.com, you can use an instance of MapBoxOnlineTileProvider to display it.

```` java
// Retrieve GoogleMap instance from MapFragment or elsewhere
GoogleMap map;

// Create new TileOverlayOptions instance.
TileOverlayOptions opts = new TileOverlayOptions();

// Find your MapBox online map ID.
String myMapID = "username.mapID";

// Create an instance of MapBoxOnlineTileProvider.
MapBoxOnlineTileProvider provider = new MapBoxOnlineTileProvider(myMapID);

// Set the tile provider on the TileOverlayOptions.
opts.tileProvider(provider);

// Add the tile overlay to the map.
TileOverlay overlay = map.addTileOverlay(opts);
````

### Offline MBTiles Layer
If you have an MBTiles offline layer you would like to use, the following code should work.

```` java
// Retrieve GoogleMap instance from MapFragment or elsewhere
GoogleMap map;

// Create new TileOverlayOptions instance.
TileOverlayOptions opts = new TileOverlayOptions();

// Get a File reference to the MBTiles file.
File myMBTiles;

// Create an instance of MapBoxOfflineTileProvider.
MapBoxOfflineTileProvider provider = new MapBoxOfflineTileProvider(myMBTiles);

// Set the tile provider on the TileOverlayOptions.
opts.tileProvider(provider);

// Add the tile overlay to the map.
TileOverlay overlay = map.addTileOverlay(opts);

// Sometime later when the map view is destroyed, close the provider.
// This is important to prevent a leak of the backing SQLiteDatabase.
provider.close();
````

## Tips & Tricks

### Google Maps Sans Google
If you have no need for Google's baselayer, you can turn it off by setting your GoogleMap instance's map type to `GoogleMap.MAP_TYPE_NONE`. This also has the added benefit of making your app completely offline capable when paired with a MapBoxOfflineTileProvider.

## Issues / Support
The key behind the beauty of open source software is community collaboration. Please do not email me, or any committers directly regarding issues or support of this library. Please use [GitHub Issues](https://github.com/cocoahero/android-gmaps-addons/issues) for these types of things so that others may help or learn from them.

## License
Copyright (c) 2014 Jonathan Baker

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
