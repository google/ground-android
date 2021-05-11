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

package com.google.android.gnd.ui.map.tileprovider;

import com.google.android.gms.maps.model.Tile;
import java.io.File;
import java.io.IOException;
import java8.util.function.Function;

public abstract class LocalTileProvider implements CloseableTileProvider {
  public CloseableTileProvider tileProvider;

  public LocalTileProvider(File file, Function<File, CloseableTileProvider> providerFunction) {
    this.tileProvider = providerFunction.apply(file);
  }

  @Override
  public void close() throws IOException {
    this.tileProvider.close();
  }

  @Override
  public Tile getTile(int x, int y, int z) {
    return this.tileProvider.getTile(x, y, z);
  }
}
