package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.media.FirestoreMediaFetcher
import com.example.security.PinnedNetworkClient

class HelloApplication : Application(), ImageLoaderFactory {
  override fun newImageLoader(): ImageLoader =
    ImageLoader.Builder(this)
      .callFactory(PinnedNetworkClient.client)
      .components {
        add(FirestoreMediaFetcher.Factory(applicationContext))
      }
      .crossfade(true)
      .build()
}
