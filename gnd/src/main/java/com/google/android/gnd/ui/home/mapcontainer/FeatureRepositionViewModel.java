package com.google.android.gnd.ui.home.mapcontainer;

import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.map.MapProvider;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

public class FeatureRepositionViewModel extends AbstractViewModel {

  private final MapProvider mapProvider;

  @Hot private final Subject<Point> confirmButtonClicks = PublishSubject.create();
  @Hot private final Subject<Nil> cancelButtonClicks = PublishSubject.create();

  @Inject
  FeatureRepositionViewModel(MapProvider mapProvider) {
    this.mapProvider = mapProvider;
  }

  public void onConfirmButtonClick() {
    confirmButtonClicks.onNext(mapProvider.getCameraTarget());
  }

  public void onCancelButtonClick() {
    cancelButtonClicks.onNext(Nil.NIL);
  }

  public Observable<Point> getConfirmButtonClicks() {
    return confirmButtonClicks;
  }

  public Observable<Nil> getCancelButtonClicks() {
    return cancelButtonClicks;
  }
}
