package com.google.android.ground.persistence.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestWorkerBuilder
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.basemap.tile.TileSet
import com.google.android.ground.persistence.local.stores.LocalTileSetStore
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TileSetDownloadWorkerTest : BaseHiltTest() {
  @Inject lateinit var localTileSetStore: LocalTileSetStore
  private lateinit var context: Context
  @Mock private lateinit var mockContext: Context

  private val factory =
    object : WorkerFactory() {
      override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
      ): ListenableWorker = TileSetDownloadWorker(appContext, workerParameters, localTileSetStore)
    }

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun doWork_SucceedsOnNoPendingTileSets() {
    val worker =
      TestWorkerBuilder<TileSetDownloadWorker>(context, SynchronousExecutor())
        .setWorkerFactory(factory)
        .build()

    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.success())
  }

  @Test
  fun doWork_SucceedsWhenTileSetAlreadyDownloaded() {
    val tilefile = File(context.filesDir, "TILESET")
    val out = FileOutputStream(tilefile)
    out.write(0)

    val tiles =
      TileSet(
        url = "http://google.com",
        id = "TILESET",
        path = "TILESET",
        state = TileSet.State.DOWNLOADED,
        offlineAreaReferenceCount = 1
      )
    localTileSetStore.insertOrUpdateTileSet(tiles).blockingAwait()
    val worker =
      TestWorkerBuilder<TileSetDownloadWorker>(context, SynchronousExecutor())
        .setWorkerFactory(factory)
        .build()

    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.success())
  }

  @Test
  fun doWork_RetriesOnFailure() {
    // Force an IOException by writing to a closed stream.
    `when`(mockContext.openFileOutput("TILESET", Context.MODE_PRIVATE))
      .thenReturn(FileOutputStream("fake").also { it.close() })
    val tiles =
      TileSet(
        url = "http://google.com",
        id = "TILESET",
        path = "TILESET",
        state = TileSet.State.PENDING,
        offlineAreaReferenceCount = 1
      )

    localTileSetStore.insertOrUpdateTileSet(tiles).blockingAwait()
    val worker =
      TestWorkerBuilder<TileSetDownloadWorker>(mockContext, SynchronousExecutor())
        .setWorkerFactory(factory)
        .build()

    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.retry())
  }

  @Test
  fun doWork_FailsOnInvalidURL() {
    val tiles =
      TileSet(
        url = "BAD URL",
        id = "TILESET",
        path = "TILESET",
        state = TileSet.State.PENDING,
        offlineAreaReferenceCount = 1
      )

    localTileSetStore.insertOrUpdateTileSet(tiles).blockingAwait()
    val worker =
      TestWorkerBuilder<TileSetDownloadWorker>(context, SynchronousExecutor())
        .setWorkerFactory(factory)
        .build()

    val result = worker.doWork()
    assertThat(result).isEqualTo(Result.failure())
  }
}
