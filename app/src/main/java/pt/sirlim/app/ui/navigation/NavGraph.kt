package pt.sirlim.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import pt.sirlim.app.ui.screens.login.LoginScreen
import pt.sirlim.app.ui.screens.login.LoginViewModel
import pt.sirlim.app.ui.screens.admin.AdminHomeScreen
import pt.sirlim.app.ui.screens.admin.application.DatabaseSettingsScreen
import pt.sirlim.app.ui.screens.admin.application.AdminApplicationMenu
import pt.sirlim.app.ui.screens.admin.application.accounts.AccountsListScreen
import pt.sirlim.app.ui.screens.admin.application.accounts.AccountFormScreen
import pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsScreen
import pt.sirlim.app.ui.screens.admin.groups_compartments.CompartmentFormScreen
import pt.sirlim.app.ui.screens.admin.tasks.TasksScreen
import pt.sirlim.app.ui.screens.admin.consultations.AdminConsultationsScreen
import pt.sirlim.app.ui.screens.admin.reports.ReportsScreen
import pt.sirlim.app.ui.screens.admin.indications.IndicationsScreen
import pt.sirlim.app.ui.screens.admin.indications.IndicationFormScreen
import pt.sirlim.app.ui.screens.admin.indications.IndicationsViewModel
import pt.sirlim.app.ui.screens.user.UserHomeScreen
import pt.sirlim.app.ui.screens.user.scanner.ScannerScreen
import pt.sirlim.app.ui.screens.user.ManualSelectionScreen
import pt.sirlim.app.ui.screens.user.indications.UserIndicationsScreen
import pt.sirlim.app.ui.screens.user.cleaning.CleaningStartScreen
import pt.sirlim.app.ui.screens.user.cleaning.CleaningTimerScreen
import pt.sirlim.app.ui.screens.user.consultations.UserConsultationsScreen
import pt.sirlim.app.data.model.UserRole
import java.time.LocalDate

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { user ->
                    if (user.role == UserRole.ADMIN) {
                        navController.navigate("admin_home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("user_home/${user.id}") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Admin Routes
        composable("admin_home") {
            AdminHomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("admin_home") { inclusive = true }
                    }
                }
            )
        }

        composable("admin_application") {
            AdminApplicationMenu(
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable("admin_db_settings") {
            DatabaseSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable("admin_accounts") {
            AccountsListScreen(
                onAddUser = { navController.navigate("admin_account_form") },
                onEditUser = { user -> 
                    navController.navigate("admin_account_form?userId=${user.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "admin_account_form?userId={userId}",
            arguments = listOf(navArgument("userId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            val loginViewModel: LoginViewModel = viewModel()
            val users by loginViewModel.users.collectAsState()
            val user = users.find { it.id == userId }
            
            AccountFormScreen(
                user = user,
                onBack = { 
                    loginViewModel.fetchUsers()
                    navController.popBackStack() 
                }
            )
        }

        composable(
            route = "admin_groups_compartments?tab={tab}",
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
            GroupsCompartmentsScreen(
                initialTab = tab,
                onAddCompartment = { navController.navigate("admin_compartment_form") },
                onEditCompartment = { comp ->
                    navController.navigate("admin_compartment_form?compId=${comp.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "admin_compartment_form?compId={compId}",
            arguments = listOf(navArgument("compId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val compId = backStackEntry.arguments?.getString("compId")
            val viewModel: pt.sirlim.app.ui.screens.admin.groups_compartments.GroupsCompartmentsViewModel = viewModel()
            val compartments by viewModel.compartments.collectAsState()
            val compartment = compartments.find { it.id == compId }
            
            CompartmentFormScreen(
                compartment = compartment,
                onBack = { 
                    navController.navigate("admin_groups_compartments?tab=1") {
                        popUpTo("admin_groups_compartments") { inclusive = true }
                    }
                }
            )
        }

        composable("admin_tasks") {
            TasksScreen(onBack = { navController.popBackStack() })
        }

        composable("admin_consultations") {
            AdminConsultationsScreen(onBack = { navController.popBackStack() })
        }

        composable("admin_reports") {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable("admin_indications") {
            IndicationsScreen(
                onAddIndication = { date -> navController.navigate("admin_indication_form?date=$date") },
                onEditIndication = { ind -> navController.navigate("admin_indication_form?indId=${ind.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "admin_indication_form?indId={indId}&date={date}",
            arguments = listOf(
                navArgument("indId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val indId = backStackEntry.arguments?.getString("indId")
            val dateStr = backStackEntry.arguments?.getString("date")
            
            IndicationFormScreen(
                indicationId = indId,
                initialDate = dateStr?.let { LocalDate.parse(it) },
                onBack = { navController.popBackStack() }
            )
        }

        // User Routes
        composable(
            route = "user_home/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserHomeScreen(
                userId = userId,
                onNavigate = { route -> navController.navigate("$route/$userId") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("user_home/$userId") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "scanner/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ScannerScreen(
                onQrScanned = { qrKey ->
                    navController.navigate("cleaning_start/$userId?qrKey=$qrKey")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "manual_selection/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ManualSelectionScreen(
                onCompartmentSelected = { qrKey ->
                    navController.navigate("cleaning_start/$userId?qrKey=$qrKey")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "user_indications/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserIndicationsScreen(
                userId = userId,
                onStartCleaning = { compId, indId ->
                    navController.navigate("cleaning_start/$userId?compId=$compId&indId=$indId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "user_consultations/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserConsultationsScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "cleaning_start/{userId}?qrKey={qrKey}&compId={compId}&indId={indId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("qrKey") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("compId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("indId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val qrKey = backStackEntry.arguments?.getString("qrKey")
            val compId = backStackEntry.arguments?.getString("compId")
            val indId = backStackEntry.arguments?.getString("indId")
            
            CleaningStartScreen(
                userId = userId,
                qrKey = qrKey,
                compId = compId,
                indicationId = indId,
                onStart = { actualCompId, actualIndId ->
                    navController.navigate("cleaning_timer/$userId/$actualCompId?indId=$actualIndId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "cleaning_timer/{userId}/{compId}?indId={indId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("compId") { type = NavType.StringType },
                navArgument("indId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val compId = backStackEntry.arguments?.getString("compId") ?: ""
            val indId = backStackEntry.arguments?.getString("indId")
            
            CleaningTimerScreen(
                userId = userId,
                compId = compId,
                indicationId = indId,
                onFinish = {
                    navController.navigate("user_home/$userId") {
                        popUpTo("user_home/$userId") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
