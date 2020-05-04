package com.google.android.gnd.system;

import com.google.android.gnd.model.User;
import com.google.android.gnd.system.SignInState.State;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import javax.inject.Inject;


public class FakeAuthenticationManager implements AuthenticationManager {

  public Subject<SignInState> behaviourSubject = BehaviorSubject.create();

  private User user = User.builder()
    .setDisplayName("Bobby")
    .setEmail("d@d.com")
    .setId("MY ID")
    .build();

  @Inject
  public FakeAuthenticationManager(){};

  @Override
  public Observable<SignInState> getSignInState() {
    return behaviourSubject;
  }

  @Override
  public void signOut() {
    behaviourSubject.onNext(new SignInState(State.SIGNED_OUT));
  }

  @Override
  public User getCurrentUser() {
    return user;
  }

  @Override
  public void signIn() {
    // Notifies all listeners that a new item has arrived in the pipe
    // This is like a queue with one item in the buffer, new listeners will get the last item in the pipe
    // this wouldn't work with an observer because it's "cold"
    behaviourSubject.onNext(new SignInState(user));
  }

  @Override
  public void init() {
    behaviourSubject.onNext(new SignInState(State.SIGNED_OUT));
  }
}
