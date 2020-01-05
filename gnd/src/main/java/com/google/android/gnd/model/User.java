package com.google.android.gnd.model;

/** Represents a single application user. */
public class User {

  public static final User ANONYMOUS = new User("", "", "");

  private final String uid;
  private final String email;
  private final String displayName;

  public User(String uid, String email, String displayName) {
    this.uid = uid;
    this.email = email;
    this.displayName = displayName;
  }

  public String getId() {
    return uid;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }
}
