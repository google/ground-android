/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gnd.ui.map.tileprovider.mapbox;

import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gnd.ui.map.tileprovider.CloseableTileProvider;
import com.google.android.gnd.ui.map.tileprovider.LocalTileProvider;
import java.io.File;

public class LocalMapboxTileProvider extends LocalTileProvider {

  static class CloseableMapboxTileProvider implements CloseableTileProvider {
    private final MapBoxOfflineTileProvider tileProvider;

    CloseableMapboxTileProvider(File file) {
      this.tileProvider = new MapBoxOfflineTileProvider(file);
    }

    @Override
    public Tile getTile(int x, int y, int z) {
      return this.tileProvider.getTile(x, y, z);
    }

    @Override
    public void close() {
      this.tileProvider.close();
    }
  }

  public LocalMapboxTileProvider(File file) {
    super(file, CloseableMapboxTileProvider::new);
  }
}
