package com.example.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
  AVAILABLE,
  NONE_ENROLLED,
  UNAVAILABLE
}

object BiometricAuthHelper {

  private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.BIOMETRIC_WEAK or
      BiometricManager.Authenticators.DEVICE_CREDENTIAL

  fun checkAvailability(context: Context): BiometricAvailability {
    val manager = BiometricManager.from(context)
    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      manager.canAuthenticate(ALLOWED_AUTHENTICATORS)
    } else {
      @Suppress("DEPRECATION")
      manager.canAuthenticate()
    }
    return when (result) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
      else -> BiometricAvailability.UNAVAILABLE
    }
  }

  fun authenticate(
    activity: FragmentActivity,
    title: String = "Verify Identity",
    subtitle: String = "Unlock HELLO with Fingerprint, Face, or Passkey",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onCancelled: () -> Unit = {}
  ) {
    val availability = checkAvailability(activity)
    if (availability != BiometricAvailability.AVAILABLE) {
      onError(
        if (availability == BiometricAvailability.NONE_ENROLLED) {
          "No biometric or device screen lock is enrolled on this device."
        } else {
          "Biometric authentication hardware is unavailable on this device."
        }
      )
      return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        if (
          errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
          errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
          errorCode == BiometricPrompt.ERROR_CANCELED
        ) {
          onCancelled()
        } else {
          onError(errString.toString().ifBlank { "Biometric verification failed." })
        }
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
      }
    }

    val prompt = BiometricPrompt(activity, executor, callback)
    val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setConfirmationRequired(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      promptInfoBuilder.setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
    } else {
      promptInfoBuilder.setNegativeButtonText("Cancel")
    }

    prompt.authenticate(promptInfoBuilder.build())
  }
}

tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
  is FragmentActivity -> this
  is android.content.ContextWrapper -> baseContext.findFragmentActivity()
  else -> null
}
