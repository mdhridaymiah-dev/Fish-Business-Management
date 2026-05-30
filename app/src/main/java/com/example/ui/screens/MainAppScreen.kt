package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Notification
import com.example.ui.FarmViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: FarmViewModel,
    onNavigateToForm: (String) -> Unit // Handles add item triggers
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val notificationList by viewModel.allNotifications.collectAsState()
    
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showNotifMenu by remember { mutableStateOf(false) }
    val isBN by viewModel.isBengali.collectAsState()

    val userRole = currentUser?.role ?: "ADMIN"
    val userName = currentUser?.fullName ?: "Administrator"

    // Responsive configuration check
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Filter screens based on Role Data Visibility rules
    val navItems = remember(userRole, isBN) {
        listOf(
            NavItem("dashboard", viewModel.t("Dashboard", "ড্যাশবোর্ড"), Icons.Default.Dashboard),
            NavItem("projects", viewModel.t("Fish Projects", "মৎস্য প্রজেক্ট সমূহ"), Icons.Default.Water),
            NavItem("shareholders", viewModel.t("Shareholders", "শেয়ারহোল্ডার তালিকা"), Icons.Default.People),
            NavItem("inventory", viewModel.t("Inventory / Stock", "ইনভেন্টরি / স্টক"), Icons.Default.Inventory),
            NavItem("expenses", viewModel.t("Expenses & Purchases", "ক্রয় ও খরচ অনুমোদন"), Icons.Default.ReceiptLong),
            NavItem("sales", viewModel.t("Sales Management", "বিক্রয় ও উৎপাদন"), Icons.Default.AttachMoney),
            NavItem("customers", viewModel.t("Customers", "ক্রেতা তালিকা"), Icons.Default.ContactPage),
            NavItem("reports", viewModel.t("Reports Analytics", "রিপোর্ট ও বিশ্লেষণ"), Icons.Default.Assessment),
            NavItem("admin_panel", viewModel.t("Admin Panel", "প্যানেল পরিচালনা"), Icons.Default.AdminPanelSettings, roles = listOf("ADMIN")),
            NavItem("settings", viewModel.t("Settings", "সেটিংস"), Icons.Default.Settings)
        ).filter { it.roles.isEmpty() || it.roles.contains(userRole) }
    }

    // Modal Navigation Drawer structure
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isTablet,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF4FBFA)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Drawer Header Branding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Water,
                        contentDescription = "Water",
                        tint = Color(0xFF006A6A),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = viewModel.t("Fish Business Management ERP", "ফিজিক্যাল ফিশ বিজনেস ইআরপি"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF006A6A)
                        )
                    )
                }
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // Nav Items
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(navItems) { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontWeight = if (currentScreen == item.route) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                            selected = currentScreen == item.route,
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFFCCE8E8),
                                selectedIconColor = Color(0xFF006A6A),
                                selectedTextColor = Color(0xFF002020),
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = Color(0xFF4F6363),
                                unselectedTextColor = Color(0xFF4F6363)
                            ),
                            onClick = {
                                viewModel.navigateTo(item.route)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .minimumInteractiveComponentSize()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Logout Section
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.Red) },
                    label = { Text(viewModel.t("Logout Profile", "প্রস্থান করুন"), color = Color.Red, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        viewModel.logout()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .minimumInteractiveComponentSize()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Sidebar list if Expanded screen (tablet)
            if (isTablet) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = Color.White,
                    header = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open full drawer", tint = Color(0xFF006A6A))
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    navItems.forEach { item ->
                        NavigationRailItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            selected = currentScreen == item.route,
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color(0xFF006A6A),
                                selectedTextColor = Color(0xFF002020),
                                indicatorColor = Color(0xFFCCE8E8),
                                unselectedIconColor = Color(0xFF4F6363),
                                unselectedTextColor = Color(0xFF4F6363)
                            ),
                            onClick = { viewModel.navigateTo(item.route) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out", tint = Color.Red) },
                        label = { Text("Logout", color = Color.Red, fontSize = 10.sp) },
                        selected = false,
                        onClick = { viewModel.logout() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
                VerticalDivider(color = Color(0xFFE0E3E3))
            }

            // Main Scaffold with standard top of page Navbar
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color(0xFF191C1C),
                            navigationIconContentColor = Color(0xFF006A6A),
                            actionIconContentColor = Color(0xFF006A6A)
                        ),
                        title = {
                            Column {
                                Text(
                                    text = navItems.find { it.route == currentScreen }?.label ?: viewModel.t("Fish Business Management ERP", "ফিশ বিজনেস ম্যানেজমেন্ট ইআরপি"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF006A6A)
                                )
                                val roleText = when(userRole) {
                                    "ADMIN" -> viewModel.t("Admin", "প্রধান এডমিন")
                                    "MANAGER" -> viewModel.t("Manager", "খামার ম্যানেজার")
                                    "SHAREHOLDER" -> viewModel.t("Shareholder", "শেয়ারহোল্ডার")
                                    else -> userRole
                                }
                                Text(
                                    text = "${viewModel.t("Role", "পদবি")}: $roleText | $userName",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4F6363)
                                )
                            }
                        },
                        navigationIcon = {
                            if (!isTablet) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Hamburger")
                                }
                            }
                        },
                        actions = {
                            // Dynamic real-time EN / BN Language core Switch
                            TextButton(
                                onClick = { viewModel.isBengali.value = !isBN },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = if (isBN) "English" else "বাংলা",
                                    color = Color(0xFF006A6A),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            
                            // Notifications Badge
                            val unreadCount = notificationList.size
                            Box {
                                IconButton(
                                    onClick = { showNotifMenu = !showNotifMenu },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge { Text("$unreadCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Alerts Hub"
                                        )
                                    }
                                }

                                // Quick dropdown lists of dynamic alerts
                                if (showNotifMenu) {
                                    DropdownMenu(
                                        expanded = showNotifMenu,
                                        onDismissRequest = { showNotifMenu = false },
                                        modifier = Modifier
                                            .width(320.dp)
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Farm Notifications",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            TextButton(
                                                onClick = { viewModel.markNotificationsRead() },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text("Read All", fontSize = 11.sp)
                                            }
                                        }
                                        Divider()
                                        if (notificationList.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No active alerts. Stable pond status.", fontSize = 12.sp) },
                                                onClick = { showNotifMenu = false },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            )
                                        } else {
                                            notificationList.forEach { notif ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.navigateTo("notifications")
                                                            showNotifMenu = false
                                                        }
                                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Icon(
                                                            imageVector = when(notif.type) {
                                                                "low_inventory" -> Icons.Default.Warning
                                                                "new_expense" -> Icons.Default.ReceiptLong
                                                                "new_sale" -> Icons.Default.AttachMoney
                                                                else -> Icons.Default.Info
                                                            },
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(notif.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                            Text(notif.message, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing // Prevent camera notch / system bar cut-offs
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(viewModel = viewModel)
                        "projects" -> ProjectsScreen(viewModel = viewModel, onAddProject = { onNavigateToForm("project_form") })
                        "shareholders" -> ShareholdersScreen(viewModel = viewModel, onAddUser = { onNavigateToForm("user_form") })
                        "inventory" -> InventoryScreen(viewModel = viewModel, onAddItem = { onNavigateToForm("inventory_form") })
                        "expenses" -> ExpensesScreen(viewModel = viewModel, onAddExpense = { onNavigateToForm("expense_form") })
                        "sales" -> SalesScreen(viewModel = viewModel, onAddSale = { onNavigateToForm("sale_form") })
                        "customers" -> CustomersScreen(viewModel = viewModel, onAddCustomer = { onNavigateToForm("customer_form") })
                        "reports" -> ReportsScreen(viewModel = viewModel)
                        "admin_panel" -> AdminScreen(viewModel = viewModel, onAddUser = { onNavigateToForm("user_form") })
                        "settings" -> SettingsScreen(viewModel = viewModel)
                        "notifications" -> NotificationsScreen(viewModel = viewModel)
                        else -> Text("Screen not found", modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val roles: List<String> = emptyList()
)
