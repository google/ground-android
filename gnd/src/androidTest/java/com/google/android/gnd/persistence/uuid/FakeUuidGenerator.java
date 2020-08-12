package com.google.android.gnd.persistence.uuid;

import javax.inject.Inject;

public class FakeUuidGenerator implements OfflineUuidGenerator {

  @Inject
  FakeUuidGenerator() {
  }

  @Override
  public String generateUuid() {
    return "TEST UUID";
  }
}
