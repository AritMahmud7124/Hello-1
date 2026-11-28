package com.example.voicegesture

import com.example.ui.navigation.HelloRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceGestureCommandTest {

  @Test
  fun parseVoiceCommand_navigatesPrimaryAndSecondaryRoutes() {
    assertEquals(
      VoiceGestureAction.Navigate(HelloRoutes.HOME, "Home"),
      VoiceGestureEngine.parseVoiceCommand("open home")
    )
    assertEquals(
      VoiceGestureAction.Navigate(HelloRoutes.REELS, "Reels"),
      VoiceGestureEngine.parseVoiceCommand("go to reels")
    )
    assertEquals(
      VoiceGestureAction.Navigate(HelloRoutes.MARKETPLACE, "Marketplace"),
      VoiceGestureEngine.parseVoiceCommand("marketplace")
    )
    assertEquals(
      VoiceGestureAction.Navigate(HelloRoutes.PROFILE, "Profile"),
      VoiceGestureEngine.parseVoiceCommand("my profile")
    )
    assertEquals(
      VoiceGestureAction.Navigate(HelloRoutes.SETTINGS, "Settings"),
      VoiceGestureEngine.parseVoiceCommand("settings")
    )
  }

  @Test
  fun parseVoiceCommand_mediaScrollBackLikeAndDisable() {
    assertEquals(VoiceGestureAction.MediaNext, VoiceGestureEngine.parseVoiceCommand("next reel"))
    assertEquals(VoiceGestureAction.MediaPrevious, VoiceGestureEngine.parseVoiceCommand("previous"))
    assertEquals(VoiceGestureAction.MediaPlayPause, VoiceGestureEngine.parseVoiceCommand("pause"))
    assertEquals(VoiceGestureAction.Back, VoiceGestureEngine.parseVoiceCommand("go back"))
    assertEquals(VoiceGestureAction.Like, VoiceGestureEngine.parseVoiceCommand("like"))
    assertEquals(VoiceGestureAction.Disable, VoiceGestureEngine.parseVoiceCommand("turn off voice"))

    val down = VoiceGestureEngine.parseVoiceCommand("scroll down") as VoiceGestureAction.Scroll
    assertEquals(1, down.direction)
    assertTrue(!down.toTop)

    val top = VoiceGestureEngine.parseVoiceCommand("scroll to top") as VoiceGestureAction.Scroll
    assertEquals(-1, top.direction)
    assertTrue(top.toTop)
  }

  @Test
  fun parseVoiceCommand_ignoresEmptyOrUnmatchedSpeech() {
    assertNull(VoiceGestureEngine.parseVoiceCommand(""))
    assertNull(VoiceGestureEngine.parseVoiceCommand("   "))
    assertNull(VoiceGestureEngine.parseVoiceCommand("completely unrelated sentence"))
  }
}
