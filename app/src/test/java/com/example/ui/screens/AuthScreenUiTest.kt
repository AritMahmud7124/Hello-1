package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthScreenUiTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun authScreen_rendersMinimalistBrandingSignInAndLoadingState() {
    var isLoading by mutableStateOf(false)
    var clicked = false

    composeRule.setContent {
      AuthScreenContent(
        isLoading = isLoading,
        errorMessage = null,
        onDismissError = {},
        onGoogleSignInClick = {
          clicked = true
          isLoading = true
        }
      )
    }

    composeRule.onNodeWithText("HELLO").assertIsDisplayed()
    composeRule.onNodeWithText("Connect deeper. Stay protected.").assertIsDisplayed()
    composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()

    composeRule.onNodeWithTag("google_sign_in_button").performClick()
    assertTrue(clicked)
    composeRule.onNodeWithText("Signing in...").assertIsDisplayed()
  }

  @Test
  fun authScreen_opensTermsAndPrivacyBottomSheets() {
    composeRule.setContent {
      AuthScreenContent(
        isLoading = false,
        errorMessage = null,
        onDismissError = {},
        onGoogleSignInClick = {}
      )
    }

    composeRule.onNodeWithTag("terms_of_service_link").performClick()
    composeRule.onNodeWithText("1. Real Identity & Community Respect").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithContentDescription("Close").performClick()

    composeRule.onNodeWithTag("privacy_policy_link").performClick()
    composeRule.onNodeWithText("2. Isolated Secret Love Identity").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun authScreen_displaysRateLimitBannerWhenLockedOut() {
    composeRule.setContent {
      AuthScreenContent(
        isLoading = false,
        errorMessage = null,
        onDismissError = {},
        onGoogleSignInClick = {},
        rateLimitState = com.example.security.RateLimitState(
          failedAttempts = 4,
          lockoutUntilEpochMs = System.currentTimeMillis() + 30_000L,
          remainingSeconds = 30L,
          isLockedOut = true
        )
      )
    }

    composeRule.onNodeWithTag("rate_limit_banner").assertIsDisplayed()
    composeRule.onNodeWithText("Attempt #4 • Retry in 30s").assertIsDisplayed()
  }
}
