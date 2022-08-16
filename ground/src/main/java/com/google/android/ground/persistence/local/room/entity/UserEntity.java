/*
 * Copyright 2019 Google LLC
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

package com.google.android.ground.persistence.local.room.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.android.ground.model.User;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;

@AutoValue
@Entity(tableName = "user")
public abstract class UserEntity {

  @CopyAnnotations
  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "id")
  public abstract String getId();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "email")
  public abstract String getEmail();

  @CopyAnnotations
  @NonNull
  @ColumnInfo(name = "display_name")
  public abstract String getDisplayName();

  // TODO(https://github.com/google/ground-android/issues/964): Save to remote db
  @CopyAnnotations
  @Nullable
  @ColumnInfo(name = "photo_url")
  public abstract String getPhotoUrl();

  public static UserEntity fromUser(User user) {
    return UserEntity.builder().setId(user.getId()).setEmail(user.getEmail())
        .setDisplayName(user.getDisplayName()).setPhotoUrl(user.getPhotoUrl()).build();
  }

  public static User toUser(UserEntity u) {
    return new User(u.getId(), u.getEmail(), u.getDisplayName(), u.getPhotoUrl());
  }

  public static UserEntity create(String id, String email, String displayName, String photoUrl) {
    return builder().setId(id).setEmail(email).setDisplayName(displayName).setPhotoUrl(photoUrl)
        .build();
  }

  public static Builder builder() {
    return new AutoValue_UserEntity.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setId(String id);

    public abstract Builder setEmail(String email);

    public abstract Builder setDisplayName(String displayName);

    public abstract Builder setPhotoUrl(@Nullable String photoUrl);

    public abstract UserEntity build();
  }
}
