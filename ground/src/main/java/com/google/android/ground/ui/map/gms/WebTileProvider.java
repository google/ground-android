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

package com.google.android.ground.ui.map.gms;

import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.UrlTileProvider;
import java.net.MalformedURLException;
import java.net.URL;
import timber.log.Timber;

/**
 * Fetches tile imagery from a server according to a formatted URL.
 *
 * <p>Tile URLs should have the format: host/z/x/y
 */
public class WebTileProvider extends UrlTileProvider {
  private final String formatUrl;

  public WebTileProvider(String url) {
    super(256, 256);
    this.formatUrl = url;
  }

  @Override
  @Nullable
  public URL getTileUrl(int x, int y, int z) {
    String url =
        formatUrl
            .replace("${z}", Integer.toString(z))
            .replace("${x}", Integer.toString(x))
            .replace("${y}", Integer.toString(y));
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      Timber.e(e, "Tile URL malformed: %s", url);
      return null;
    }
  }
}
