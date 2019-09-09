package com.google.android.gnd.ui.basemapselector;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;

import com.google.android.gnd.model.basemap.tile.Tile;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.ui.map.Extent;
import com.google.android.gnd.workers.FileDownloadWorkManager;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;

import javax.inject.Inject;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;

import static com.google.android.gnd.util.ImmutableSetCollector.toImmutableSet;
import static java8.util.stream.StreamSupport.stream;

/**
 * This view model is responsible for managing state for the {@link BasemapSelectorFragment}.
 * Together, they constitute a basemap selector that users can interact with to select portions of a
 * basemap for offline viewing. Among other things, this view model is responsible for receiving
 * requests to download basemap files and for scheduling those requests with an {@link
 * com.google.android.gnd.workers.FileDownloadWorker}.
 */
public class BasemapSelectorViewModel extends ViewModel {
  private static final String TAG = BasemapSelectorViewModel.class.getName();

  private final Flowable<ImmutableSet<Tile>> tilesStream;
  private final LiveData<ImmutableSet<Tile>> tiles;
  private final LiveData<ImmutableSet<Tile>> downloadedTiles;
  private final LiveData<ImmutableSet<Tile>> pendingTiles;
  private HashSet<String> selectedExtents = new HashSet<>();
  private PublishSubject<String> downloadedExtentsSubject = PublishSubject.create();
  private LiveData<String> downloadedExtents;
  private PublishSubject<String> removedExtentsSubject = PublishSubject.create();
  private LiveData<String> removedExtents;
  private HashSet<String> extentsPendingRemoval = new HashSet<>();
  private final DataRepository dataRepository;
  private final FileDownloadWorkManager downloadWorkManager;

  @Inject
  BasemapSelectorViewModel(
      DataRepository dataRepository, FileDownloadWorkManager downloadWorkManager) {
    this.dataRepository = dataRepository;
    this.downloadWorkManager = downloadWorkManager;

    this.tilesStream = this.dataRepository.getTilesOnceAndStream();

    this.tiles = LiveDataReactiveStreams.fromPublisher(tilesStream);

    this.downloadedTiles =
        LiveDataReactiveStreams.fromPublisher(
            tilesStream.map(
                ts ->
                    stream(ts)
                        .filter(tile -> tile.getState() == Tile.State.DOWNLOADED)
                        .collect(toImmutableSet())));

    this.pendingTiles =
        LiveDataReactiveStreams.fromPublisher(
            tilesStream.map(
                ts ->
                    stream(ts)
                        .filter(tile -> tile.getState() == Tile.State.PENDING)
                        .collect(toImmutableSet())));

    this.downloadedExtents =
        LiveDataReactiveStreams.fromPublisher(
            downloadedExtentsSubject.toFlowable(BackpressureStrategy.LATEST));
    this.removedExtents =
        LiveDataReactiveStreams.fromPublisher(
            removedExtentsSubject.toFlowable(BackpressureStrategy.LATEST));
  }

  public void updateSelectedExtents(Extent extent) {
    switch (extent.getState()) {
      case PENDING_DOWNLOAD:
        selectedExtents.add(extent.getId());
        break;
      case PENDING_REMOVAL:
        extentsPendingRemoval.add(extent.getId());
        break;
      case NONE:
        selectedExtents.remove(extent.getId());
        break;
      default:
    }
  }


  public LiveData<ImmutableSet<Tile>> getDownloadedTiles() {
    return downloadedTiles;
  }

  public LiveData<ImmutableSet<Tile>> getPendingTiles() {
    return pendingTiles;
  }

  public LiveData<String> getDownloadedExtents() {
    return downloadedExtents;
  }

  public LiveData<String> getRemovedExtents() {
    return removedExtents;
  }

  public LiveData<ImmutableSet<Tile>> getTiles() {
    return tiles;
  }

  /** Download selected extents. */
  public void downloadExtents() {
    for (String extentId : selectedExtents) {
      Log.d(TAG, "Downloading: " + extentId);
      downloadedExtentsSubject.onNext(extentId);
      downloadWorkManager
          .enqueueFileDownloadWorker(extentId)
          .subscribe(() -> Log.d(TAG, "Download worker queued"));
    }
  }

  /** Remove selected extents. */
  public void removeExtents() {
    Log.d(TAG, "Removing extents.");
    for (String extentId : extentsPendingRemoval) {
      Log.d(TAG, "Removing extent: " + extentId);
      removedExtentsSubject.onNext(extentId);
      downloadWorkManager
          .enqueueRemovalWorker(extentId)
          .onErrorComplete()
          .subscribe(() -> Log.d(TAG, "Removal worker queued"));
    }
  }
}
