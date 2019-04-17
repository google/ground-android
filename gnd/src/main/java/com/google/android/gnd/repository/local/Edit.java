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

package com.google.android.gnd.repository.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverter;
import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import org.json.JSONObject;

/**
 * Fields describing operation type and old and new attribute values. Edits are maintained in
 * addition to latest local snapshot of entities to allow two-way data sync (merging local changes
 * with remote edits and vice versa) and rollback on sync failure.
 */
@AutoValue
public abstract class Edit {
  public enum Type implements IntEnum {
    UNKNOWN(0),
    CREATE(1),
    UPDATE(2),
    DELETE(3);

    private final int intValue;

    Type(int intValue) {
      this.intValue = intValue;
    }

    public int intValue() {
      return intValue;
    }

    @TypeConverter
    public static int toInt(@Nullable Edit.Type value) {
      return IntEnum.toInt(value, UNKNOWN);
    }

    @NonNull
    @TypeConverter
    public static Edit.Type fromInt(int intValue) {
      return IntEnum.fromInt(values(), intValue, UNKNOWN);
    }
  }

  @CopyAnnotations
  @ColumnInfo(name = "type")
  @NonNull
  public abstract Type getType();

  /**
   * For edits of type {@link Type#DELETE} and {@link Type#UPDATE}, returns a JSON object with the
   * original values of modified attributes before this edit. For all other edit types this will
   * return null. The old values are maintained to allow rollback in case remote sync fails.
   */
  @CopyAnnotations
  @ColumnInfo(name = "old_values")
  @Nullable
  public abstract JSONObject getOldValues();

  /**
   * For edits of type {@link Type#CREATE} and {@link Type#UPDATE}, returns a JSON object with the
   * new value of modified attributes after this edit. For all other edit types this will return
   * null.
   *
   * <p>Taken together, the old and new values represent changes as follows:
   *
   * <ul>
   *   <li>If a key is present in new values, but not the old, the entry represents an unset
   *       attribute that is now being set.
   *   <li>If a key is present in both old and new values, the entry represents an attribute that
   *       was already set but has now been modified.
   *   <li>If a key is present in old values but not the new, the entry represents an attribute that
   *       was present, but has been removed.
   * </ul>
   */
  @CopyAnnotations
  @ColumnInfo(name = "new_values")
  @Nullable
  public abstract JSONObject getNewValues();

  @NonNull
  public static Edit create(
      @NonNull Type type, @Nullable JSONObject oldValues, @Nullable JSONObject newValues) {
    return builder().setType(type).setOldValues(oldValues).setNewValues(newValues).build();
  }

  public static Builder builder() {
    return new AutoValue_Edit.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setType(Type type);

    public abstract Builder setOldValues(JSONObject oldValues);

    public abstract Builder setNewValues(JSONObject newValues);

    public abstract Edit build();
  }
}
