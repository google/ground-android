package com.google.android.gnd.system.auth;

import com.google.android.gnd.model.User;
import com.google.android.gnd.system.auth.SignInState.State;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;

public class FakeAuthenticationManager implements AuthenticationManager {

  private static final String TAG = FakeAuthenticationManager.class.toString();

  public Subject<SignInState> behaviourSubject = BehaviorSubject.create();

  public final static User TEST_USER = User.builder()
    .setDisplayName("Test User")
    .setEmail("test@user.com")
    .setId("TEST_USER_ID")
    .build();

  @Inject
  public FakeAuthenticationManager() {
  }

  @Override
  public Observable<SignInState> getSignInState() {
    return behaviourSubject;
  }

  @Override
  public User getCurrentUser() {
    return TEST_USER;
  }

  @Override
  public void init() {
    behaviourSubject.onNext(new SignInState(TEST_USER));
  }

  @Override
  public void signIn() {
    // Notifies all listeners that a new item has arrived in the pipe
    // This is like a queue with one item in the buffer, new listeners will get the last item in the pipe
    // this wouldn't work with an observer because it's "cold"
    behaviourSubject.onNext(new SignInState(TEST_USER));
  }

  @Override
  public void signOut() {
    behaviourSubject.onNext(new SignInState(State.SIGNED_OUT));
  }


}
