package com.google.android.gnd.system;

import com.google.android.gnd.model.User;
import com.google.android.gnd.rx.ValueOrError;
import java8.util.Optional;

public class SignInState extends ValueOrError<User> {

  private final State state;

  public enum State {
    SIGNED_OUT,
    SIGNING_IN,
    SIGNED_IN,
    ERROR
  }

  SignInState(State state) {
    super(null, null);
    this.state = state;
  }

  SignInState(User user) {
    super(user, null);
    this.state = State.SIGNED_IN;
  }

  SignInState(Throwable error) {
    super(null, error);
    this.state = State.ERROR;
  }

  public State state() {
    return state;
  }

  public Optional<User> getUser() {
    return value();
  }
}