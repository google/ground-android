/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.offlinearea.viewer;

import static java8.util.stream.StreamSupport.stream;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.repository.OfflineAreaRepository;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.common.collect.ImmutableSet;
import io.reactivex.processors.BehaviorProcessor;
import java.io.File;
import java.lang.ref.WeakReference;
import javax.inject.Inject;

public class OfflineAreaViewerViewModel extends AbstractViewModel {

  private final BehaviorProcessor<OfflineAreaViewerFragmentArgs> argsProcessor;
  private final OfflineAreaRepository offlineAreaRepository;
  private final WeakReference<Context> context;
  public LiveData<Double> areaStorageSize;
  private LiveData<OfflineArea> offlineArea;

  @Inject
  public OfflineAreaViewerViewModel(OfflineAreaRepository offlineAreaRepository, Context context) {
    this.argsProcessor = BehaviorProcessor.create();
    this.offlineAreaRepository = offlineAreaRepository;
    this.context = new WeakReference<>(context);
    this.areaStorageSize =
        LiveDataReactiveStreams.fromPublisher(
            this.argsProcessor.switchMap(
                args ->
                    this.offlineAreaRepository
                        .getOfflineArea(args.getOfflineAreaId())
                        .toFlowable()
                        .flatMap(offlineAreaRepository::getIntersectingDownloadedTilesOnceAndStream)
                        .map(this::tilesToTotalStorageSize)));
    this.offlineArea =
        LiveDataReactiveStreams.fromPublisher(
            this.argsProcessor.switchMap(
                args ->
                    this.offlineAreaRepository
                        .getOfflineArea(args.getOfflineAreaId())
                        .toFlowable()));
  }

  private Double tilesToTotalStorageSize(ImmutableSet<Tile> tiles) {
    return stream(tiles).map(this::tileStorageSize).reduce((x, y) -> x + y).orElse(0.0);
  }

  private double tileStorageSize(Tile tile) {
    if (context == null) {
      return 0.0;
    }

    File tileFile = new File(context.get().getFilesDir(), tile.getPath());
    return (double) tileFile.length() / (1024 * 1024);
  }

  public void onRemoveClick() {
    // TODO: Delete the area.
  }

  public LiveData<OfflineArea> getOfflineArea() {
    return offlineArea;
  }

  public void loadOfflineArea(OfflineAreaViewerFragmentArgs args) {
    this.argsProcessor.onNext(args);
  }
}
