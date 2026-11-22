package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import com.example.BuildConfig
import java.io.File

enum class SecurityRiskLevel {
  CLEAN,
  PREVIEW_WARNING,
  BLOCKED
}

data class DeviceIntegrityReport(
  val isRooted: Boolean,
  val isEmulator: Boolean,
  val isDebuggerAttached: Boolean,
  val isDebugBuild: Boolean,
  val strictEnforcement: Boolean,
  val isBlocked: Boolean,
  val findings: List<String>,
  val riskLevel: SecurityRiskLevel
) {
  val summary: String
    get() = if (findings.isEmpty()) {
      "Hardware integrity verified (no root, emulator, or debugger detected)"
    } else {
      findings.joinToString(" • ")
    }
}

object DeviceIntegrityChecker {

  private val ROOT_BINARY_PATHS = listOf(
    "/system/app/Superuser.apk",
    "/sbin/su",
    "/system/bin/su",
    "/system/xbin/su",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/system/sd/xbin/su",
    "/system/bin/failsafe/su",
    "/data/local/su",
    "/su/bin/su",
    "/system/xbin/busybox",
    "/sbin/magisk",
    "/system/bin/magisk"
  )

  fun assess(
    context: Context,
    strictDebugEnforcement: Boolean = false
  ): DeviceIntegrityReport {
    val findings = mutableListOf<String>()
    val rooted = detectRoot(context, findings)
    val emulator = detectEmulator(findings)
    val debugger = detectDebugger(context, findings)
    val debugBuild = BuildConfig.DEBUG

    val anyThreat = rooted || emulator || debugger
    val enforceBlock = !debugBuild || strictDebugEnforcement
    val blocked = enforceBlock && (rooted || emulator || Debug.isDebuggerConnected() || Debug.waitingForDebugger() || (!debugBuild && debugger))

    val riskLevel = when {
      blocked -> SecurityRiskLevel.BLOCKED
      anyThreat -> SecurityRiskLevel.PREVIEW_WARNING
      else -> SecurityRiskLevel.CLEAN
    }

    return DeviceIntegrityReport(
      isRooted = rooted,
      isEmulator = emulator,
      isDebuggerAttached = debugger,
      isDebugBuild = debugBuild,
      strictEnforcement = enforceBlock,
      isBlocked = blocked,
      findings = findings,
      riskLevel = riskLevel
    )
  }

  private fun detectRoot(context: Context, findings: MutableList<String>): Boolean {
    var detected = false
    for (path in ROOT_BINARY_PATHS) {
      if (runCatching { File(path).exists() }.getOrDefault(false)) {
        findings.add("Root binary present ($path)")
        detected = true
        break
      }
    }

    val tags = Build.TAGS.orEmpty()
    if (tags.contains("test-keys", ignoreCase = true)) {
      findings.add("Unsigned test-keys ROM signature")
      detected = true
    }

    val rootPackages = listOf(
      "com.topjohnwu.magisk",
      "eu.chainfire.supersu",
      "com.noshufou.android.su",
      "com.koushikdutta.superuser"
    )
    val pm = context.packageManager
    for (pkg in rootPackages) {
      val installed = runCatching {
        pm.getPackageInfo(pkg, 0)
        true
      }.getOrDefault(false)
      if (installed) {
        findings.add("Privileged root manager installed ($pkg)")
        detected = true
        break
      }
    }

    val writableSystem = runCatching {
      File("/system").canWrite()
    }.getOrDefault(false)
    if (writableSystem) {
      findings.add("Writable /system partition")
      detected = true
    }
    return detected
  }

  private fun detectEmulator(findings: MutableList<String>): Boolean {
    val fingerprint = Build.FINGERPRINT.orEmpty()
    val model = Build.MODEL.orEmpty()
    val manufacturer = Build.MANUFACTURER.orEmpty()
    val brand = Build.BRAND.orEmpty()
    val device = Build.DEVICE.orEmpty()
    val product = Build.PRODUCT.orEmpty()
    val hardware = Build.HARDWARE.orEmpty()

    val isEmu = fingerprint.startsWith("generic", ignoreCase = true) ||
      fingerprint.startsWith("unknown", ignoreCase = true) ||
      fingerprint.contains("emulator", ignoreCase = true) ||
      model.contains("google_sdk", ignoreCase = true) ||
      model.contains("Emulator", ignoreCase = true) ||
      model.contains("Android SDK built for", ignoreCase = true) ||
      manufacturer.contains("Genymotion", ignoreCase = true) ||
      hardware.contains("goldfish", ignoreCase = true) ||
      hardware.contains("ranchu", ignoreCase = true) ||
      hardware.contains("vbox86", ignoreCase = true) ||
      product.contains("sdk_gphone", ignoreCase = true) ||
      product.contains("emulator", ignoreCase = true) ||
      product.contains("simulator", ignoreCase = true) ||
      (brand.startsWith("generic", ignoreCase = true) && device.startsWith("generic", ignoreCase = true))

    if (isEmu) {
      findings.add("Virtual / emulator runtime environment ($hardware/$model)")
    }
    return isEmu
  }

  private fun detectDebugger(context: Context, findings: MutableList<String>): Boolean {
    var detected = false
    if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
      findings.add("Active JDWP debugger attached")
      detected = true
    }
    val debuggableFlag = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (debuggableFlag && !BuildConfig.DEBUG) {
      findings.add("Repackaged debuggable flag on release binary")
      detected = true
    }
    return detected
  }
}
