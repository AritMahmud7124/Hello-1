package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthSignInResult
import com.example.ui.components.HelloBrandLogo
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  authRepository: AuthRepository,
  onAuthenticated: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    authRepository.attemptAutoSignIn(context)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
          )
        )
      )
      .safeDrawingPadding()
      .padding(20.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(210.dp)
          .clip(RoundedCornerShape(28.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_auth_hero),
          contentDescription = "HELLO social community artwork",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
              )
            )
        )
        Row(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          HelloBrandLogo(size = 48.dp)
          Column {
            Text(
              text = "HELLO",
              style = MaterialTheme.typography.headlineMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Say Hello to your real circle.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.88f)
            )
          }
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            "One social space for everything that matters",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          AuthFeatureRow(Icons.Filled.PlayCircle, "Feed, Stories & Reels", "Share moments, react, comment, and explore short videos.")
          AuthFeatureRow(Icons.Filled.Groups, "Messenger, Groups & SOS", "Real-time chats plus consent-first Emergency SOS.")
          AuthFeatureRow(Icons.Filled.Favorite, "Secret Love & Marketplace", "Anonymous love notes with strict privacy and local listings.")
          AuthFeatureRow(Icons.Filled.Lock, "Protected by Google Sign-In", "Verified accounts with zero-trust Firestore rules.")
        }
      }

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      Button(
        onClick = {
          val activity = context as? Activity
          if (activity == null) {
            errorMessage = "Activity context unavailable for Google Sign-In."
            return@Button
          }
          isLoading = true
          errorMessage = null
          scope.launch {
            when (val res = authRepository.signInWithGoogle(activity)) {
              is AuthSignInResult.Success -> {
                isLoading = false
                onAuthenticated()
              }
              is AuthSignInResult.Cancelled -> {
                isLoading = false
              }
              is AuthSignInResult.Error -> {
                isLoading = false
                errorMessage = res.message
              }
            }
          }
        },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
          )
        } else {
          Text(
            text = "Sign in with Google",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun AuthFeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(22.dp)
    )
    Column {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
