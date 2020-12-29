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

package com.google.android.gnd.rx;

import com.akaita.java.rxjava2debug.RxJava2Debug;
import io.reactivex.Completable;
import io.reactivex.CompletableConverter;
import io.reactivex.Flowable;
import io.reactivex.FlowableConverter;
import io.reactivex.Maybe;
import io.reactivex.MaybeConverter;
import io.reactivex.Single;
import io.reactivex.SingleConverter;
import io.reactivex.disposables.Disposable;
import org.reactivestreams.Subscription;
import timber.log.Timber;

public abstract class RxDebug {
  private static final String RX_TRACE_PREFIX = "[RxDebug]";

  /** Container for static helper methods. Do not instantiate. */
  private RxDebug() {}

  public static void logEnhancedStackTrace(Throwable t) {
    Timber.e(RxJava2Debug.getEnhancedStackTrace(t), "Unhandled Rx error");
  }

  /**
   * Returns a converter for use with {@link Maybe#as} that logs all events that occur on the source
   * stream.
   */
  public static <T> MaybeConverter<T, Maybe<T>> tracedMaybe(String tag, String streamName) {
    Tracer t = new Tracer(tag, streamName);
    return m ->
        m.doOnSubscribe(t::onSubscribe)
            .doOnSuccess(t::onSuccess)
            .doOnComplete(t::onComplete)
            .doOnError(t::onError);
  }

  /**
   * Returns a converter for use with {@link Single#as} that logs all events that occur on the
   * source stream.
   */
  public static <T> SingleConverter<T, Single<T>> tracedSingle(String tag, String streamName) {
    Tracer t = new Tracer(tag, streamName);
    return m -> m.doOnSubscribe(t::onSubscribe).doOnSuccess(t::onSuccess).doOnError(t::onError);
  }

  /**
   * Returns a converter for use with {@link Flowable#as} that logs all events that occur on the
   * source stream.
   */
  public static <T> FlowableConverter<T, Flowable<T>> tracedFlowable(
      String tag, String streamName) {
    Tracer t = new Tracer(tag, streamName);
    return f ->
        f.doOnSubscribe(t::onSubscribe)
            .doOnNext(t::onNext)
            .doOnComplete(t::onComplete)
            .doOnError(t::onError);
  }

  /**
   * Returns a converter for use with {@link Completable#as} that logs all events that occur on the
   * source stream.
   */
  public static CompletableConverter<Completable> tracedCompletable(String tag, String streamName) {
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
      Timber.tag(tag).v(RX_TRACE_PREFIX + " " + streamName + " " + action);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void onSubscribe(Disposable disposable) {
      trace(tag, streamName, "subscribe");
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void onSubscribe(Subscription subscription) {
      trace(tag, streamName, "subscribe");
    }

    private void onSuccess(Object value) {
      trace(tag, streamName, "success: " + value);
    }

    private void onNext(Object value) {
      trace(tag, streamName, "next: " + value);
    }

    private void onComplete() {
      trace(tag, streamName, "complete");
    }

    private void onError(Throwable t) {
      trace(tag, streamName, "error:  " + t);
    }
  }
}
