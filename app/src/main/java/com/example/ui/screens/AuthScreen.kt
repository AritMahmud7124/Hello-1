package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSignInResult
import com.example.security.AuthRateLimiter
import com.example.security.DeviceIntegrityChecker
import com.example.security.EncryptedVaultStorage
import com.example.security.RateLimitState
import com.example.ui.components.HELLO_WEBSITE_URL
import com.example.ui.components.HelloBrandLogo
import com.example.ui.components.openExternalUrl
import com.example.ui.theme.HelloAmber
import com.example.ui.theme.HelloCoral
import com.example.ui.theme.HelloTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  authRepository: AuthRepository,
  onAuthenticated: () -> Unit,
  vaultStorage: EncryptedVaultStorage? = null,
  signInScope: CoroutineScope? = null
) {
  val context = LocalContext.current
  val localScope = rememberCoroutineScope()
  val effectiveScope = remember(signInScope, context, localScope) {
    signInScope ?: (context as? LifecycleOwner)?.lifecycleScope ?: localScope
  }
  val vault = remember(context, vaultStorage) { vaultStorage ?: EncryptedVaultStorage(context) }
  val rateLimiter = remember(vault) { AuthRateLimiter(vault) }
  val integrityReport = remember(context) {
    DeviceIntegrityChecker.assess(context, vault.isStrictTamperModeEnabled)
  }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var rateLimitState by remember { mutableStateOf(rateLimiter.getState()) }

  LaunchedEffect(rateLimitState.isLockedOut) {
    while (rateLimiter.getState().isLockedOut) {
      rateLimitState = rateLimiter.getState()
      delay(1000L)
    }
    rateLimitState = rateLimiter.getState()
  }

  LaunchedEffect(Unit) {
    if (!rateLimitState.isLockedOut) {
      effectiveScope.launch {
        val autoSuccess = authRepository.attemptAutoSignIn(context)
        if (autoSuccess) {
          authRepository.refreshAndVerifyIdToken(vault)
          authRepository.syncSecurityState(
            failedAttempts = 0,
            lockoutUntilMs = 0L,
            lastAttemptMs = System.currentTimeMillis(),
            biometricEnabled = vault.isBiometricLockEnabled,
            integrityReport = integrityReport
          )
          onAuthenticated()
        }
      }
    }
  }

  val launchGoogleSignIn: () -> Unit = {
    val currentLimit = rateLimiter.getState()
    rateLimitState = currentLimit
    if (currentLimit.isLockedOut) {
      errorMessage = "Security cooldown active. Retry in ${currentLimit.remainingSeconds}s."
    } else {
      val activity = context as? Activity
      if (activity == null) {
        errorMessage = "Activity context unavailable for Google Sign-In."
      } else if (!isLoading) {
        isLoading = true
        errorMessage = null
        effectiveScope.launch {
          when (val res = authRepository.signInWithGoogle(activity)) {
            is AuthSignInResult.Success -> {
              rateLimiter.recordSuccess()
              rateLimitState = rateLimiter.getState()
              authRepository.refreshAndVerifyIdToken(vault)
              val remote = authRepository.fetchBackendLockout()
              if (remote != null && remote.second > System.currentTimeMillis()) {
                rateLimitState = rateLimiter.syncRemoteLockout(remote.first, remote.second)
                authRepository.logSecurityEvent(
                  eventType = "rate_limit_triggered",
                  detail = "Backend lockout enforced on login",
                  integritySummary = integrityReport.summary
                )
                authRepository.signOut(context, vault)
                isLoading = false
                errorMessage = "Backend cooldown active. Retry in ${rateLimitState.remainingSeconds}s."
              } else {
                authRepository.syncSecurityState(
                  failedAttempts = 0,
                  lockoutUntilMs = 0L,
                  lastAttemptMs = System.currentTimeMillis(),
                  biometricEnabled = vault.isBiometricLockEnabled,
                  integrityReport = integrityReport
                )
                authRepository.logSecurityEvent(
                  eventType = "sign_in_verified",
                  detail = "Google Credential Manager + ID Token verified",
                  integritySummary = integrityReport.summary
                )
                isLoading = false
                onAuthenticated()
              }
            }
            is AuthSignInResult.Cancelled -> {
              isLoading = false
            }
            is AuthSignInResult.Error -> {
              isLoading = false
              rateLimitState = rateLimiter.recordFailure()
              errorMessage = if (rateLimitState.isLockedOut) {
                "Repeated failed sign-ins. Locked for ${rateLimitState.remainingSeconds}s."
              } else {
                res.message
              }
            }
          }
        }
      }
    }
  }

  AuthScreenContent(
    isLoading = isLoading,
    errorMessage = errorMessage,
    onDismissError = { errorMessage = null },
    onGoogleSignInClick = launchGoogleSignIn,
    rateLimitState = rateLimitState
  )
}

@Composable
internal fun AuthScreenContent(
  isLoading: Boolean,
  errorMessage: String?,
  onDismissError: () -> Unit,
  onGoogleSignInClick: () -> Unit,
  rateLimitState: RateLimitState? = null
) {
  val context = LocalContext.current
  var activeLegalSheet by remember { mutableStateOf<AuthLegalType?>(null) }
  val isLockedOut = rateLimitState?.isLockedOut == true

  val subtleBackground = remember {
    Brush.verticalGradient(
      colors = listOf(
        Color(0xFF07090F),
        Color(0xFF0C101B),
        Color(0xFF07090F)
      )
    )
  }

  HelloTheme(darkTheme = true) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(subtleBackground)
        .safeDrawingPadding()
        .padding(horizontal = 28.dp, vertical = 20.dp)
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 420.dp)
          .fillMaxWidth()
          .align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp)
      ) {
        // Minimalist Brand & Tagline
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF131826),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
          ) {
            Box(modifier = Modifier.padding(14.dp)) {
              HelloBrandLogo(size = 56.dp)
            }
          }

          Text(
            text = "HELLO",
            style = MaterialTheme.typography.headlineMedium,
            color = AuthTextPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
          )

          Text(
            text = "Connect deeper. Stay protected.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
          )
        }

        // Security Cooldown or Auth Error (only visible when active)
        AnimatedVisibility(
          visible = isLockedOut || errorMessage != null,
          enter = fadeIn() + slideInVertically { it / 4 },
          exit = fadeOut()
        ) {
          if (isLockedOut) {
            Surface(
              color = Color(0xFF2B180B),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, HelloAmber.copy(alpha = 0.5f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("rate_limit_banner")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Timer,
                  contentDescription = "Cooldown timer",
                  tint = HelloAmber,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Attempt #${rateLimitState?.failedAttempts ?: 0} • Retry in ${rateLimitState?.remainingSeconds ?: 0}s",
                  style = MaterialTheme.typography.bodySmall,
                  color = AuthTextPrimary
                )
              }
            }
          } else if (errorMessage != null) {
            Surface(
              color = Color(0xFF331118),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, HelloCoral.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  text = errorMessage,
                  color = Color(0xFFFFD9DA),
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = onDismissError,
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss error",
                    tint = Color(0xFFFFD9DA),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }

        MinimalistGoogleSignInButton(
          isLoading = isLoading,
          enabled = !isLockedOut,
          onClick = onGoogleSignInClick
        )

        TextButton(
          onClick = { openExternalUrl(context, HELLO_WEBSITE_URL) },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("auth_open_website_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.LaptopMac,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = "Open HELLO Website & Laptop App",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFCBD5E1)
          )
        }
      }

      // Muted Legal Footer Anchored Cleanly at Bottom
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { activeLegalSheet = AuthLegalType.TERMS }
            .minimumInteractiveComponentSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("terms_of_service_link"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Terms of Service",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            textDecoration = TextDecoration.Underline
          )
        }
        Text(
          text = "·",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF475569)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { activeLegalSheet = AuthLegalType.PRIVACY }
            .minimumInteractiveComponentSize()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("privacy_policy_link"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
            textDecoration = TextDecoration.Underline
          )
        }
      }
    }

    activeLegalSheet?.let { legalType ->
      AuthLegalSheet(
        legalType = legalType,
        onDismiss = { activeLegalSheet = null }
      )
    }
  }
}

@Composable
private fun MinimalistGoogleSignInButton(
  isLoading: Boolean,
  enabled: Boolean,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && !isLoading && enabled) 0.98f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "minimal_google_btn_scale"
  )

  Button(
    onClick = onClick,
    enabled = enabled && !isLoading,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(14.dp),
    elevation = ButtonDefaults.buttonElevation(
      defaultElevation = 4.dp,
      pressedElevation = 1.dp,
      disabledElevation = 0.dp
    ),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color(0xFFFFFFFF),
      contentColor = Color(0xFF0F172A),
      disabledContainerColor = Color(0xFFE2E8F0),
      disabledContentColor = Color(0xFF475569)
    ),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp)
      .heightIn(min = 54.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .minimumInteractiveComponentSize()
      .testTag("google_sign_in_button")
  ) {
    AnimatedContent(
      targetState = isLoading,
      transitionSpec = {
        fadeIn(tween(150)) togetherWith fadeOut(tween(120))
      },
      label = "minimal_google_btn_content"
    ) { loading ->
      if (loading) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = Color(0xFF0F172A),
            strokeWidth = 2.dp
          )
          Text(
            text = "Signing in...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          GoogleBrandMark()
          Text(
            text = "Continue with Google",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
