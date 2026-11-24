package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemeMode
import com.example.data.repository.AuthRepository
import com.example.data.repository.CommunityRepository
import com.example.data.repository.GovernanceRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.SpacesRepository
import com.example.notifications.HelloFirebaseMessagingService
import com.example.security.AuthRateLimiter
import com.example.security.BiometricAuthHelper
import com.example.security.DeviceIntegrityChecker
import com.example.security.DeviceIntegrityReport
import com.example.security.EncryptedVaultStorage
import com.example.security.findFragmentActivity
import com.example.ui.components.HelloBrandLogo
import com.example.ui.components.MessengerCallOverlay
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.navigation.HelloRoutes
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricVaultLockScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GroupDetailScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HubMenuScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MessengerScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.OwnerProfileScreen
import com.example.ui.screens.PagesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SecretLoveScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SosHubScreen
import com.example.ui.screens.StoriesScreen
import com.example.ui.screens.StoryViewerDialog
import com.example.ui.screens.TamperBlockedScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.HelloTheme
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
  private var pendingNotificationRoute by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    pendingNotificationRoute = intent?.getStringExtra(HelloFirebaseMessagingService.EXTRA_ROUTE)
    setContent {
      HelloRootApp(
        pendingRoute = pendingNotificationRoute,
        onPendingRouteConsumed = { pendingNotificationRoute = null }
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    pendingNotificationRoute = intent.getStringExtra(HelloFirebaseMessagingService.EXTRA_ROUTE)
  }
}

private data class PrimaryNavItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val tag: String,
  val isEmergency: Boolean = false
)

@Composable
fun HelloRootApp(
  pendingRoute: String? = null,
  onPendingRouteConsumed: () -> Unit = {}
) {
  val context = LocalContext.current
  val appPrefs = remember(context) { AppPreferences(context.applicationContext) }
  val themeMode by appPrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
  val language by appPrefs.language.collectAsStateWithLifecycle(initialValue = com.example.data.preferences.AppLanguage.EN)
  val scope = rememberCoroutineScope()

  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val vault = remember(context) { EncryptedVaultStorage(context) }
  val rateLimiter = remember(vault) { AuthRateLimiter(vault) }
  var strictDebugMode by remember(vault) { mutableStateOf(vault.isStrictTamperModeEnabled) }
  var integrityScanNonce by remember { mutableIntStateOf(0) }
  val integrityReport = remember(strictDebugMode, integrityScanNonce) {
    DeviceIntegrityChecker.assess(context, strictDebugMode)
  }

  if (integrityReport.isBlocked) {
    TamperBlockedScreen(
      report = integrityReport,
      onRetryCheck = { integrityScanNonce++ },
      onExitStrictPreview = if (BuildConfig.DEBUG && strictDebugMode) {
        {
          vault.isStrictTamperModeEnabled = false
          strictDebugMode = false
        }
      } else null
    )
    return
  }

  val db = remember(context) {
    FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
  }
  val authRepository = remember(db) { AuthRepository(db) }
  val socialRepository = remember(db) { SocialRepository(db) }
  val firebaseUser by authRepository.authStateFlow().collectAsStateWithLifecycle(initialValue = authRepository.currentUser())

  HelloTheme(darkTheme = isDark) {
    val user = firebaseUser
    if (user == null) {
      AuthScreen(
        authRepository = authRepository,
        onAuthenticated = {},
        vaultStorage = vault,
        signInScope = scope
      )
    } else {
      var biometricUnlocked by remember(user.uid) { mutableStateOf(!vault.isBiometricLockEnabled) }
      var biometricError by remember(user.uid) { mutableStateOf<String?>(null) }
      var biometricRateLimit by remember(user.uid) { mutableStateOf(rateLimiter.getState()) }

      LaunchedEffect(biometricRateLimit.isLockedOut) {
        while (rateLimiter.getState().isLockedOut) {
          biometricRateLimit = rateLimiter.getState()
          delay(1000L)
        }
        biometricRateLimit = rateLimiter.getState()
      }

      if (vault.isBiometricLockEnabled && !biometricUnlocked) {
        BiometricVaultLockScreen(
          userDisplayLabel = user.email ?: user.displayName ?: "your account",
          integrityReport = integrityReport,
          rateLimitState = biometricRateLimit,
          errorMessage = biometricError,
          onTriggerBiometric = {
            val currentLimit = rateLimiter.getState()
            biometricRateLimit = currentLimit
            if (currentLimit.isLockedOut) {
              biometricError = "Cooldown active. Retry in ${currentLimit.remainingSeconds}s."
            } else {
              val fragActivity = context.findFragmentActivity()
              if (fragActivity == null) {
                biometricError = "FragmentActivity unavailable for BiometricPrompt."
              } else {
                BiometricAuthHelper.authenticate(
                  activity = fragActivity,
                  title = "Unlock HELLO Vault",
                  subtitle = "Confirm Fingerprint, Face Unlock, or Passkey",
                  onSuccess = {
                    rateLimiter.recordSuccess()
                    biometricRateLimit = rateLimiter.getState()
                    biometricError = null
                    biometricUnlocked = true
                    scope.launch {
                      authRepository.refreshAndVerifyIdToken(vault)
                      authRepository.syncSecurityState(
                        failedAttempts = 0,
                        lockoutUntilMs = 0L,
                        lastAttemptMs = System.currentTimeMillis(),
                        biometricEnabled = true,
                        integrityReport = integrityReport
                      )
                      authRepository.logSecurityEvent(
                        eventType = "biometric_unlocked",
                        detail = "Second-factor biometric verification succeeded",
                        integritySummary = integrityReport.summary
                      )
                    }
                  },
                  onError = { err ->
                    val nextLimit = rateLimiter.recordFailure()
                    biometricRateLimit = nextLimit
                    biometricError = err
                    scope.launch {
                      authRepository.syncSecurityState(
                        failedAttempts = nextLimit.failedAttempts,
                        lockoutUntilMs = nextLimit.lockoutUntilEpochMs,
                        lastAttemptMs = System.currentTimeMillis(),
                        biometricEnabled = true,
                        integrityReport = integrityReport
                      )
                      authRepository.logSecurityEvent(
                        eventType = "biometric_failed",
                        detail = err,
                        integritySummary = integrityReport.summary
                      )
                    }
                  },
                  onCancelled = {
                    biometricError = "Verification required to access encrypted session."
                  }
                )
              }
            }
          },
          onSignOut = {
            scope.launch { authRepository.signOut(context, vault) }
          }
        )
      } else {
        key(user.uid) {
          AuthenticatedHelloShell(
            uid = user.uid,
            db = db,
            authRepository = authRepository,
            socialRepository = socialRepository,
            pendingRoute = pendingRoute,
            onPendingRouteConsumed = onPendingRouteConsumed,
            appPrefs = appPrefs,
            themeMode = themeMode,
            isDark = isDark,
            language = language,
            onThemeChange = { mode -> scope.launch { appPrefs.setThemeMode(mode) } },
            onLanguageChange = { lang -> scope.launch { appPrefs.setLanguage(lang) } },
            vaultStorage = vault,
            integrityReport = integrityReport,
            strictDebugMode = strictDebugMode,
            onStrictDebugModeChange = { enabled ->
              vault.isStrictTamperModeEnabled = enabled
              strictDebugMode = enabled
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedHelloShell(
  uid: String,
  db: FirebaseFirestore,
  authRepository: AuthRepository,
  socialRepository: SocialRepository,
  pendingRoute: String?,
  onPendingRouteConsumed: () -> Unit,
  appPrefs: AppPreferences,
  themeMode: ThemeMode,
  isDark: Boolean,
  language: com.example.data.preferences.AppLanguage,
  onThemeChange: (ThemeMode) -> Unit,
  onLanguageChange: (com.example.data.preferences.AppLanguage) -> Unit,
  vaultStorage: EncryptedVaultStorage,
  integrityReport: DeviceIntegrityReport,
  strictDebugMode: Boolean,
  onStrictDebugModeChange: (Boolean) -> Unit
) {
  val context = LocalContext.current
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { /* User decision respected; Firestore notifications remain accessible in-app */ }

  val socialVm: MainSocialViewModel = viewModel(
    key = "social_$uid",
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainSocialViewModel(authRepository, socialRepository, uid) as T
    }
  )
  val communityVm: CommunityViewModel = viewModel(
    key = "community_$uid",
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CommunityViewModel(CommunityRepository(db), socialRepository, uid, GovernanceRepository(db)) as T
    }
  )
  val spacesVm: SpacesGovernanceViewModel = viewModel(
    key = "spaces_$uid",
    factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SpacesGovernanceViewModel(SpacesRepository(db), GovernanceRepository(db), socialRepository, uid) as T
    }
  )

  LaunchedEffect(uid) {
    socialVm.bootstrapProfile()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val me by socialVm.currentUser.collectAsStateWithLifecycle()
  val privateUser by socialVm.privateUser.collectAsStateWithLifecycle()
  val friendships by communityVm.friendships.collectAsStateWithLifecycle()
  val blocked by communityVm.blockedUsers.collectAsStateWithLifecycle()
  val notifState by communityVm.notifications.collectAsStateWithLifecycle()
  val conversationsState by communityVm.conversations.collectAsStateWithLifecycle()
  val globalSettings by spacesVm.globalSettings.collectAsStateWithLifecycle()
  val ownerConfig by spacesVm.ownerConfig.collectAsStateWithLifecycle()
  val isVerifiedOwner by spacesVm.isVerifiedOwner.collectAsStateWithLifecycle()
  val mutualMatches by spacesVm.myMutualMatches.collectAsStateWithLifecycle()
  val storiesState by socialVm.stories.collectAsStateWithLifecycle()
  val savedHighlightStories by socialVm.savedHighlightStories.collectAsStateWithLifecycle()

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, uid) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> socialVm.setAppForegroundOnline(true)
        Lifecycle.Event.ON_STOP -> socialVm.setAppForegroundOnline(false)
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      socialVm.setAppForegroundOnline(false)
    }
  }

  LaunchedEffect(mutualMatches) {
    com.example.notifications.SecretLoveAlarmScheduler.syncAlarms(context, mutualMatches)
  }
  val searchHistory by appPrefs.searchHistory.collectAsStateWithLifecycle(initialValue = emptyList())
  val reelsAutoAdvancePref by appPrefs.reelsAutoAdvance.collectAsStateWithLifecycle(initialValue = true)
  val reelsRegionPref by appPrefs.reelsRegion.collectAsStateWithLifecycle(initialValue = "GLOBAL")
  val incognitoHideStatus by appPrefs.incognitoHideStatus.collectAsStateWithLifecycle(initialValue = false)
  val incognitoPrivateViewing by appPrefs.incognitoPrivateViewing.collectAsStateWithLifecycle(initialValue = false)
  val disappearingChat24h by appPrefs.disappearingChat24h.collectAsStateWithLifecycle(initialValue = false)
  val incognitoAnyActive by appPrefs.incognitoAnyActive.collectAsStateWithLifecycle(initialValue = false)
  val prefsScope = rememberCoroutineScope()

  LaunchedEffect(reelsAutoAdvancePref, reelsRegionPref) {
    socialVm.restoreReelsPlaybackSettings(
      autoAdvance = reelsAutoAdvancePref,
      regionCode = reelsRegionPref
    )
  }

  LaunchedEffect(incognitoHideStatus) {
    socialVm.setIncognitoHideStatus(incognitoHideStatus)
  }

  val seenNotificationIds = remember(uid) { mutableSetOf<String>() }
  var notifBaselineLoaded by remember(uid) { mutableStateOf(false) }
  LaunchedEffect(notifState, privateUser) {
    val state = notifState as? UiState.Success ?: return@LaunchedEffect
    if (!notifBaselineLoaded) {
      seenNotificationIds.addAll(state.data.map { it.id })
      notifBaselineLoaded = true
    } else {
      state.data.forEach { notif ->
        if (notif.id.isNotBlank() && notif.id !in seenNotificationIds) {
          seenNotificationIds.add(notif.id)
          if (!notif.isRead && HelloFirebaseMessagingService.isTypeEnabled(privateUser, notif.type)) {
            HelloFirebaseMessagingService.showLocalNotification(
              context = context,
              title = notif.title,
              body = notif.body,
              type = notif.type,
              targetId = notif.targetId,
              notificationKey = notif.id
            )
          }
        }
      }
    }
  }

  val friendUids = remember(friendships, uid) {
    friendships.flatMap { it.participantUids }.filter { it != uid }.distinct()
  }
  val mutualClockMs by spacesVm.liveClockMs.collectAsStateWithLifecycle()
  val mutualUids = remember(mutualMatches, mutualClockMs, uid) {
    mutualMatches
      .filter { com.example.data.model.MutualSecretLoveEngine.phaseOf(it, mutualClockMs) == com.example.data.model.MutualMatchPhase.UNLOCKED }
      .map { it.partnerUidFor(uid) }
      .filter { it.isNotBlank() && it != uid }
      .distinct()
  }
  val discoverableUsersState by communityVm.discoverableUsers.collectAsStateWithLifecycle()
  val discoverableUsersList = (discoverableUsersState as? UiState.Success)?.data.orEmpty()
  val blockedUids = remember(blocked) { blocked.map { it.blockedUid }.toSet() }
  val unreadNotifications = remember(notifState) {
    ((notifState as? UiState.Success)?.data ?: emptyList()).count { !it.isRead }
  }
  val unreadChats = remember(conversationsState, uid) {
    ((conversationsState as? UiState.Success)?.data ?: emptyList()).count { uid !in it.readByUids }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val socialMsg by socialVm.actionMessage.collectAsStateWithLifecycle()
  val communityMsg by communityVm.statusMessage.collectAsStateWithLifecycle()
  val spacesMsg by spacesVm.feedback.collectAsStateWithLifecycle()

  LaunchedEffect(socialMsg) {
    socialMsg?.let {
      snackbarHostState.showSnackbar(it)
      socialVm.clearMessage()
    }
  }
  LaunchedEffect(communityMsg) {
    communityMsg?.let {
      snackbarHostState.showSnackbar(it)
      communityVm.clearMessage()
    }
  }
  LaunchedEffect(spacesMsg) {
    spacesMsg?.let {
      snackbarHostState.showSnackbar(it)
      spacesVm.clearFeedback()
    }
  }

  val navController = rememberNavController()
  val backStack by navController.currentBackStackEntryAsState()
  val currentRoute = backStack?.destination?.route ?: HelloRoutes.HOME
  var activeStory by remember { mutableStateOf<Story?>(null) }

  LaunchedEffect(pendingRoute) {
    if (!pendingRoute.isNullOrBlank()) {
      runCatching { navController.navigate(pendingRoute) }
      onPendingRouteConsumed()
    }
  }

  val strings = stringsFor(language)
  val primaryNav = listOf(
    PrimaryNavItem(HelloRoutes.HOME, strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    PrimaryNavItem(HelloRoutes.FRIENDS, strings.navFriends, Icons.Rounded.People, Icons.Outlined.People, "nav_friends"),
    PrimaryNavItem(HelloRoutes.GROUPS, strings.navGroups, Icons.Rounded.Groups, Icons.Outlined.Groups, "nav_groups"),
    PrimaryNavItem(HelloRoutes.REELS, strings.navReels, Icons.Rounded.SlowMotionVideo, Icons.Outlined.SlowMotionVideo, "nav_reels"),
    PrimaryNavItem(HelloRoutes.SECRET_LOVE, strings.navLove, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder, "nav_secret_love")
  )
  val primaryRoutes = remember(primaryNav) { primaryNav.map { it.route }.toSet() }

  fun navigateSingleTop(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val hideChrome = currentRoute.startsWith("chat_thread")

  CompositionLocalProvider(LocalAppStrings provides strings) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail && !hideChrome) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
          Spacer(Modifier.padding(top = 12.dp))
          Box(
            modifier = Modifier
              .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                if (currentRoute == HelloRoutes.HUB) navController.popBackStack()
                else navigateSingleTop(HelloRoutes.HUB)
              }
              .testTag("rail_hub"),
            contentAlignment = Alignment.Center
          ) {
            HelloBrandLogo(size = 38.dp)
          }
          Spacer(Modifier.padding(top = 16.dp))
          primaryNav.forEach { item ->
            val selected = currentRoute == item.route
            NavigationRailItem(
              selected = selected,
              onClick = { navigateSingleTop(item.route) },
              icon = {
                Icon(
                  imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.label,
                  tint = if (item.isEmergency && selected) MaterialTheme.colorScheme.error else androidx.compose.material3.LocalContentColor.current
                )
              },
              label = { Text(item.label) },
              modifier = Modifier.testTag(item.tag)
            )
          }
          NavigationRailItem(
            selected = currentRoute == HelloRoutes.MESSENGER,
            onClick = { navigateSingleTop(HelloRoutes.MESSENGER) },
            icon = {
              Icon(
                painter = painterResource(
                  if (currentRoute == HelloRoutes.MESSENGER) R.drawable.ic_messenger_smart else R.drawable.ic_messenger_smart_outline
                ),
                contentDescription = strings.navChat,
                modifier = Modifier.size(22.dp)
              )
            },
            label = { Text(strings.navChat) },
            modifier = Modifier.testTag("nav_messenger")
          )
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = if (hideChrome) WindowInsets(0, 0, 0, 0) else WindowInsets.navigationBars,
        topBar = {
          if (!hideChrome) {
            Column {
              TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                      .defaultMinSize(minHeight = 48.dp)
                      .clip(RoundedCornerShape(14.dp))
                      .clickable {
                        if (currentRoute == HelloRoutes.HUB) {
                          navController.popBackStack()
                        } else {
                          navigateSingleTop(HelloRoutes.HUB)
                        }
                      }
                      .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                      .testTag("nav_hub")
                  ) {
                    HelloBrandLogo(size = 26.dp)
                    Column(verticalArrangement = Arrangement.Center) {
                      Text(
                        text = globalSettings.appName.ifBlank { "HELLO" },
                        fontSize = 19.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                      )
                      if (language != com.example.data.preferences.AppLanguage.EN) {
                        Text(
                          text = language.greetingWord,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          softWrap = false
                        )
                      }
                    }
                  }
                },
                actions = {
                  IconButton(
                    onClick = { onThemeChange(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK) },
                    modifier = Modifier.testTag("top_theme_toggle")
                  ) {
                    Surface(
                      modifier = Modifier.size(36.dp),
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                          contentDescription = if (isDark) strings.dayMode else strings.nightMode,
                          tint = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                  }
                  IconButton(
                    onClick = { navigateSingleTop(HelloRoutes.SEARCH) },
                    modifier = Modifier.testTag("top_search_button")
                  ) {
                    val searchActive = currentRoute == HelloRoutes.SEARCH
                    Surface(
                      modifier = Modifier.size(36.dp),
                      shape = CircleShape,
                      color = if (searchActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = Icons.Rounded.Search,
                          contentDescription = strings.search,
                          tint = if (searchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.size(20.dp)
                        )
                      }
                    }
                  }
                  IconButton(
                    onClick = { navigateSingleTop(HelloRoutes.MESSENGER) },
                    modifier = Modifier.testTag("top_chat_button")
                  ) {
                    val chatActive = currentRoute == HelloRoutes.MESSENGER
                    Surface(
                      modifier = Modifier.size(36.dp),
                      shape = CircleShape,
                      color = if (chatActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                          badge = {
                            if (unreadChats > 0) {
                              Badge { Text("$unreadChats") }
                            }
                          }
                        ) {
                          Icon(
                            painter = painterResource(
                              if (chatActive) R.drawable.ic_messenger_smart else R.drawable.ic_messenger_smart_outline
                            ),
                            contentDescription = strings.navChat,
                            tint = if (chatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                          )
                        }
                      }
                    }
                  }
                  IconButton(
                    onClick = { navigateSingleTop(HelloRoutes.NOTIFICATIONS) },
                    modifier = Modifier.testTag("top_notifications_button")
                  ) {
                    val notifActive = currentRoute == HelloRoutes.NOTIFICATIONS
                    Surface(
                      modifier = Modifier.size(36.dp),
                      shape = CircleShape,
                      color = if (notifActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        BadgedBox(
                          badge = {
                            if (unreadNotifications > 0) {
                              Badge { Text("$unreadNotifications") }
                            }
                          }
                        ) {
                          Icon(
                            imageVector = if (unreadNotifications > 0 || notifActive) Icons.Rounded.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = strings.notifications,
                            tint = if (notifActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                          )
                        }
                      }
                    }
                  }
                  Box(
                    modifier = Modifier
                      .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                      .clip(CircleShape)
                      .clickable { navigateSingleTop(HelloRoutes.PROFILE) }
                      .padding(horizontal = 4.dp)
                      .testTag("top_profile_avatar"),
                    contentAlignment = Alignment.Center
                  ) {
                    UserAvatar(
                      name = me?.displayName ?: "Me",
                      photoUrl = me?.photoUrl ?: "",
                      size = 34.dp,
                      isOnline = me?.effectiveOnline == true,
                      isVerifiedOwner = isVerifiedOwner
                    )
                  }
                }
              )
              HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
              )
            }
          }
        },
        bottomBar = {
          if (!useRail && !hideChrome) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 2.dp
            ) {
              primaryNav.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateSingleTop(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label
                    )
                  },
                  label = {
                    Text(
                      text = item.label,
                      fontSize = 11.sp,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  alwaysShowLabel = true,
                  colors = if (item.isEmergency) {
                    NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.error,
                      selectedTextColor = MaterialTheme.colorScheme.error,
                      indicatorColor = MaterialTheme.colorScheme.errorContainer
                    )
                  } else {
                    NavigationBarItemDefaults.colors()
                  },
                  modifier = Modifier.testTag(item.tag)
                )
              }
            }
          }
        }
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),
          contentAlignment = Alignment.TopCenter
        ) {
          NavHost(
            navController = navController,
            startDestination = HelloRoutes.HOME,
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize()
          ) {
            val openUserProfile: (String) -> Unit = { targetUid ->
              if (targetUid.isNotBlank()) {
                if (targetUid == uid) {
                  navigateSingleTop(HelloRoutes.PROFILE)
                } else {
                  navController.navigate(HelloRoutes.userProfile(targetUid))
                }
              }
            }
            composable(HelloRoutes.HOME) {
              HomeScreen(
                viewModel = socialVm,
                currentUserId = uid,
                friendUids = friendUids,
                blockedUids = blockedUids,
                globalSettings = globalSettings,
                communityVm = communityVm,
                onOpenStories = { navController.navigate(HelloRoutes.STORIES) },
                onOpenStoryViewer = { story -> activeStory = story },
                onOpenProfile = openUserProfile
              )
            }
            composable(HelloRoutes.FRIENDS) {
              FriendsScreen(
                viewModel = communityVm,
                me = me,
                currentUserId = uid,
                onOpenChat = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) },
                onOpenProfile = openUserProfile
              )
            }
            composable(HelloRoutes.REELS) {
              ReelsScreen(
                viewModel = socialVm,
                communityVm = communityVm,
                me = me,
                currentUserId = uid,
                onOpenStories = { navController.navigate(HelloRoutes.STORIES) },
                onOpenProfile = openUserProfile,
                onOpenSettings = { navController.navigate(HelloRoutes.SETTINGS) }
              )
            }
            composable(HelloRoutes.STORIES) {
              StoriesScreen(
                viewModel = socialVm,
                currentUserId = uid,
                friendUids = friendUids,
                mutualUids = mutualUids,
                discoverableUsers = discoverableUsersList,
                blockedUids = blockedUids,
                onOpenViewer = { story -> activeStory = story }
              )
            }
            composable(HelloRoutes.MESSENGER) {
              MessengerScreen(
                viewModel = communityVm,
                me = me,
                currentUserId = uid,
                incognitoActive = incognitoAnyActive,
                onOpenThread = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) }
              )
            }
            composable(
              route = HelloRoutes.CHAT_THREAD,
              arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { entry ->
              val convId = entry.arguments?.getString("conversationId").orEmpty()
              ChatThreadScreen(
                conversationId = convId,
                viewModel = communityVm,
                me = me,
                currentUserId = uid,
                incognitoHideStatus = incognitoHideStatus,
                disappearingDefault24h = disappearingChat24h,
                onToggleDisappearing24h = { enabled ->
                  prefsScope.launch { appPrefs.setDisappearingChat24h(enabled) }
                },
                onBack = { navController.popBackStack() },
                onOpenProfile = openUserProfile
              )
            }
            composable(HelloRoutes.NOTIFICATIONS) {
              NotificationsScreen(
                viewModel = communityVm,
                onNotificationSelected = { notif ->
                  when (notif.type) {
                    "friend_request" -> navigateSingleTop(HelloRoutes.FRIENDS)
                    "message" -> if (notif.targetId.isNotBlank()) navController.navigate(HelloRoutes.chatThread(notif.targetId)) else navigateSingleTop(HelloRoutes.MESSENGER)
                    "group_invite", "sos_alert" -> if (notif.targetId.isNotBlank()) navController.navigate(HelloRoutes.groupDetail(notif.targetId)) else navigateSingleTop(HelloRoutes.GROUPS)
                    "page_invite" -> navigateSingleTop(HelloRoutes.PAGES)
                    "secret_love", "crush_match", "mutual_unlock_24h" -> navigateSingleTop(HelloRoutes.SECRET_LOVE)
                    else -> navigateSingleTop(HelloRoutes.HOME)
                  }
                },
                onOpenSettings = { navController.navigate(HelloRoutes.SETTINGS) }
              )
            }
            composable(HelloRoutes.GROUPS) {
              GroupsScreen(
                viewModel = spacesVm,
                currentUserId = uid,
                onOpenGroup = { gid -> navController.navigate(HelloRoutes.groupDetail(gid)) },
                onOpenSos = { navController.navigate(HelloRoutes.SOS) }
              )
            }
            composable(HelloRoutes.SOS) {
              SosHubScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                globalSettings = globalSettings,
                currentUserId = uid,
                friendUids = friendUids,
                onOpenGroup = { gid -> navController.navigate(HelloRoutes.groupDetail(gid)) },
                onOpenGroups = { navigateSingleTop(HelloRoutes.GROUPS) }
              )
            }
            composable(
              route = HelloRoutes.GROUP_DETAIL,
              arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { entry ->
              val gid = entry.arguments?.getString("groupId").orEmpty()
              GroupDetailScreen(
                groupId = gid,
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                globalSettings = globalSettings,
                currentUserId = uid,
                friendUids = friendUids,
                onBack = { navController.popBackStack() },
                onOpenProfile = openUserProfile
              )
            }
            composable(HelloRoutes.PAGES) {
              PagesScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                currentUserId = uid,
                friendUids = friendUids,
                communityVm = communityVm,
                me = me
              )
            }
            composable(HelloRoutes.EVENTS) {
              EventsScreen(spacesVm = spacesVm, communityVm = communityVm, me = me, currentUserId = uid)
            }
            composable(HelloRoutes.MARKETPLACE) {
              MarketplaceScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                me = me,
                currentUserId = uid,
                onOpenChat = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) }
              )
            }
            composable(HelloRoutes.PROFILE) {
              ProfileScreen(
                socialVm = socialVm,
                currentUserId = uid,
                friendCount = friendUids.size,
                isVerifiedOwner = isVerifiedOwner,
                communityVm = communityVm,
                spacesVm = spacesVm,
                vaultStorage = vaultStorage,
                integrityReport = integrityReport,
                onOpenStoryViewer = { story -> activeStory = story },
                onOpenSettings = { navController.navigate(HelloRoutes.SETTINGS) }
              )
            }
            composable(
              route = HelloRoutes.USER_PROFILE,
              arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { entry ->
              val targetUserId = entry.arguments?.getString("userId").orEmpty()
              UserProfileScreen(
                targetUserId = targetUserId,
                socialVm = socialVm,
                communityVm = communityVm,
                currentUserId = uid,
                friendUids = friendUids,
                blockedUids = blockedUids,
                spacesVm = spacesVm,
                vaultStorage = vaultStorage,
                integrityReport = integrityReport,
                incognitoPrivateViewing = incognitoPrivateViewing,
                onBack = { navController.popBackStack() },
                onOpenChat = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) },
                onOpenStoryViewer = { story -> activeStory = story },
                onOpenSettings = { navController.navigate(HelloRoutes.SETTINGS) }
              )
            }
            composable(HelloRoutes.SECRET_LOVE) {
              SecretLoveScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                me = me,
                currentUserId = uid,
                isFeatureEnabled = globalSettings.secretLoveEnabled,
                vaultStorage = vaultStorage
              )
            }
            composable(HelloRoutes.SEARCH) {
              SearchScreen(
                socialVm = socialVm,
                communityVm = communityVm,
                spacesVm = spacesVm,
                searchHistory = searchHistory,
                onSaveSearchHistory = { q -> prefsScope.launch { appPrefs.addSearchHistory(q) } },
                onRemoveSearchHistory = { q -> prefsScope.launch { appPrefs.removeSearchHistoryItem(q) } },
                onClearSearchHistory = { prefsScope.launch { appPrefs.clearSearchHistory() } },
                onOpenGroup = { gid -> navController.navigate(HelloRoutes.groupDetail(gid)) },
                onOpenPages = { navController.navigate(HelloRoutes.PAGES) },
                onOpenEvents = { navController.navigate(HelloRoutes.EVENTS) },
                onOpenMarketplace = { navController.navigate(HelloRoutes.MARKETPLACE) },
                onOpenChat = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) },
                onOpenProfile = openUserProfile
              )
            }
            composable(HelloRoutes.SETTINGS) {
              SettingsScreen(
                socialVm = socialVm,
                communityVm = communityVm,
                currentThemeMode = themeMode,
                currentLanguage = language,
                onThemeChange = onThemeChange,
                onLanguageChange = onLanguageChange,
                spacesVm = spacesVm,
                isVerifiedOwner = isVerifiedOwner,
                vaultStorage = vaultStorage,
                integrityReport = integrityReport,
                strictDebugMode = strictDebugMode,
                onStrictDebugModeChange = onStrictDebugModeChange,
                onReelsAutoAdvancePersist = { enabled ->
                  prefsScope.launch { appPrefs.setReelsAutoAdvance(enabled) }
                },
                onReelsRegionPersist = { regionCode ->
                  prefsScope.launch { appPrefs.setReelsRegion(regionCode) }
                },
                incognitoHideStatus = incognitoHideStatus,
                incognitoPrivateViewing = incognitoPrivateViewing,
                disappearingChat24h = disappearingChat24h,
                onIncognitoMasterChange = { enabled ->
                  prefsScope.launch { appPrefs.setIncognitoModeAll(enabled) }
                },
                onIncognitoHideStatusChange = { enabled ->
                  prefsScope.launch { appPrefs.setIncognitoHideStatus(enabled) }
                },
                onIncognitoPrivateViewingChange = { enabled ->
                  prefsScope.launch { appPrefs.setIncognitoPrivateViewing(enabled) }
                },
                onDisappearingChat24hChange = { enabled ->
                  prefsScope.launch { appPrefs.setDisappearingChat24h(enabled) }
                }
              )
            }
            composable(HelloRoutes.OWNER_PROFILE) {
              OwnerProfileScreen(
                spacesVm = spacesVm,
                isVerifiedOwner = isVerifiedOwner,
                socialVm = socialVm,
                vaultStorage = vaultStorage,
                integrityReport = integrityReport,
                onOpenDashboard = { navController.navigate(HelloRoutes.OWNER_DASHBOARD) }
              )
            }
            composable(HelloRoutes.OWNER_DASHBOARD) {
              OwnerDashboardScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                isVerifiedOwner = isVerifiedOwner,
                vaultStorage = vaultStorage,
                integrityReport = integrityReport
              )
            }
            composable(HelloRoutes.HUB) {
              HubMenuScreen(
                me = me,
                ownerConfig = ownerConfig,
                isVerifiedOwner = isVerifiedOwner,
                globalSettings = globalSettings,
                onNavigate = { route ->
                  if (route in primaryRoutes) {
                    navigateSingleTop(route)
                  } else {
                    navController.navigate(route) { launchSingleTop = true }
                  }
                }
              )
            }
          }
        }
      }
    }

    activeStory?.let { story ->
      val activeFeedStories = ((storiesState as? UiState.Success)?.data.orEmpty())
        .filter { it.authorId !in blockedUids }
      val allStories = if (activeFeedStories.any { it.id == story.id }) {
        activeFeedStories
      } else {
        savedHighlightStories
          .filter { it.authorId == story.authorId && it.authorId !in blockedUids }
          .ifEmpty { listOf(story) }
      }
      StoryViewerDialog(
        stories = allStories,
        initialStory = story,
        viewModel = socialVm,
        communityVm = communityVm,
        me = me,
        currentUserId = uid,
        incognitoPrivateViewing = incognitoPrivateViewing,
        onOpenProfile = { targetUid ->
          if (targetUid.isNotBlank()) {
            if (targetUid == uid) {
              navigateSingleTop(HelloRoutes.PROFILE)
            } else {
              navController.navigate(HelloRoutes.userProfile(targetUid))
            }
          }
        },
        onDismiss = { activeStory = null }
      )
    }

    val activeCalls by communityVm.activeCalls.collectAsStateWithLifecycle()
    val usersStateForCall by communityVm.discoverableUsers.collectAsStateWithLifecycle()
    val allUsersForCall = (usersStateForCall as? UiState.Success)?.data.orEmpty()
    activeCalls.firstOrNull()?.let { liveCall ->
      val callSignals by remember(liveCall.id) {
        communityVm.observeCallSignals(liveCall.id)
      }.collectAsStateWithLifecycle(initialValue = emptyList())
      MessengerCallOverlay(
        call = liveCall,
        currentUserId = uid,
        allUsers = allUsersForCall,
        signals = callSignals,
        onSendSignal = { type, sdp, sdpMid, sdpMLineIndex, candidate ->
          communityVm.sendCallSignal(
            callId = liveCall.id,
            type = type,
            sdp = sdp,
            sdpMid = sdpMid,
            sdpMLineIndex = sdpMLineIndex,
            candidate = candidate
          )
        },
        onAccept = { communityVm.answerMessengerCall(liveCall.id) },
        onDecline = { communityVm.endMessengerCall(liveCall, "declined") },
        onEnd = {
          val endStatus = if (liveCall.status == "ringing" && liveCall.callerId != uid) "declined" else "ended"
          communityVm.endMessengerCall(liveCall, endStatus)
        },
        onTimeoutMissed = {
          communityVm.endMessengerCall(liveCall, "missed")
        }
      )
    }
  }
  }
}
