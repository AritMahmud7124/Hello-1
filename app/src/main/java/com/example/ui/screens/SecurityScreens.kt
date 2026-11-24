package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.security.DeviceIntegrityReport
import com.example.security.RateLimitState
import com.example.ui.theme.HelloCoral
import com.example.ui.theme.HelloCyan
import com.example.ui.theme.HelloEmerald
import com.example.ui.theme.HelloTheme
import com.example.ui.theme.HelloViolet

@Composable
fun TamperBlockedScreen(
  report: DeviceIntegrityReport,
  onRetryCheck: () -> Unit,
  onExitStrictPreview: (() -> Unit)? = null
) {
  HelloTheme(darkTheme = true) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF14070D), Color(0xFF070916))
          )
        )
        .safeDrawingPadding()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 480.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Surface(
          modifier = Modifier.size(84.dp),
          shape = CircleShape,
          color = HelloCoral.copy(alpha = 0.16f),
          border = BorderStroke(1.5.dp, HelloCoral.copy(alpha = 0.65f))
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.GppBad,
              contentDescription = "Security Block",
              tint = HelloCoral,
              modifier = Modifier.size(44.dp)
            )
          }
        }

        Text(
          text = "Security Access Blocked",
          style = MaterialTheme.typography.headlineMedium,
          color = AuthTextPrimary,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )

        Text(
          text = "HELLO Enterprise Security detected an untrusted runtime environment and blocked access to protect your account, keys, and private groups.",
          style = MaterialTheme.typography.bodyMedium,
          color = AuthTextSecondary,
          textAlign = TextAlign.Center
        )

        Surface(
          color = Color(0xFF1A1021).copy(alpha = 0.85f),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, HelloCoral.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Integrity Check Findings",
              style = MaterialTheme.typography.titleSmall,
              color = HelloCoral,
              fontWeight = FontWeight.Bold
            )
            if (report.findings.isEmpty()) {
              Text(
                text = "• Strict hardware integrity check failed.",
                style = MaterialTheme.typography.bodySmall,
                color = AuthTextPrimary
              )
            } else {
              report.findings.forEach { finding ->
                Text(
                  text = "• $finding",
                  style = MaterialTheme.typography.bodySmall,
                  color = AuthTextPrimary
                )
              }
            }
          }
        }

        Button(
          onClick = onRetryCheck,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = HelloCoral, contentColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("tamper_retry_button")
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(8.dp))
          Text("Re-scan Device Integrity", fontWeight = FontWeight.Bold)
        }

        if (onExitStrictPreview != null) {
          OutlinedButton(
            onClick = onExitStrictPreview,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthTextPrimary),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("exit_strict_tamper_simulation")
          ) {
            Text("Exit Debug Strict Simulation", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
fun BiometricVaultLockScreen(
  userDisplayLabel: String,
  integrityReport: DeviceIntegrityReport,
  rateLimitState: RateLimitState,
  errorMessage: String?,
  onTriggerBiometric: () -> Unit,
  onSignOut: () -> Unit
) {
  LaunchedEffect(rateLimitState.isLockedOut) {
    if (!rateLimitState.isLockedOut && errorMessage == null) {
      onTriggerBiometric()
    }
  }

  HelloTheme(darkTheme = true) {
    Box(modifier = Modifier.fillMaxSize()) {
      FuturisticAuthBackground()

      Box(
        modifier = Modifier
          .fillMaxSize()
          .safeDrawingPadding()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AuthGlassFill)
            .border(
              1.dp,
              Brush.linearGradient(listOf(HelloCyan.copy(alpha = 0.45f), HelloViolet.copy(alpha = 0.35f))),
              RoundedCornerShape(28.dp)
            )
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Surface(
            modifier = Modifier.size(76.dp),
            shape = CircleShape,
            color = HelloCyan.copy(alpha = 0.14f),
            border = BorderStroke(1.5.dp, HelloCyan.copy(alpha = 0.5f))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = "Biometric Vault Lock",
                tint = HelloCyan,
                modifier = Modifier.size(40.dp)
              )
            }
          }

          Surface(
            color = HelloEmerald.copy(alpha = 0.15f),
            shape = CircleShape,
            border = BorderStroke(1.dp, HelloEmerald.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(Icons.Rounded.Shield, contentDescription = null, tint = HelloEmerald, modifier = Modifier.size(14.dp))
              Text(
                text = if (integrityReport.isRooted) "RISK DETECTED" else "ENCRYPTED KEYSTORE VAULT",
                style = MaterialTheme.typography.labelSmall,
                color = HelloEmerald,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Text(
            text = "Biometric Vault Locked",
            style = MaterialTheme.typography.headlineSmall,
            color = AuthTextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )

          Text(
            text = "Confirm Fingerprint, Face Unlock, or Passkey to unlock session for $userDisplayLabel.",
            style = MaterialTheme.typography.bodyMedium,
            color = AuthTextSecondary,
            textAlign = TextAlign.Center
          )

          if (rateLimitState.isLockedOut) {
            Surface(
              color = Color(0xFF45121B).copy(alpha = 0.85f),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, HelloCoral.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = HelloCoral)
                Text(
                  text = "Too many failed unlocks. Cooldown active: retry in ${rateLimitState.remainingSeconds}s",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFFFD9DA)
                )
              }
            }
          } else if (errorMessage != null) {
            Surface(
              color = Color(0xFF45121B).copy(alpha = 0.85f),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, HelloCoral.copy(alpha = 0.55f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFD9DA),
                modifier = Modifier.padding(12.dp)
              )
            }
          }

          Button(
            onClick = onTriggerBiometric,
            enabled = !rateLimitState.isLockedOut,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HelloCyan, contentColor = Color(0xFF04101B)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 52.dp)
              .testTag("biometric_unlock_button")
          ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
              text = if (rateLimitState.isLockedOut) "Locked (${rateLimitState.remainingSeconds}s)" else "Unlock with Biometrics / Passkey",
              fontWeight = FontWeight.Bold
            )
          }

          OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuthTextPrimary),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("biometric_sign_out_button")
          ) {
            Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Sign Out & Switch Account", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}
