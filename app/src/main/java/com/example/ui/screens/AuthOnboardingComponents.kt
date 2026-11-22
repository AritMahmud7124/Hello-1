package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.R
import com.example.ui.theme.HelloAmber
import com.example.ui.theme.HelloCoral
import com.example.ui.theme.HelloCyan
import com.example.ui.theme.HelloEmerald
import com.example.ui.theme.HelloRose
import com.example.ui.theme.HelloViolet
import com.example.ui.theme.HelloVioletLight
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val AuthObsidianBg = Color(0xFF060916)
internal val AuthGlassFill = Color(0xFF131936).copy(alpha = 0.68f)
internal val AuthGlassElevated = Color(0xFF1A2247).copy(alpha = 0.78f)
internal val AuthTextPrimary = Color(0xFFF8FAFC)
internal val AuthTextSecondary = Color(0xFFA5B4CF)

@Immutable
internal data class OnboardingFeatureSlide(
  val id: String,
  val badge: String,
  val title: String,
  val description: String,
  val primaryGlow: Color,
  val secondaryGlow: Color,
  val icon: ImageVector? = null,
  @param:DrawableRes val iconDrawable: Int? = null,
  val highlights: List<String>
)

internal enum class AuthLegalType {
  TERMS,
  PRIVACY
}

internal fun defaultOnboardingSlides(): List<OnboardingFeatureSlide> = listOf(
  OnboardingFeatureSlide(
    id = "feed",
    badge = "SOCIAL PULSE",
    title = "Feed, Stories & Reels",
    description = "Share vivid moments, follow creators in Reels, and react instantly with your circle.",
    primaryGlow = HelloCoral,
    secondaryGlow = HelloAmber,
    icon = Icons.Rounded.PlayCircle,
    highlights = listOf("HD Reels", "24h Stories", "Live Reactions")
  ),
  OnboardingFeatureSlide(
    id = "messenger",
    badge = "REAL-TIME CHAT",
    title = "Smart Messenger",
    description = "Direct and group conversations with voice notes, live typing status, and instant alerts.",
    primaryGlow = HelloCyan,
    secondaryGlow = HelloVioletLight,
    iconDrawable = R.drawable.ic_messenger_smart,
    highlights = listOf("Voice Notes", "Private Groups", "Read Receipts")
  ),
  OnboardingFeatureSlide(
    id = "secret_love",
    badge = "100% ANONYMOUS",
    title = "Secret Love",
    description = "Send heartfelt crush notes with cryptographically hidden sender identity and safety blocks.",
    primaryGlow = HelloRose,
    secondaryGlow = HelloCoral,
    icon = Icons.Rounded.Favorite,
    highlights = listOf("Hidden Sender", "Safe Reveal", "Zero Leaks")
  ),
  OnboardingFeatureSlide(
    id = "sos",
    badge = "TRUSTED CIRCLE",
    title = "Emergency SOS",
    description = "One-tap safety alerts prioritized for your closest friends and private groups.",
    primaryGlow = HelloEmerald,
    secondaryGlow = HelloCyan,
    icon = Icons.Rounded.HealthAndSafety,
    highlights = listOf("Friend Priority", "Consent-First", "Fast Alert")
  )
)

@Composable
internal fun FuturisticAuthBackground(
  modifier: Modifier = Modifier,
  animateGlows: Boolean = true
) {
  val orbitPhase: Float
  val pulseAlpha: Float
  if (animateGlows) {
    val transition = rememberInfiniteTransition(label = "auth_ambient_glow")
    orbitPhase = transition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 14000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "orbit_phase"
    ).value
    pulseAlpha = transition.animateFloat(
      initialValue = 0.24f,
      targetValue = 0.42f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "pulse_alpha"
    ).value
  } else {
    orbitPhase = 0f
    pulseAlpha = 0.32f
  }

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF050713),
            Color(0xFF0A0F24),
            Color(0xFF070A1A)
          )
        )
      )
  ) {
    val w = size.width
    val h = size.height

    val topGlowCenter = Offset(
      x = w * (0.25f + 0.14f * cos(orbitPhase)),
      y = h * (0.14f + 0.05f * sin(orbitPhase))
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          HelloCoral.copy(alpha = pulseAlpha),
          HelloRose.copy(alpha = pulseAlpha * 0.45f),
          Color.Transparent
        ),
        center = topGlowCenter,
        radius = w * 0.72f
      ),
      center = topGlowCenter,
      radius = w * 0.72f
    )

    val midGlowCenter = Offset(
      x = w * (0.82f - 0.15f * sin(orbitPhase)),
      y = h * (0.48f + 0.07f * cos(orbitPhase))
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          HelloViolet.copy(alpha = pulseAlpha * 0.9f),
          HelloCyan.copy(alpha = pulseAlpha * 0.35f),
          Color.Transparent
        ),
        center = midGlowCenter,
        radius = w * 0.78f
      ),
      center = midGlowCenter,
      radius = w * 0.78f
    )

    val bottomGlowCenter = Offset(
      x = w * (0.35f + 0.12f * sin(orbitPhase * 0.7f)),
      y = h * 0.90f
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          HelloCyan.copy(alpha = 0.16f),
          Color.Transparent
        ),
        center = bottomGlowCenter,
        radius = w * 0.65f
      ),
      center = bottomGlowCenter,
      radius = w * 0.65f
    )

    val step = 48.dp.toPx()
    var gx = step * 0.5f
    while (gx < w) {
      var gy = step * 0.5f
      while (gy < h) {
        drawCircle(
          color = Color.White.copy(alpha = 0.035f),
          radius = 1.dp.toPx(),
          center = Offset(gx, gy)
        )
        gy += step
      }
      gx += step
    }
  }
}

@Composable
internal fun OnboardingFeatureCarousel(
  slides: List<OnboardingFeatureSlide>,
  modifier: Modifier = Modifier,
  autoAdvance: Boolean = true,
  animateGlows: Boolean = true
) {
  val pagerState = rememberPagerState(pageCount = { slides.size })
  val scope = rememberCoroutineScope()

  LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, autoAdvance) {
    if (autoAdvance && !pagerState.isScrollInProgress && slides.size > 1) {
      delay(4400)
      val next = (pagerState.currentPage + 1) % slides.size
      pagerState.animateScrollToPage(
        page = next,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
      )
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    HorizontalPager(
      state = pagerState,
      pageSpacing = 12.dp,
      contentPadding = PaddingValues(horizontal = 2.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("onboarding_carousel")
    ) { pageIndex ->
      val slide = slides[pageIndex]
      val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
        .absoluteValue
        .coerceIn(0f, 1f)

      OnboardingSlideCard(
        slide = slide,
        isCurrent = pagerState.currentPage == pageIndex,
        animateGlows = animateGlows,
        modifier = Modifier
          .fillMaxWidth()
          .graphicsLayer {
            val scale = lerp(1f, 0.94f, pageOffset)
            scaleX = scale
            scaleY = scale
            alpha = lerp(1f, 0.68f, pageOffset)
          }
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      slides.forEachIndexed { index, slide ->
        val selected = pagerState.currentPage == index
        val indicatorWidth by animateDpAsState(
          targetValue = if (selected) 28.dp else 8.dp,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          label = "indicator_width"
        )
        val indicatorColor by animateColorAsState(
          targetValue = if (selected) slide.primaryGlow else Color.White.copy(alpha = 0.24f),
          animationSpec = tween(260),
          label = "indicator_color"
        )
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .clickable {
              scope.launch { pagerState.animateScrollToPage(index) }
            }
            .padding(vertical = 6.dp, horizontal = 2.dp)
        ) {
          Box(
            modifier = Modifier
              .width(indicatorWidth)
              .height(7.dp)
              .clip(CircleShape)
              .background(indicatorColor)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingSlideCard(
  slide: OnboardingFeatureSlide,
  isCurrent: Boolean,
  animateGlows: Boolean,
  modifier: Modifier = Modifier
) {
  val haloScale = if (animateGlows) {
    val haloTransition = rememberInfiniteTransition(label = "icon_halo_${slide.id}")
    haloTransition.animateFloat(
      initialValue = 0.96f,
      targetValue = 1.14f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "halo_scale"
    ).value
  } else {
    1.04f
  }

  val borderBrush = remember(slide.primaryGlow, slide.secondaryGlow) {
    Brush.linearGradient(
      colors = listOf(
        slide.primaryGlow.copy(alpha = 0.55f),
        Color.White.copy(alpha = 0.14f),
        slide.secondaryGlow.copy(alpha = 0.42f)
      )
    )
  }
  val cardBackdrop = remember(slide.primaryGlow, slide.secondaryGlow) {
    Brush.linearGradient(
      colors = listOf(
        Color(0xFF161D3D).copy(alpha = 0.84f),
        Color(0xFF10152E).copy(alpha = 0.78f),
        slide.primaryGlow.copy(alpha = 0.12f)
      )
    )
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(26.dp))
      .background(cardBackdrop)
      .border(1.dp, borderBrush, RoundedCornerShape(26.dp))
      .padding(20.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = slide.primaryGlow.copy(alpha = 0.16f),
          shape = CircleShape,
          border = BorderStroke(1.dp, slide.primaryGlow.copy(alpha = 0.4f))
        ) {
          Text(
            text = slide.badge,
            style = MaterialTheme.typography.labelSmall,
            color = slide.primaryGlow,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }

        Box(
          modifier = Modifier.size(58.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .graphicsLayer {
                val activeScale = if (isCurrent) haloScale else 1f
                scaleX = activeScale
                scaleY = activeScale
              }
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  listOf(
                    slide.primaryGlow.copy(alpha = 0.36f),
                    slide.secondaryGlow.copy(alpha = 0.08f),
                    Color.Transparent
                  )
                )
              )
          )
          Surface(
            modifier = Modifier.size(46.dp),
            shape = CircleShape,
            color = Color(0xFF1B234A),
            border = BorderStroke(1.dp, slide.primaryGlow.copy(alpha = 0.55f))
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (slide.iconDrawable != null) {
                Icon(
                  painter = painterResource(id = slide.iconDrawable),
                  contentDescription = slide.title,
                  tint = slide.primaryGlow,
                  modifier = Modifier.size(24.dp)
                )
              } else if (slide.icon != null) {
                Icon(
                  imageVector = slide.icon,
                  contentDescription = slide.title,
                  tint = slide.primaryGlow,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = slide.title,
          style = MaterialTheme.typography.titleLarge,
          color = AuthTextPrimary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = slide.description,
          style = MaterialTheme.typography.bodyMedium,
          color = AuthTextSecondary,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        slide.highlights.forEach { tag ->
          Surface(
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
          ) {
            Text(
              text = tag,
              style = MaterialTheme.typography.labelMedium,
              color = AuthTextPrimary.copy(alpha = 0.92f),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun GlassTrustHighlightsCard(modifier: Modifier = Modifier) {
  val borderBrush = remember {
    Brush.linearGradient(
      listOf(
        Color.White.copy(alpha = 0.22f),
        Color.White.copy(alpha = 0.06f),
        HelloVioletLight.copy(alpha = 0.20f)
      )
    )
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(AuthGlassFill)
      .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlassMiniStat(
      icon = Icons.Rounded.VerifiedUser,
      tint = HelloCyan,
      title = "SSL Pinning",
      subtitle = "TLS 1.3 + CT",
      modifier = Modifier.weight(1f)
    )
    Box(
      modifier = Modifier
        .width(1.dp)
        .height(34.dp)
        .background(Color.White.copy(alpha = 0.12f))
    )
    GlassMiniStat(
      icon = Icons.Rounded.Lock,
      tint = HelloCoral,
      title = "Keystore Vault",
      subtitle = "AES-256 GCM",
      modifier = Modifier.weight(1f)
    )
    Box(
      modifier = Modifier
        .width(1.dp)
        .height(34.dp)
        .background(Color.White.copy(alpha = 0.12f))
    )
    GlassMiniStat(
      icon = Icons.Rounded.Security,
      tint = HelloEmerald,
      title = "Biometric 2FA",
      subtitle = "Anti-tamper",
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun GlassMiniStat(
  icon: ImageVector,
  tint: Color,
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = tint,
      modifier = Modifier.size(18.dp)
    )
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      color = AuthTextPrimary,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.labelSmall,
      color = AuthTextSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
internal fun GoogleBrandMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(22.dp)) {
    val stroke = size.minDimension * 0.21f
    val inset = stroke * 0.55f
    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
    val topLeft = Offset(inset, inset)

    // Red top-left arc
    drawArc(
      color = Color(0xFFEA4335),
      startAngle = -148f,
      sweepAngle = 102f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    // Yellow left-bottom arc
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 142f,
      sweepAngle = 72f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    // Green bottom arc
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 42f,
      sweepAngle = 104f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    // Blue right arc
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -12f,
      sweepAngle = 56f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt)
    )
    // Blue horizontal bar of 'G'
    val centerY = size.height * 0.5f
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(size.width * 0.52f, centerY),
      end = Offset(size.width - inset * 0.65f, centerY),
      strokeWidth = stroke * 0.92f,
      cap = StrokeCap.Butt
    )
  }
}

@Composable
internal fun PremiumGoogleSignInButton(
  isLoading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed && !isLoading && enabled) 0.97f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "google_btn_scale"
  )

  val outerHalo = remember {
    Brush.horizontalGradient(
      colors = listOf(
        HelloCoral.copy(alpha = 0.72f),
        HelloViolet.copy(alpha = 0.72f),
        HelloCyan.copy(alpha = 0.65f)
      )
    )
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(18.dp))
      .background(outerHalo)
      .padding(1.5.dp)
  ) {
    Button(
      onClick = onClick,
      enabled = enabled && !isLoading,
      interactionSource = interactionSource,
      shape = RoundedCornerShape(16.5.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFF8FAFC),
        contentColor = Color(0xFF0F172A),
        disabledContainerColor = Color(0xFFE2E8F0),
        disabledContentColor = Color(0xFF334155)
      ),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .minimumInteractiveComponentSize()
        .testTag("google_sign_in_button")
    ) {
      AnimatedContent(
        targetState = isLoading,
        transitionSpec = {
          fadeIn(tween(180)) togetherWith fadeOut(tween(140))
        },
        label = "sign_in_btn_content"
      ) { loading ->
        if (loading) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = HelloViolet,
              strokeWidth = 2.4.dp
            )
            Text(
              text = "Signing you in securely...",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            GoogleBrandMark()
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = Color(0xFF334155)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthLearnMoreSheet(
  slides: List<OnboardingFeatureSlide>,
  onDismiss: () -> Unit,
  onContinueWithGoogle: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF10152B),
    contentColor = AuthTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Inside HELLO",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AuthTextPrimary
          )
          Text(
            text = "Built for genuine friends, privacy, and rapid safety.",
            style = MaterialTheme.typography.bodySmall,
            color = AuthTextSecondary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close tour", tint = AuthTextSecondary)
        }
      }

      slides.forEach { slide ->
        Surface(
          color = AuthGlassElevated,
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.dp, slide.primaryGlow.copy(alpha = 0.28f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Surface(
              modifier = Modifier.size(40.dp),
              shape = CircleShape,
              color = slide.primaryGlow.copy(alpha = 0.16f)
            ) {
              Box(contentAlignment = Alignment.Center) {
                if (slide.iconDrawable != null) {
                  Icon(
                    painter = painterResource(id = slide.iconDrawable),
                    contentDescription = slide.title,
                    tint = slide.primaryGlow,
                    modifier = Modifier.size(20.dp)
                  )
                } else if (slide.icon != null) {
                  Icon(
                    imageVector = slide.icon,
                    contentDescription = slide.title,
                    tint = slide.primaryGlow,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = slide.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AuthTextPrimary
              )
              Text(
                text = slide.description,
                style = MaterialTheme.typography.bodySmall,
                color = AuthTextSecondary
              )
            }
          }
        }
      }

      Surface(
        color = Color(0xFF0B1124),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Why Google Sign-In?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AuthTextPrimary
          )
          Text(
            text = "Every account is authenticated via Credential Manager and protected by zero-trust database rules so bots, fake profiles, and unauthorized reads are blocked.",
            style = MaterialTheme.typography.bodySmall,
            color = AuthTextSecondary
          )
        }
      }

      Button(
        onClick = {
          onDismiss()
          onContinueWithGoogle()
        },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = HelloCoral,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 52.dp)
      ) {
        Text(
          text = "Continue with Google",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(Modifier.height(8.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthLegalSheet(
  legalType: AuthLegalType,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isTerms = legalType == AuthLegalType.TERMS
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF10152B),
    contentColor = AuthTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isTerms) "Terms of Service" else "Privacy Policy",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = AuthTextPrimary
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = AuthTextSecondary)
        }
      }
      HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
      val sections = if (isTerms) {
        listOf(
          "1. Real Identity & Community Respect" to "HELLO uses Google Sign-In to maintain authentic circles. Harassment, impersonation, or non-consensual sharing in Private Groups is strictly prohibited.",
          "2. Private Groups & Anti-Capture" to "Private groups disable external sharing and block screenshots. Only group admins may invite or remove members.",
          "3. Emergency SOS Responsibility" to "SOS alerts notify your chosen trusted friends and circles. Use Emergency SOS responsibly for genuine assistance.",
          "4. Secret Love Safety" to "Anonymous crush notes must stay respectful. Recipients can block or report abusive notes immediately."
        )
      } else {
        listOf(
          "1. Google Authentication Data" to "We store only your public profile essentials (display name, handle, avatar) and private notification preferences tied to your verified account.",
          "2. Isolated Secret Love Identity" to "Sender UIDs on Secret Love messages are never exposed to recipients in client queries or inboxes.",
          "3. Granular Notification Controls" to "You control likes, comments, friend requests, Messenger, Secret Love, and SOS alert channels anytime in Settings.",
          "4. Account Control" to "You can sign out, update visibility, or manage blocked users directly from your HELLO account settings."
        )
      }
      sections.forEach { (heading, body) ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AuthTextPrimary
          )
          Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = AuthTextSecondary
          )
        }
      }
      Spacer(Modifier.height(12.dp))
    }
  }
}
