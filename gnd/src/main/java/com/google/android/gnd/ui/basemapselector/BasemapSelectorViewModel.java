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
  private HashSet<String> extentsPendingDownload = new HashSet<>();
  private PublishSubject<HashSet<String>> extentsPendingDownloadStream = PublishSubject.create();
  private HashSet<String> extentsPendingRemoval = new HashSet<>();
  private PublishSubject<HashSet<String>> extentsPendingRemovalStream = PublishSubject.create();
  private LiveData<String> downloadedExtents;
  private PublishSubject<String> downloadedExtentsSubject = PublishSubject.create();
  private PublishSubject<String> removedExtentsSubject = PublishSubject.create();
  private LiveData<String> removedExtents;
  private final DataRepository dataRepository;
  private final FileDownloadWorkManager downloadWorkManager;

  @Inject
  BasemapSelectorViewModel(
      DataRepository dataRepository, FileDownloadWorkManager downloadWorkManager) {
    this.dataRepository = dataRepository;
    this.downloadWorkManager = downloadWorkManager;

    this.tilesStream = this.dataRepository.getTilesOnceAndStream();

    this.tiles = LiveDataReactiveStreams.fromPublisher(tilesStream);

    this.downloadedExtents =
        LiveDataReactiveStreams.fromPublisher(
            downloadedExtentsSubject.toFlowable(BackpressureStrategy.LATEST));
    this.removedExtents =
        LiveDataReactiveStreams.fromPublisher(
            removedExtentsSubject.toFlowable(BackpressureStrategy.LATEST));
  }

  public void updateExtentsPendingDownload(Extent extent) {
    switch (extent.getState()) {
      case DOWNLOADED:
        extentsPendingRemoval.remove(extent.getId());
        extentsPendingDownload.remove(extent.getId());
        extentsPendingRemovalStream.onNext(extentsPendingRemoval);
        extentsPendingDownloadStream.onNext(extentsPendingDownload);
        break;
      case PENDING_DOWNLOAD:
        extentsPendingDownload.add(extent.getId());
        extentsPendingDownloadStream.onNext(extentsPendingDownload);
        break;
      case PENDING_REMOVAL:
        extentsPendingRemoval.add(extent.getId());
        extentsPendingRemovalStream.onNext(extentsPendingRemoval);
        break;
      case NONE:
        extentsPendingDownload.remove(extent.getId());
        extentsPendingRemoval.remove(extent.getId());
        extentsPendingDownloadStream.onNext(extentsPendingDownload);
        extentsPendingRemovalStream.onNext(extentsPendingRemoval);
        break;
      default:
    }
  }

  public LiveData<HashSet<String>> getExtentsPendingRemoval() {
    return LiveDataReactiveStreams.fromPublisher(
        extentsPendingRemovalStream.toFlowable(BackpressureStrategy.LATEST));
  }

  public LiveData<HashSet<String>> getExtentsPendingDownload() {
    return LiveDataReactiveStreams.fromPublisher(
        extentsPendingDownloadStream.toFlowable(BackpressureStrategy.LATEST));
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
    for (String extentId : extentsPendingDownload) {
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
