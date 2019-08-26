package com.google.android.gnd.persistence.local.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public interface TileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdate(TileEntity tileEntity);

    @Query("SELECT * FROM tile")
    Flowable<List<TileEntity>> findAll();

    @Query("SELECT * FROM tile WHERE id = :id")
    Maybe<TileEntity> findById(String id);

    @Query("SELECT * FROM tile WHERE path = :path")
    Maybe<TileEntity> findByPath(String path);
}
