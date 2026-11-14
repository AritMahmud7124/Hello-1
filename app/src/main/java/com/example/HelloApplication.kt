package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.media.FirestoreMediaFetcher

class HelloApplication : Application(), ImageLoaderFactory {
  override fun newImageLoader(): ImageLoader =
    ImageLoader.Builder(this)
      .components {
        add(FirestoreMediaFetcher.Factory(applicationContext))
      }
      .crossfade(true)
      .build()
}
