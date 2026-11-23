package com.example.security

import java.util.concurrent.TimeUnit
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion

object PinnedNetworkClient {

  // Real Google Trust Services & GlobalSign Root CA SPKI SHA-256 hashes
  private const val PIN_GTS_ROOT_R1 = "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc="
  private const val PIN_GTS_ROOT_R2 = "sha256/Vfd95BwDeSQo+NUYxVEEIlvkOlWY2SalKK1lPhzOx78="
  private const val PIN_GTS_ROOT_R3 = "sha256/QXnt2YHvdHR3tJYmQIr0Paosp6t/nggsEGD4QJZ3Q0g="
  private const val PIN_GTS_ROOT_R4 = "sha256/mEflZT5enoR1FuXLgYYGqnVEoZvmf9c2bVBpiOjYQ0c="
  private const val PIN_GLOBALSIGN_R3 = "sha256/cGuxAXyFXFkWm61cF4HPWX8S0srS9j0aSqN0k4AP+4A="
  private const val PIN_GLOBALSIGN_R4 = "sha256/CLOmM1/OXvSPjw5UOYbAf9GKOxImEp9hhku9W90fHMk="

  private val certificatePinner: CertificatePinner by lazy {
    CertificatePinner.Builder()
      .add("*.googleapis.com", PIN_GTS_ROOT_R1, PIN_GTS_ROOT_R2, PIN_GTS_ROOT_R3, PIN_GTS_ROOT_R4, PIN_GLOBALSIGN_R3, PIN_GLOBALSIGN_R4)
      .add("*.firebaseio.com", PIN_GTS_ROOT_R1, PIN_GTS_ROOT_R2, PIN_GTS_ROOT_R3, PIN_GTS_ROOT_R4, PIN_GLOBALSIGN_R3, PIN_GLOBALSIGN_R4)
      .add("*.firebaseapp.com", PIN_GTS_ROOT_R1, PIN_GTS_ROOT_R2, PIN_GTS_ROOT_R3, PIN_GTS_ROOT_R4, PIN_GLOBALSIGN_R3, PIN_GLOBALSIGN_R4)
      .add("*.ytimg.com", PIN_GTS_ROOT_R1, PIN_GTS_ROOT_R2, PIN_GTS_ROOT_R3, PIN_GTS_ROOT_R4, PIN_GLOBALSIGN_R3, PIN_GLOBALSIGN_R4)
      .add("*.youtube.com", PIN_GTS_ROOT_R1, PIN_GTS_ROOT_R2, PIN_GTS_ROOT_R3, PIN_GTS_ROOT_R4, PIN_GLOBALSIGN_R3, PIN_GLOBALSIGN_R4)
      .build()
  }

  private val strictTlsSpec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
    .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
    .build()

  val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .certificatePinner(certificatePinner)
      .connectionSpecs(listOf(strictTlsSpec))
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .followSslRedirects(false)
      .build()
  }
}
