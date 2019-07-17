package com.google.android.gnd.persistence.local.room;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

public enum TileEntityState implements IntEnum {
    UNKNOWN(0),
    PENDING(1),
    DOWNLOADED(2),
    FAILED(3);

    private final int intValue;

    TileEntityState(int intValue) {
        this.intValue = intValue;
    }

    public int intValue() {
        return intValue;
    }

    @TypeConverter
    public static int toInt(@Nullable TileEntityState value) {
        return IntEnum.toInt(value, UNKNOWN);
    }

    @NonNull
    @TypeConverter
    public static TileEntityState fromInt(int intValue) {
        return IntEnum.fromInt(values(), intValue, UNKNOWN);
    }
}