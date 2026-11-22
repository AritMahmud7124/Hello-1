package com.example.ui.navigation

object HelloRoutes {
  const val HOME = "home"
  const val FRIENDS = "friends"
  const val REELS = "reels"
  const val STORIES = "stories"
  const val MESSENGER = "messenger"
  const val CHAT_THREAD = "chat_thread/{conversationId}"
  const val NOTIFICATIONS = "notifications"
  const val GROUPS = "groups"
  const val SOS = "sos"
  const val GROUP_DETAIL = "group_detail/{groupId}"
  const val PAGES = "pages"
  const val EVENTS = "events"
  const val MARKETPLACE = "marketplace"
  const val PROFILE = "profile"
  const val USER_PROFILE = "user_profile/{userId}"
  const val SECRET_LOVE = "secret_love"
  const val SEARCH = "search"
  const val SETTINGS = "settings"
  const val OWNER_PROFILE = "owner_profile"
  const val OWNER_DASHBOARD = "owner_dashboard"
  const val HUB = "hub"

  fun chatThread(conversationId: String) = "chat_thread/$conversationId"
  fun groupDetail(groupId: String) = "group_detail/$groupId"
  fun userProfile(userId: String) = "user_profile/$userId"
}
