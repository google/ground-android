package org.groundplatform.domain.repository

/**
 * Provides access to user-provided media stored locally and remotely. This currently includes only
 * photos.
 */
interface UserMediaRepositoryInterface {
  typealias MediaUri = String
  typealias MediaFile = String

  suspend fun createImageFile(fieldId: String): MediaFile

  fun addImageToGallery(filePath: String, title: String): String

  /**
   * Attempts to load the file from local cache. Else attempts to fetch it from Firestore Storage.
   * Returns the uri of the file.
   *
   * @param path Final destination path of the uploaded file relative to Firestore
   */
  suspend fun getDownloadUrl(path: String?): MediaUri

  /**
   * Returns the path of the file saved in the sdcard used for uploading to the provided destination
   * path.
   */
  fun getLocalFileFromRemotePath(destinationPath: String): MediaFile

  fun getUriForFile(mediaFile: MediaFile): MediaUri
}
