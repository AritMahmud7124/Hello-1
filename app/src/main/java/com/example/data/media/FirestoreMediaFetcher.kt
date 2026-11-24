package com.example.data.media

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import okio.buffer
import okio.source

class FirestoreMediaFetcher(
  private val context: Context,
  private val db: FirebaseFirestore,
  private val uri: Uri
) : Fetcher {

  override suspend fun fetch(): FetchResult {
    val file = FirestoreMediaStore.resolveToLocalFile(context, db, uri.toString())
    val ext = file.extension.lowercase()
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "image/jpeg"
    return SourceResult(
      source = ImageSource(
        source = file.source().buffer(),
        context = context
      ),
      mimeType = mimeType,
      dataSource = DataSource.DISK
    )
  }

  class Factory(context: Context) : Fetcher.Factory<Uri> {
    private val appContext = context.applicationContext
    private val db: FirebaseFirestore by lazy {
      FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }

    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
      if (data.scheme == FirestoreMediaStore.SCHEME && data.host == FirestoreMediaStore.HOST) {
        return FirestoreMediaFetcher(appContext, db, data)
      }
      return null
    }
  }
}
