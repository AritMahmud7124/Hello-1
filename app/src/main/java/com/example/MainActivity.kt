package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.example.ui.components.HelloBrandLogo
import com.example.ui.components.UserAvatar
import com.example.ui.navigation.HelloRoutes
import com.example.ui.screens.AuthScreen
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
import com.example.ui.screens.StoriesScreen
import com.example.ui.screens.StoryViewerDialog
import com.example.ui.theme.HelloTheme
import com.example.ui.viewmodel.CommunityViewModel
import com.example.ui.viewmodel.MainSocialViewModel
import com.example.ui.viewmodel.SpacesGovernanceViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
  val icon: ImageVector,
  val tag: String
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
        onAuthenticated = {}
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
          language = language,
          onThemeChange = { mode -> scope.launch { appPrefs.setThemeMode(mode) } },
          onLanguageChange = { lang -> scope.launch { appPrefs.setLanguage(lang) } }
        )
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
  language: com.example.data.preferences.AppLanguage,
  onThemeChange: (ThemeMode) -> Unit,
  onLanguageChange: (com.example.data.preferences.AppLanguage) -> Unit
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
        CommunityViewModel(CommunityRepository(db), socialRepository, uid) as T
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
  val friendships by communityVm.friendships.collectAsStateWithLifecycle()
  val blocked by communityVm.blockedUsers.collectAsStateWithLifecycle()
  val notifState by communityVm.notifications.collectAsStateWithLifecycle()
  val globalSettings by spacesVm.globalSettings.collectAsStateWithLifecycle()
  val ownerConfig by spacesVm.ownerConfig.collectAsStateWithLifecycle()
  val isVerifiedOwner by spacesVm.isVerifiedOwner.collectAsStateWithLifecycle()
  val storiesState by socialVm.stories.collectAsStateWithLifecycle()

  val friendUids = remember(friendships, uid) {
    friendships.flatMap { it.participantUids }.filter { it != uid }.distinct()
  }
  val blockedUids = remember(blocked) { blocked.map { it.blockedUid }.toSet() }
  val unreadNotifications = remember(notifState) {
    ((notifState as? UiState.Success)?.data ?: emptyList()).count { !it.isRead }
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

  val primaryNav = listOf(
    PrimaryNavItem(HelloRoutes.HOME, "Home", Icons.Filled.Home, "nav_home"),
    PrimaryNavItem(HelloRoutes.FRIENDS, "Friends", Icons.Filled.People, "nav_friends"),
    PrimaryNavItem(HelloRoutes.REELS, "Reels", Icons.Filled.PlayCircle, "nav_reels"),
    PrimaryNavItem(HelloRoutes.MESSENGER, "Chat", Icons.AutoMirrored.Filled.Message, "nav_messenger"),
    PrimaryNavItem(HelloRoutes.HUB, "Menu", Icons.Filled.Apps, "nav_hub")
  )

  fun navigateSingleTop(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  val hideChrome = currentRoute.startsWith("chat_thread")

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail && !hideChrome) {
        NavigationRail(modifier = Modifier.fillMaxHeight()) {
          Spacer(Modifier.padding(top = 12.dp))
          HelloBrandLogo(size = 38.dp)
          Spacer(Modifier.padding(top = 16.dp))
          primaryNav.forEach { item ->
            NavigationRailItem(
              selected = currentRoute == item.route,
              onClick = { navigateSingleTop(item.route) },
              icon = { Icon(item.icon, contentDescription = item.label) },
              label = { Text(item.label) },
              modifier = Modifier.testTag(item.tag)
            )
          }
          NavigationRailItem(
            selected = currentRoute == HelloRoutes.GROUPS,
            onClick = { navigateSingleTop(HelloRoutes.GROUPS) },
            icon = { Icon(Icons.Filled.Groups, contentDescription = "Groups") },
            label = { Text("Groups") }
          )
          if (globalSettings.secretLoveEnabled) {
            NavigationRailItem(
              selected = currentRoute == HelloRoutes.SECRET_LOVE,
              onClick = { navigateSingleTop(HelloRoutes.SECRET_LOVE) },
              icon = { Icon(Icons.Filled.Favorite, contentDescription = "Secret Love") },
              label = { Text("Love") }
            )
          }
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
          if (!hideChrome) {
            TopAppBar(
              colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
              title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  HelloBrandLogo(size = 32.dp)
                  Column {
                    Text(globalSettings.appName.ifBlank { "HELLO" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (language != com.example.data.preferences.AppLanguage.EN) {
                      Text(language.greetingWord, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                  }
                }
              },
              actions = {
                IconButton(onClick = { navigateSingleTop(HelloRoutes.SEARCH) }, modifier = Modifier.testTag("top_search_button")) {
                  Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(onClick = { navigateSingleTop(HelloRoutes.NOTIFICATIONS) }, modifier = Modifier.testTag("top_notifications_button")) {
                  BadgedBox(
                    badge = {
                      if (unreadNotifications > 0) {
                        Badge { Text("$unreadNotifications") }
                      }
                    }
                  ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                  }
                }
                Box(
                  modifier = Modifier
                    .padding(end = 10.dp)
                    .clickable { navigateSingleTop(HelloRoutes.PROFILE) }
                    .testTag("top_profile_avatar")
                ) {
                  UserAvatar(
                    name = me?.displayName ?: "Me",
                    photoUrl = me?.photoUrl ?: "",
                    size = 34.dp,
                    isOnline = true,
                    isVerifiedOwner = isVerifiedOwner
                  )
                }
              }
            )
          }
        },
        bottomBar = {
          if (!useRail && !hideChrome) {
            NavigationBar {
              primaryNav.forEach { item ->
                NavigationBarItem(
                  selected = currentRoute == item.route,
                  onClick = { navigateSingleTop(item.route) },
                  icon = { Icon(item.icon, contentDescription = item.label) },
                  label = { Text(item.label) },
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
            .padding(innerPadding),
          contentAlignment = Alignment.TopCenter
        ) {
          NavHost(
            navController = navController,
            startDestination = HelloRoutes.HOME,
            modifier = Modifier
              .widthIn(max = 840.dp)
              .fillMaxSize()
          ) {
            composable(HelloRoutes.HOME) {
              HomeScreen(
                viewModel = socialVm,
                currentUserId = uid,
                friendUids = friendUids,
                blockedUids = blockedUids,
                globalSettings = globalSettings,
                onOpenStories = { navController.navigate(HelloRoutes.STORIES) },
                onOpenStoryViewer = { story -> activeStory = story }
              )
            }
            composable(HelloRoutes.FRIENDS) {
              FriendsScreen(
                viewModel = communityVm,
                me = me,
                currentUserId = uid,
                onOpenChat = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) }
              )
            }
            composable(HelloRoutes.REELS) {
              ReelsScreen(
                viewModel = socialVm,
                onOpenStories = { navController.navigate(HelloRoutes.STORIES) }
              )
            }
            composable(HelloRoutes.STORIES) {
              StoriesScreen(
                viewModel = socialVm,
                onOpenViewer = { story -> activeStory = story }
              )
            }
            composable(HelloRoutes.MESSENGER) {
              MessengerScreen(
                viewModel = communityVm,
                me = me,
                currentUserId = uid,
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
                onBack = { navController.popBackStack() }
              )
            }
            composable(HelloRoutes.NOTIFICATIONS) {
              NotificationsScreen(
                viewModel = communityVm,
                onNotificationSelected = { notif ->
                  when (notif.type) {
                    "friend_request" -> navController.navigate(HelloRoutes.FRIENDS)
                    "message" -> if (notif.targetId.isNotBlank()) navController.navigate(HelloRoutes.chatThread(notif.targetId)) else navController.navigate(HelloRoutes.MESSENGER)
                    "group_invite", "sos_alert" -> if (notif.targetId.isNotBlank()) navController.navigate(HelloRoutes.groupDetail(notif.targetId)) else navController.navigate(HelloRoutes.GROUPS)
                    "secret_love" -> navController.navigate(HelloRoutes.SECRET_LOVE)
                    else -> navController.navigate(HelloRoutes.HOME)
                  }
                }
              )
            }
            composable(HelloRoutes.GROUPS) {
              GroupsScreen(
                viewModel = spacesVm,
                currentUserId = uid,
                onOpenGroup = { gid -> navController.navigate(HelloRoutes.groupDetail(gid)) }
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
                onBack = { navController.popBackStack() }
              )
            }
            composable(HelloRoutes.PAGES) {
              PagesScreen(spacesVm = spacesVm, socialVm = socialVm, currentUserId = uid, friendUids = friendUids)
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
                onOpenSettings = { navController.navigate(HelloRoutes.SETTINGS) }
              )
            }
            composable(HelloRoutes.SECRET_LOVE) {
              SecretLoveScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                me = me,
                currentUserId = uid
              )
            }
            composable(HelloRoutes.SEARCH) {
              SearchScreen(
                socialVm = socialVm,
                communityVm = communityVm,
                spacesVm = spacesVm,
                onOpenGroup = { gid -> navController.navigate(HelloRoutes.groupDetail(gid)) },
                onOpenPages = { navController.navigate(HelloRoutes.PAGES) },
                onOpenEvents = { navController.navigate(HelloRoutes.EVENTS) },
                onOpenMarketplace = { navController.navigate(HelloRoutes.MARKETPLACE) },
                onOpenChat = { convId -> navController.navigate(HelloRoutes.chatThread(convId)) }
              )
            }
            composable(HelloRoutes.SETTINGS) {
              SettingsScreen(
                socialVm = socialVm,
                communityVm = communityVm,
                currentThemeMode = themeMode,
                currentLanguage = language,
                onThemeChange = onThemeChange,
                onLanguageChange = onLanguageChange
              )
            }
            composable(HelloRoutes.OWNER_PROFILE) {
              OwnerProfileScreen(
                spacesVm = spacesVm,
                isVerifiedOwner = isVerifiedOwner,
                onOpenDashboard = { navController.navigate(HelloRoutes.OWNER_DASHBOARD) }
              )
            }
            composable(HelloRoutes.OWNER_DASHBOARD) {
              OwnerDashboardScreen(
                spacesVm = spacesVm,
                socialVm = socialVm,
                communityVm = communityVm,
                isVerifiedOwner = isVerifiedOwner
              )
            }
            composable(HelloRoutes.HUB) {
              HubMenuScreen(
                me = me,
                ownerConfig = ownerConfig,
                isVerifiedOwner = isVerifiedOwner,
                globalSettings = globalSettings,
                onNavigate = { route -> navController.navigate(route) }
              )
            }
          }
        }
      }
    }

    activeStory?.let { story ->
      val allStories = (storiesState as? UiState.Success)?.data ?: listOf(story)
      StoryViewerDialog(
        stories = allStories,
        initialStory = story,
        viewModel = socialVm,
        onDismiss = { activeStory = null }
      )
    }
  }
}
