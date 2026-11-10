package com.example.parentapp.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.parentapp.ui.auth.LoginScreen
import com.example.parentapp.ui.auth.RegisterScreen
import com.example.parentapp.ui.auth.RoleSelectionScreen
import com.example.parentapp.ui.auth.SplashScreen
import com.example.parentapp.ui.auth.StartRoute
import com.example.parentapp.ui.activity.ActivityDetailScreen
import com.example.parentapp.ui.activity.ActivityEditorScreen
import com.example.parentapp.ui.activity.ActivityListScreen
import com.example.parentapp.ui.child.ChildDetailScreen
import com.example.parentapp.ui.child.ChildEditorScreen
import com.example.parentapp.ui.child.ChildListScreen
import com.example.parentapp.ui.reminder.ReminderDetailScreen
import com.example.parentapp.ui.reminder.ReminderEditorScreen
import com.example.parentapp.ui.reminder.ReminderListScreen
import com.example.parentapp.ui.admin.AuditLogsScreen
import com.example.parentapp.ui.admin.UserDetailScreen
import com.example.parentapp.ui.admin.UserManagementScreen
import com.example.parentapp.ui.profile.ProfileScreen
import com.example.parentapp.ui.referral.ReferralScreen
import com.example.parentapp.ui.reports.ReportsScreen
import com.example.parentapp.ui.settings.SettingsScreen
import com.example.parentapp.ui.task.TaskDetailScreen
import com.example.parentapp.ui.task.TaskEditorScreen
import com.example.parentapp.ui.task.TaskListScreen
import com.example.parentapp.ui.tips.TipDetailScreen
import com.example.parentapp.ui.tips.TipsScreen
import com.example.parentapp.ui.dashboard.DashboardScreen
import com.example.parentapp.ui.more.MoreScreen
import com.example.parentapp.ui.onboarding.OnboardingScreen
import com.example.parentapp.ui.shell.AppShell
import com.example.parentapp.ui.shell.ShellViewModel
import com.example.parentapp.ui.shell.TopLevelDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAppNavHost(
    navController: NavHostController = rememberNavController(),
    shellViewModel: ShellViewModel = viewModel(factory = ShellViewModel.Factory),
    pendingReminderId: Long? = null,
    onPendingReminderOpened: () -> Unit = {},
) {
    val session by shellViewModel.session.collectAsStateWithLifecycle()

    // A tapped reminder notification opens that reminder — but only once the user is past
    // splash/onboarding/login, so a signed-out user lands on it right after signing in.
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry?.destination
    LaunchedEffect(pendingReminderId, currentDestination) {
        val id = pendingReminderId ?: return@LaunchedEffect
        val dest = currentDestination ?: return@LaunchedEffect
        val beforeSignIn = listOf(
            Destinations.Splash::class, Destinations.Onboarding::class, Destinations.RoleSelection::class,
            Destinations.Register::class, Destinations.Login::class,
        ).any { dest.hasRoute(it) }
        if (beforeSignIn) return@LaunchedEffect
        onPendingReminderOpened()
        navController.navigate(Destinations.ReminderDetail(id)) { launchSingleTop = true }
    }

    fun goHomeAfterAuth() {
        navController.navigate(Destinations.Dashboard) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun toLogin() {
        navController.navigate(Destinations.Login) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun switchTab(dest: TopLevelDestination) {
        navController.navigate(dest.route) {
            popUpTo(Destinations.Dashboard) { inclusive = dest == TopLevelDestination.HOME }
            launchSingleTop = true
        }
    }

    val signOut: () -> Unit = { shellViewModel.logout(::toLogin) }

    // A screen that is the selected bottom-nav tab for the current role shows no back
    // arrow; the same screen reached from elsewhere (e.g. "More") gets one.
    val roleTabClasses = TopLevelDestination.forRole(session.role).map { it.route::class }.toSet()
    fun upIfNotTab(dest: Destinations): (() -> Unit)? =
        if (dest::class in roleTabClasses) null else ({ navController.popBackStack() })

    NavHost(navController = navController, startDestination = Destinations.Splash) {

        composable<Destinations.Splash> {
            SplashScreen(onDecided = { start ->
                val target = when (start) {
                    StartRoute.ONBOARDING -> Destinations.Onboarding
                    StartRoute.DASHBOARD -> Destinations.Dashboard
                    else -> Destinations.Login
                }
                navController.navigate(target) {
                    popUpTo(Destinations.Splash) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }

        composable<Destinations.Onboarding> {
            OnboardingScreen(onFinished = ::toLogin)
        }

        composable<Destinations.RoleSelection> {
            RoleSelectionScreen(
                onBack = { navController.popBackStack() },
                onCategorySelected = { category -> navController.navigate(Destinations.Register(category)) },
                onHasAccount = ::toLogin,
            )
        }

        composable<Destinations.Register> { entry ->
            val args = entry.toRoute<Destinations.Register>()
            RegisterScreen(
                category = args.category,
                onBack = { navController.popBackStack() },
                onAuthenticated = ::goHomeAfterAuth,
                onHasAccount = ::toLogin,
            )
        }

        composable<Destinations.Login> {
            LoginScreen(
                onAuthenticated = ::goHomeAfterAuth,
                onRegister = { navController.navigate(Destinations.RoleSelection) },
            )
        }

        composable<Destinations.Dashboard> {
            DashboardScreen(
                onSelectTab = ::switchTab,
                onOpenReminder = { navController.navigate(Destinations.ReminderDetail(it)) },
                onOpenTask = { navController.navigate(Destinations.TaskDetail(it)) },
                onOpenActivity = { navController.navigate(Destinations.ActivityDetail(it)) },
                onOpenProfile = { navController.navigate(Destinations.Profile) },
                onOpenSettings = { navController.navigate(Destinations.Settings) },
                onLogout = { shellViewModel.logout(::toLogin) },
            )
        }

        composable<Destinations.More> {
            MoreScreen(
                role = session.role,
                onSelectTab = ::switchTab,
                onNavigate = { navController.navigate(it) },
                onLogout = { shellViewModel.logout(::toLogin) },
            )
        }

        // ---- Top-level tabs (real screens land in Phases 4–6) ----
        composable<Destinations.ChildList> {
            ChildListScreen(
                onSelectTab = ::switchTab,
                onOpenChild = { navController.navigate(Destinations.ChildDetail(it)) },
                onAddChild = { navController.navigate(Destinations.ChildEditor()) },
                onSignOut = signOut,
            )
        }
        composable<Destinations.ReminderList> {
            ReminderListScreen(
                onSelectTab = ::switchTab,
                onNavigateUp = upIfNotTab(Destinations.ReminderList()),
                onSignOut = signOut,
                onOpenReminder = { navController.navigate(Destinations.ReminderDetail(it)) },
                onAddReminder = { navController.navigate(Destinations.ReminderEditor()) },
            )
        }
        composable<Destinations.TaskList> {
            TaskListScreen(
                onSelectTab = ::switchTab,
                onOpenTask = { navController.navigate(Destinations.TaskDetail(it)) },
                onAssignTask = { navController.navigate(Destinations.TaskEditor()) },
                onSignOut = signOut,
            )
        }
        composable<Destinations.ActivityList> {
            ActivityListScreen(
                onSelectTab = ::switchTab,
                onNavigateUp = upIfNotTab(Destinations.ActivityList()),
                onSignOut = signOut,
                onOpenActivity = { navController.navigate(Destinations.ActivityDetail(it)) },
                onAddActivity = { navController.navigate(Destinations.ActivityEditor()) },
            )
        }
        composable<Destinations.TipList> {
            TipsScreen(
                onSelectTab = ::switchTab,
                onOpenTip = { navController.navigate(Destinations.TipDetail(it)) },
                onNavigateUp = upIfNotTab(Destinations.TipList),
                onSignOut = signOut,
            )
        }
        composable<Destinations.Reports> {
            ReportsScreen(
                onSelectTab = ::switchTab,
                onNavigateUp = upIfNotTab(Destinations.Reports),
                onSignOut = signOut,
            )
        }
        composable<Destinations.UserManagement> {
            UserManagementScreen(
                onSelectTab = ::switchTab,
                onOpenUser = { navController.navigate(Destinations.UserDetail(it)) },
                onNavigateUp = upIfNotTab(Destinations.UserManagement),
                onSignOut = signOut,
            )
        }
        composable<Destinations.AuditLogs> {
            AuditLogsScreen(
                onSelectTab = ::switchTab,
                onNavigateUp = upIfNotTab(Destinations.AuditLogs),
                onSignOut = signOut,
            )
        }

        // ---- Detail / editor / secondary screens ----
        composable<Destinations.ChildDetail> { entry ->
            val childId = entry.toRoute<Destinations.ChildDetail>().childId
            ChildDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.ChildEditor(it)) },
                onAddActivity = { navController.navigate(Destinations.ActivityEditor(childId = it)) },
                onOpenActivity = { navController.navigate(Destinations.ActivityDetail(it)) },
                onAddReminder = { navController.navigate(Destinations.ReminderEditor(childId = it)) },
                onOpenReminder = { navController.navigate(Destinations.ReminderDetail(it)) },
                onAddTask = { navController.navigate(Destinations.TaskEditor(childId = it)) },
                onOpenTask = { navController.navigate(Destinations.TaskDetail(it)) },
            )
        }
        composable<Destinations.ChildEditor> {
            ChildEditorScreen(
                onDone = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() },
            )
        }
        composable<Destinations.ActivityDetail> {
            ActivityDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.ActivityEditor(activityId = it)) },
            )
        }
        composable<Destinations.ActivityEditor> {
            ActivityEditorScreen(
                onDone = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() },
            )
        }
        composable<Destinations.ReminderDetail> {
            ReminderDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.ReminderEditor(reminderId = it)) },
            )
        }
        composable<Destinations.ReminderEditor> {
            ReminderEditorScreen(
                onDone = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() },
            )
        }
        composable<Destinations.TaskDetail> {
            TaskDetailScreen(
                onNavigateUp = { navController.popBackStack() },
                onEdit = { navController.navigate(Destinations.TaskEditor(taskId = it)) },
            )
        }
        composable<Destinations.TaskEditor> {
            TaskEditorScreen(
                onDone = { navController.popBackStack() },
                onNavigateUp = { navController.popBackStack() },
            )
        }
        composable<Destinations.TipDetail> {
            TipDetailScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable<Destinations.ReferralDirectory> {
            ReferralScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable<Destinations.Profile> {
            ProfileScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable<Destinations.Settings> {
            SettingsScreen(
                onNavigateUp = { navController.popBackStack() },
                onDataReset = ::toLogin,
            )
        }
        composable<Destinations.UserDetail> {
            UserDetailScreen(onNavigateUp = { navController.popBackStack() })
        }
    }
}
