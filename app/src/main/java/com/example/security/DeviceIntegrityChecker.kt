package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import com.example.BuildConfig
import java.io.File
import java.security.MessageDigest

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
      "Hardware integrity verified (no root, emulator, debugger, hooking, or tampering detected)"
    } else {
      findings.joinToString(" • ")
    }
}

object DeviceIntegrityChecker {

  private val TRUSTED_SIGNING_SHA256 = setOf(
    "6DA7E22BE9EDE14E658F2BDCE766E482F0D89BC78C266F309B506391B958301C"
  )

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

  private val HOOKING_ARTIFACT_PATHS = listOf(
    "/data/local/tmp/frida-server",
    "/data/local/tmp/re.frida.server",
    "/system/framework/XposedBridge.jar"
  )

  fun assess(
    context: Context,
    strictDebugEnforcement: Boolean = false
  ): DeviceIntegrityReport {
    val findings = mutableListOf<String>()
    val rooted = detectRoot(context, findings)
    val emulator = detectEmulator(findings)
    val debugger = detectDebugger(context, findings)
    val hooked = detectHookingFramework(context, findings)
    val tamperedSig = detectRepackagedSignature(context, findings)
    val debugBuild = BuildConfig.DEBUG

    val anyThreat = rooted || emulator || debugger || hooked || tamperedSig
    val enforceBlock = !debugBuild || strictDebugEnforcement
    val blocked = enforceBlock && (
      rooted ||
        emulator ||
        hooked ||
        tamperedSig ||
        Debug.isDebuggerConnected() ||
        Debug.waitingForDebugger() ||
        (!debugBuild && debugger)
      )

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

  private fun detectHookingFramework(context: Context, findings: MutableList<String>): Boolean {
    var detected = false
    val hookClasses = listOf(
      "de.robv.android.xposed.XposedBridge",
      "de.robv.android.xposed.XC_MethodHook",
      "com.saurik.substrate.MS"
    )
    for (cls in hookClasses) {
      val loaded = runCatching {
        Class.forName(cls, false, context.classLoader)
        true
      }.getOrDefault(false)
      if (loaded) {
        findings.add("Runtime hooking framework detected ($cls)")
        detected = true
        break
      }
    }
    for (path in HOOKING_ARTIFACT_PATHS) {
      if (runCatching { File(path).exists() }.getOrDefault(false)) {
        findings.add("Instrumentation artifact detected ($path)")
        detected = true
        break
      }
    }
    return detected
  }

  @Suppress("DEPRECATION")
  private fun detectRepackagedSignature(context: Context, findings: MutableList<String>): Boolean {
    return runCatching {
      val pm = context.packageManager
      val pkgName = context.packageName
      val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo ?: return@runCatching false
        if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners else signingInfo.signingCertificateHistory
      } else {
        pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures
      }
      if (certs == null || certs.isEmpty()) return@runCatching false
      val sha256List = certs.map { sig ->
        MessageDigest.getInstance("SHA-256")
          .digest(sig.toByteArray())
          .joinToString("") { "%02X".format(it) }
      }
      val recognized = sha256List.any { it in TRUSTED_SIGNING_SHA256 }
      if (!recognized && !BuildConfig.DEBUG) {
        findings.add("Unofficial or repackaged APK signing certificate")
        true
      } else {
        false
      }
    }.getOrDefault(false)
  }
}
