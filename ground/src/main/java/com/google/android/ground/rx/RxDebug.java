/*
 * Copyright 2018 Google LLC
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

package com.google.android.ground.rx;

import android.util.Log;
import com.akaita.java.rxjava2debug.RxJava2Debug;
import io.reactivex.Completable;
import io.reactivex.CompletableConverter;
import io.reactivex.Flowable;
import io.reactivex.FlowableConverter;
import io.reactivex.Maybe;
import io.reactivex.MaybeConverter;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscription;

public abstract class RxDebug {
  private static final String TAG = RxDebug.class.getSimpleName();
  private static final String RX_TRACE_PREFIX = "[RxDebug]";

  /** Container for static helper methods. Do not instantiate. */
  private RxDebug() {}

  public static <T> MaybeConverter<T, Maybe<T>> traceMaybe(String tag, String streamName) {
    Tracer t = new Tracer(tag, streamName);
    return m ->
        m.doOnSubscribe(t::onSubscribe)
            .doOnSuccess(t::onSuccess)
            .doOnComplete(t::onComplete)
            .doOnError(t::onError);
  }

  public static <T> FlowableConverter<T, Flowable<T>> traceFlowable(String tag, String streamName) {
    Tracer t = new Tracer(tag, streamName);
    return f ->
        f.doOnSubscribe(t::onSubscribe)
            .doOnNext(t::onNext)
            .doOnComplete(t::onComplete)
            .doOnError(t::onError);
  }

  public static CompletableConverter<Completable> traceCompletable(String tag, String streamName) {
    Tracer t = new Tracer(tag, streamName);
    return c -> c.doOnSubscribe(t::onSubscribe).doOnComplete(t::onComplete).doOnError(t::onError);
  }

  private static class Tracer {
    private final String tag;
    private final String streamName;

    private Tracer(String tag, String streamName) {
      this.tag = tag;
      this.streamName = streamName;
    }

    private static void trace(String tag, String streamName, String action) {
      Log.v(tag, RX_TRACE_PREFIX + " " + streamName + " " + action);
    }

    private void onSubscribe(Disposable disposable) {
      trace(tag, streamName, "subscribe");
    }

    public void onSubscribe(Subscription subscription) {
      trace(tag, streamName, "subscribe");
    }

    public void onSuccess(Object value) {
      trace(tag, streamName, "success: " + value);
    }

    public void onNext(Object value) {
      trace(tag, streamName, "next: " + value);
    }

    public void onComplete() {
      trace(tag, streamName, "complete");
    }

    public void onError(Throwable t) {
      trace(tag, streamName, "error:  " + t);
    }
  }
}
