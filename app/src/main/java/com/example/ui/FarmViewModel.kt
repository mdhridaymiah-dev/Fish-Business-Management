package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FarmRepository
    
    // Auth Session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Navigation state
    private val _currentScreen = MutableStateFlow<String>("login") // login, dashboard, projects, shareholders, inventory, expenses, sales, customers, reports, settings, notifications, admin_panel, user_form, project_form, inventory_form, expense_form, sale_form, customer_form
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Error / Messages
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    // Database Flows
    val allUsers: StateFlow<List<User>>
    val allProjects: StateFlow<List<Project>>
    val allInventory: StateFlow<List<InventoryItem>>
    val allExpenses: StateFlow<List<Expense>>
    val allSales: StateFlow<List<Sale>>
    val allCustomers: StateFlow<List<Customer>>
    val allNotifications: StateFlow<List<Notification>>
    val allCancelRequests: StateFlow<List<ShareholderCancelRequest>>

    // Filters & States for UI
    val selectedProjectFilter = MutableStateFlow<Int?>(null) // null = All Projects
    val dateRangeStart = MutableStateFlow<String>("") // YYYY-MM-DD
    val dateRangeEnd = MutableStateFlow<String>("") // YYYY-MM-DD
    val darkMode = MutableStateFlow(false)
    val currencySymbol = MutableStateFlow("৳") // BDT Standard Icon
    val isBengali = MutableStateFlow(true) // Translating Toggle - Default is true/Bengali as requested

    fun t(en: String, bn: String): String {
        return if (isBengali.value) bn else en
    }

    // Active Activity Logs for Admin View (Simulated Activity Tracking)
    private val _activityLogs = MutableStateFlow<List<String>>(
        listOf(
            "System initialized. Seeding standard farm structures completed.",
            "Admin Hridoy logged in safely.",
            "Notification generated: low inventory on Aqua-Oxigen."
        )
    )
    val activityLogs: StateFlow<List<String>> = _activityLogs.asStateFlow()

    // Supabase Connection Configuration
    val supabaseUrl = MutableStateFlow(getBuildConfigValue("SUPABASE_URL", "https://wfuexmzxicqtjuzopvol.supabase.co/rest/v1/"))
    val supabaseProjectId = MutableStateFlow(getBuildConfigValue("SUPABASE_PROJECT_ID", "wfuexmzxicqtjuzopvol"))
    val supabaseAnonKey = MutableStateFlow(
        getBuildConfigValue("SUPABASE_ANON_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndmdWV4bXp4aWNxdGp1em9wdm9sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAxMTcwNTYsImV4cCI6MjA5NTY5MzA1Nn0.h2DCWwfIjY51-ulXXlvp2IE02L9Je57cZ3N_WNVUTVE").let {
            if (it.isBlank() || it == "YOUR_KEY" || it == "sb_publishable_DPYZnxPht7HLWY_VOBpHlw_vz-kg-Zu") {
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndmdWV4bXp4aWNxdGp1em9wdm9sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAxMTcwNTYsImV4cCI6MjA5NTY5MzA1Nn0.h2DCWwfIjY51-ulXXlvp2IE02L9Je57cZ3N_WNVUTVE"
            } else {
                it
            }
        }
    )

    val syncState = MutableStateFlow<String>("IDLE") // IDLE, SYNCING, SUCCESS, ERROR
    val syncErrorMessage = MutableStateFlow<String>("")

    private fun getBuildConfigValue(fieldName: String, defaultValue: String): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField(fieldName)
            field.get(null) as? String ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FarmRepository(database.farmDao)

        // Run seed synchronously inside viewModelScope
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }

        // Initialize state flows
        allUsers = repository.allUsers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allProjects = repository.allProjects
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allInventory = repository.allInventory
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allExpenses = repository.allExpenses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSales = repository.allSales
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCustomers = repository.allCustomers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allNotifications = repository.allNotifications
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allCancelRequests = repository.allCancelRequests
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // --- Authentication ---
    fun login(emailOrMobile: String, password: String): Boolean {
        var success = false
        viewModelScope.launch {
            // 1. First, search locally in the Room database cache
            var matchedUser = allUsers.value.find { 
                (it.email.equals(emailOrMobile, ignoreCase = true) || it.mobile == emailOrMobile) && it.password == password 
            }
            
            // 2. If not found locally, and a live Supabase connection is established, query Supabase authorities directly
            val apiKey = supabaseAnonKey.value
            if (matchedUser == null && apiKey.isNotBlank() && apiKey != "YOUR_KEY") {
                val url = supabaseUrl.value
                logActivity("Performing online security lookup on Supabase for credential: $emailOrMobile")
                try {
                    val service = SupabaseSyncService.create(url)
                    val authHeader = "Bearer $apiKey"
                    val userResponse = service.fetchData("users", apiKey, authHeader)
                    if (userResponse.isSuccessful) {
                        val remoteUsers = userResponse.body()
                        if (remoteUsers != null) {
                            val matchedMap = remoteUsers.find { map ->
                                val emailVal = map["email"] as? String ?: ""
                                val mobileVal = map["mobile"] as? String ?: ""
                                val passVal = map["password"] as? String ?: ""
                                (emailVal.equals(emailOrMobile, ignoreCase = true) || mobileVal == emailOrMobile) && passVal == password
                            }
                            if (matchedMap != null) {
                                val fetchedUser = matchedMap.toUser()
                                // Insert the verified credentials into the local Room database to enable offline recovery
                                repository.insertUser(fetchedUser)
                                matchedUser = fetchedUser
                                logActivity("Auth Success: Downloaded live profile for ${fetchedUser.fullName} to local database.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    logActivity("Supabase authority validation failed: ${e.message}")
                }
            }

            if (matchedUser != null) {
                if (matchedUser.status == "Inactive") {
                    _uiMessage.emit("Error: Your account is marked Inactive. Contact Admin.")
                } else {
                    _currentUser.value = matchedUser
                    success = true
                    logActivity("User ${matchedUser.fullName} (${matchedUser.role}) logged in.")
                    _currentScreen.value = "dashboard"
                }
            } else {
                _uiMessage.emit("Error: Invalid email/mobile or password.")
            }
        }
        return success
    }

    fun quickLogin(role: String) {
        viewModelScope.launch {
            val user = allUsers.value.find { it.role == role }
            if (user != null) {
                _currentUser.value = user
                logActivity("Quick-Login: ${user.fullName} as $role.")
                _currentScreen.value = "dashboard"
            } else {
                _uiMessage.emit("No seeded user found for role $role. Registering one.")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = "login"
        logActivity("User logged out.")
    }

    // --- Activity Logging Helper ---
    fun logActivity(message: String) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = formatter.format(Calendar.getInstance().time)
        val list = _activityLogs.value.toMutableList()
        list.add(0, "[$time] $message")
        _activityLogs.value = list
    }

    // --- User Management (Admin Only) ---
    fun saveUser(user: User, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            // Check project share validation if role is SHAREHOLDER
            if (user.role == "SHAREHOLDER" && user.assignedProjectId != null) {
                val sumShares = allUsers.value
                    .filter { it.assignedProjectId == user.assignedProjectId && it.id != user.id }
                    .sumOf { it.sharePercentage }
                
                if (sumShares + user.sharePercentage > 100.0) {
                    _uiMessage.emit("Error: Total share percentage on this project exceeds the 100% threshold limit! Currently allocated is ${sumShares}%.")
                    callback(false)
                    return@launch
                }
            }

            if (user.id == 0) {
                repository.insertUser(user)
                logActivity("Created new user profile: ${user.fullName} (${user.role}).")
            } else {
                repository.updateUser(user)
                logActivity("Updated user profile: ${user.fullName}.")
            }
            
            // If the user modified themselves, sync session
            if (currentUser.value?.id == user.id) {
                _currentUser.value = user
            }

            _uiMessage.emit("User record saved successfully.")
            callback(true)
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            repository.deleteUser(user)
            logActivity("Deleted user record: ${user.fullName}.")
            _uiMessage.emit("User profiling deleted.")
        }
    }

    // --- Project Management ---
    fun saveProject(project: Project, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (project.id == 0) {
                val id = repository.insertProject(project)
                logActivity("Registered new Aqua Project: ${project.name} (ID: $id).")
            } else {
                repository.updateProject(project)
                logActivity("Updated project status: ${project.name}.")
            }
            _uiMessage.emit("Project information preserved successfully.")
            callback(true)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            logActivity("Dismantled project: ${project.name}.")
            _uiMessage.emit("Project dismantled and deleted.")
        }
    }

    // --- Inventory Management ---
    fun saveInventoryItem(item: InventoryItem, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (item.id == 0) {
                repository.insertInventoryItem(item)
                logActivity("Added inventory item: ${item.itemName} (${item.quantity} ${item.unit}).")
            } else {
                repository.updateInventoryItem(item)
                logActivity("Restocked/Updated inventory item: ${item.itemName}.")
            }

            // Check if stock low alert should trigger
            if (item.quantity <= item.lowStockAlertLimit) {
                val alertMsg = "Stock Alert: '${item.itemName}' is down to ${item.quantity} ${item.unit}. (Limit: ${item.lowStockAlertLimit})."
                repository.insertNotification(
                    Notification(
                        title = "Low Stocks Warning",
                        message = alertMsg,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        type = "low_inventory"
                    )
                )
                logActivity("low stock notification posted automatically.")
            }

            _uiMessage.emit("Inventory item saved.")
            callback(true)
        }
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
            logActivity("Deleted inventory item: ${item.itemName}.")
            _uiMessage.emit("Item purged from stock registers.")
        }
    }

    // --- Expense Management ---
    fun saveExpense(expense: Expense, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isNew = (expense.id == 0)
            val requesterRole = currentUser.value?.role ?: "SHAREHOLDER"
            val requesterId = currentUser.value?.id ?: 0
            
            val finalExpense = if (isNew) {
                // If Admin initiates a purchase or if anyone requests as Petty Cash
                if (requesterRole == "ADMIN" || expense.isPettyCash) {
                    expense.copy(
                        isApproved = false,
                        approvalStatus = "PENDING",
                        requesterId = requesterId,
                        approvedByShareholders = ""
                    )
                } else {
                    // Regular manager expense or normal non-petty non-admin expense, requires standard single-approval
                    expense.copy(
                        isApproved = false,
                        approvalStatus = "PENDING",
                        requesterId = requesterId,
                        approvedByShareholders = ""
                    )
                }
            } else {
                expense
            }

            if (isNew) {
                repository.insertExpense(finalExpense)
                logActivity("Registered purchase request BDT ${finalExpense.amount} for project ID ${finalExpense.projectId}. Status: PENDING SHAREHOLDER APPROVAL.")
                
                repository.insertNotification(
                    Notification(
                        title = if (finalExpense.isPettyCash) "New Petty Cash purchase request" else "New purchase request (Needs Approval)",
                        message = "${currentUser.value?.fullName} requested approval for: ${finalExpense.title} (BDT ${finalExpense.amount}).",
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        type = "new_expense"
                    )
                )
            } else {
                repository.updateExpense(finalExpense)
                logActivity("Modified purchase details: ${finalExpense.title} (${finalExpense.amount}).")
            }
            _uiMessage.emit("Purchase request saved successfully.")
            callback(true)
        }
    }

    // Shareholder multi-person & partial approval function
    fun approveExpenseByShareholder(expense: Expense, shareholderId: Int, approve: Boolean, callback: () -> Unit = {}) {
        viewModelScope.launch {
            val currentApprovers = expense.approvedByShareholders
                .split(",")
                .filter { it.isNotEmpty() }
                .toMutableSet()
            
            if (approve) {
                currentApprovers.add(shareholderId.toString())
            } else {
                currentApprovers.remove(shareholderId.toString())
            }
            
            // Get other active shareholders of this project or general active shareholders if project ID is empty
            val projectShareholders = allUsers.value.filter { 
                it.role == "SHAREHOLDER" && it.status == "Active" && (expense.projectId == 0 || it.assignedProjectId == expense.projectId)
            }
            
            val totalShareholdersCount = if (projectShareholders.isNotEmpty()) projectShareholders.size else 1
            val approvedCount = currentApprovers.size
            
            val finalStatus = when {
                approvedCount == 0 -> "PENDING"
                approvedCount >= totalShareholdersCount -> "APPROVED"
                else -> "PARTIALLY_APPROVED"
            }
            
            val updatedExpense = expense.copy(
                approvedByShareholders = currentApprovers.joinToString(","),
                approvalStatus = finalStatus,
                isApproved = (finalStatus == "APPROVED")
            )
            repository.updateExpense(updatedExpense)
            
            logActivity("Shareholder ID $shareholderId approved purchase '${expense.title}' (${approvedCount}/$totalShareholdersCount approved). Status: $finalStatus")
            
            if (finalStatus == "APPROVED") {
                repository.insertNotification(
                    Notification(
                        title = "Expense Fully Approved",
                        message = "Expense '${expense.title}' of BDT ${expense.amount} has been fully approved by all shareholders.",
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        type = "profit_update"
                    )
                )
            } else if (finalStatus == "PARTIALLY_APPROVED") {
                repository.insertNotification(
                    Notification(
                        title = "Expense Partially Approved",
                        message = "Expense '${expense.title}' is partially approved ($approvedCount/$totalShareholdersCount shareholders).",
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        type = "new_expense"
                    )
                )
            }
            callback()
        }
    }

    fun approveExpense(expense: Expense) {
        viewModelScope.launch {
            val approved = expense.copy(isApproved = true, approvalStatus = "APPROVED")
            repository.updateExpense(approved)
            logActivity("Approved expense ID: ${expense.id} (BDT ${expense.amount}) for project ID ${expense.projectId}.")
            repository.insertNotification(
                Notification(
                    title = "Expense Approved",
                    message = "Expense '${expense.title}' of BDT ${expense.amount} has been approved. Net profits updated.",
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    type = "profit_update"
                )
            )
            _uiMessage.emit("Expense transaction approved. Cost share calculated.")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            logActivity("Deleted expense record: ${expense.title}.")
            _uiMessage.emit("Expense entry deleted.")
        }
    }

    // --- Sales Management ---
    fun saveSale(sale: Sale, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (sale.id == 0) {
                repository.insertSale(sale)
                logActivity("Registered Fish Sales: ${sale.weight} kg Tilapia sold for BDT ${sale.totalPrice}.")
                repository.insertNotification(
                    Notification(
                        title = "New Fish Sale Registered",
                        message = "Sold ${sale.weight} kg of ${sale.fishType} from Project ID ${sale.projectId} to ${sale.buyerName} for BDT ${sale.totalPrice}.",
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        type = "new_sale"
                    )
                )
            } else {
                repository.updateSale(sale)
                logActivity("Re-calibrated invoice: ${sale.invoiceNumber}.")
            }
            _uiMessage.emit("Sales receipt compiled.")
            callback(true)
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            repository.deleteSale(sale)
            logActivity("Deleted Sales receipt: ${sale.invoiceNumber}.")
            _uiMessage.emit("Sales entry removed.")
        }
    }

    // --- Customer Management ---
    fun saveCustomer(customer: Customer, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (customer.id == 0) {
                repository.insertCustomer(customer)
                logActivity("Added customer profiles: ${customer.name}.")
            } else {
                repository.updateCustomer(customer)
                logActivity("Updated customer information for ${customer.name}.")
            }
            _uiMessage.emit("Customer portfolio saved successfully.")
            callback(true)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            logActivity("purged customer portfolio: ${customer.name}.")
            _uiMessage.emit("Customer profiles purged.")
        }
    }

    fun dismissNotification(notif: Notification) {
        viewModelScope.launch {
            repository.deleteNotification(notif)
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            _uiMessage.emit("All alerts marked read.")
        }
    }

    // --- Shareholder Cancellation & Notice / Flow ---
    fun createCancelRequest(
        shareholderId: Int,
        buyerType: String,
        buyerShareholderId: Int?,
        buyerName: String,
        buyerMobile: String?,
        buyerEmail: String?,
        buyerPassword: String?,
        callback: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val shareholder = allUsers.value.find { it.id == shareholderId }
            if (shareholder == null) {
                _uiMessage.emit("Error: Shareholder not found.")
                callback(false)
                return@launch
            }
            
            val isNewAndExists = buyerType == "NEW_SHAREHOLDER" && allUsers.value.any { it.email.equals(buyerEmail, true) || it.mobile == buyerMobile }
            if (isNewAndExists) {
                _uiMessage.emit("Error: Email or Mobile matching new shareholder already exists.")
                callback(false)
                return@launch
            }
            
            val cancelRequest = ShareholderCancelRequest(
                shareholderId = shareholderId,
                shareholderName = shareholder.fullName,
                sharePercentage = shareholder.sharePercentage,
                investmentAmount = shareholder.investmentAmount,
                buyerType = buyerType,
                buyerShareholderId = buyerShareholderId,
                buyerName = buyerName,
                buyerMobile = buyerMobile,
                buyerEmail = buyerEmail,
                buyerPassword = buyerPassword,
                approvedUserIds = "",
                status = "PENDING"
            )
            
            repository.insertCancelRequest(cancelRequest)
            
            // Notice Notification to specifically alert the targeted user
            repository.insertNotification(
                Notification(
                    title = "Notice of Cancellation Action",
                    message = "Notice: Admin has requested cancel of profile for shareholder '${shareholder.fullName} (${shareholder.sharePercentage}%)'. Buyer: '$buyerName'. Pending approval of remaining shareholders.",
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    type = "harvest_reminder"
                )
            )
            
            logActivity("Admin requested cancellation for ${shareholder.fullName} with transfer to $buyerName.")
            _uiMessage.emit("Cancellation requested. Notification sent to candidates.")
            callback(true)
        }
    }

    fun approveCancelRequest(request: ShareholderCancelRequest, shareholderId: Int, callback: () -> Unit = {}) {
        viewModelScope.launch {
            val approversSet = request.approvedUserIds
                .split(",")
                .filter { it.isNotEmpty() }
                .toMutableSet()
            
            approversSet.add(shareholderId.toString())
            
            // Get all active shareholders of this project or general active shareholders EXCLUDING the canceled shareholder
            val otherShareholders = allUsers.value.filter { 
                it.role == "SHAREHOLDER" && it.status == "Active" && it.id != request.shareholderId
            }
            
            val totalRequiredCount = otherShareholders.size
            val currentApprovalsCount = approversSet.size
            
            // If all other shareholders approved, finish the transfer action
            val isFullyApproved = currentApprovalsCount >= totalRequiredCount
            val nextStatus = if (isFullyApproved) "APPROVED" else "PENDING"
            
            val updatedRequest = request.copy(
                approvedUserIds = approversSet.joinToString(","),
                status = nextStatus
            )
            repository.updateCancelRequest(updatedRequest)
            
            logActivity("Shareholder ID $shareholderId approved cancellation for ${request.shareholderName}. Status: $nextStatus ($currentApprovalsCount/$totalRequiredCount approved)")
            
            if (isFullyApproved) {
                finalizeShareholderCancellation(updatedRequest)
            }
            callback()
        }
    }

    private suspend fun finalizeShareholderCancellation(request: ShareholderCancelRequest) {
        val canceledUser = allUsers.value.find { it.id == request.shareholderId } ?: return
        
        // 1. Set the canceled user to Inactive so they don't count in active lists
        val updatedCanceledUser = canceledUser.copy(status = "Inactive", sharePercentage = 0.0, investmentAmount = 0.0)
        repository.updateUser(updatedCanceledUser)
        
        // 2. Perform Share Transfer
        when (request.buyerType) {
            "EXISTING_SHAREHOLDER" -> {
                val buyerId = request.buyerShareholderId
                if (buyerId != null) {
                    val buyerUser = allUsers.value.find { it.id == buyerId }
                    if (buyerUser != null) {
                        val updatedBuyer = buyerUser.copy(
                            sharePercentage = buyerUser.sharePercentage + request.sharePercentage,
                            investmentAmount = buyerUser.investmentAmount + request.investmentAmount
                        )
                        repository.updateUser(updatedBuyer)
                        logActivity("Shares of ${canceledUser.fullName} (${request.sharePercentage}%) successfully transferred to existing shareholder ${buyerUser.fullName}.")
                    }
                }
            }
            "PROJECT" -> {
                // Return to Project (retained shares in project reserves)
                logActivity("Shares of ${canceledUser.fullName} (${request.sharePercentage}%) absorbed back into the internal Project ownership.")
            }
            "NEW_SHAREHOLDER" -> {
                val newUser = User(
                    fullName = request.buyerName,
                    mobile = request.buyerMobile ?: "",
                    email = request.buyerEmail ?: "new@farm.com",
                    role = "SHAREHOLDER",
                    password = request.buyerPassword ?: "1234",
                    assignedProjectId = canceledUser.assignedProjectId,
                    sharePercentage = request.sharePercentage,
                    investmentAmount = request.investmentAmount,
                    status = "Active"
                )
                repository.insertUser(newUser)
                logActivity("Shares of ${canceledUser.fullName} (${request.sharePercentage}%) successfully transferred to newly registered shareholder ${request.buyerName}.")
            }
        }
        
        // Notification
        repository.insertNotification(
            Notification(
                title = "Shareholder Cancellation Done",
                message = "${canceledUser.fullName} has been deactivated. Share assets transferred successfully to buyer '${request.buyerName}'.",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                type = "harvest_reminder"
            )
        )
    }

    fun deleteCancelRequest(request: ShareholderCancelRequest) {
        viewModelScope.launch {
            repository.deleteCancelRequest(request)
            logActivity("Admin deleted/withdrew cancel request for ${request.shareholderName}.")
        }
    }

    // Calculations & Project reports helper logic (Net Profit = Sales - Approved Expenses / Investment)
    fun getProjectReportData(projectId: Int): ProjectSummary {
        val proj = allProjects.value.find { it.id == projectId } ?: return ProjectSummary()
        
        val projSales = allSales.value.filter { it.projectId == projectId }.sumOf { it.totalPrice }
        val projExpenses = allExpenses.value.filter { it.projectId == projectId }.sumOf { it.amount }
        val approvedExpenses = allExpenses.value.filter { it.projectId == projectId && it.isApproved }.sumOf { it.amount }
        val unapprovedExpenses = allExpenses.value.filter { it.projectId == projectId && !it.isApproved }.sumOf { it.amount }

        val netProfit = projSales - approvedExpenses
        val shareholdersList = allUsers.value.filter { it.assignedProjectId == projectId && it.role == "SHAREHOLDER" }

        return ProjectSummary(
            project = proj,
            totalSales = projSales,
            totalApprovedExpenses = approvedExpenses,
            totalUnapprovedExpenses = unapprovedExpenses,
            netProfit = netProfit,
            shareholders = shareholdersList
        )
    }

    // Bidirectional sync: Push to Supabase Cloud
    fun syncToSupabase() {
        viewModelScope.launch {
            syncState.value = "SYNCING"
            syncErrorMessage.value = ""
            logActivity("MIGRATION: Starting cloud database synchronization...")
            
            val url = supabaseUrl.value
            val apiKey = supabaseAnonKey.value
            
            if (apiKey.isBlank()) {
                // Perform interactive fallback simulated migration
                logActivity("MIGRATION: No live key found. Simulating cloud migration setup...")
                kotlinx.coroutines.delay(1000)
                logActivity("MIGRATION: Virtualized schema on endpoint: $url")
                kotlinx.coroutines.delay(800)
                logActivity("MIGRATION: Exported ${allProjects.value.size} projects successfully.")
                logActivity("MIGRATION: Exported ${allUsers.value.size} user profiles.")
                logActivity("MIGRATION: Synced ${allExpenses.value.size} expenses and ${allSales.value.size} sales receipts.")
                kotlinx.coroutines.delay(600)
                syncState.value = "SUCCESS"
                logActivity("MIGRATION SUCCESS: [SIMULATED] Secure backup completed on Supabase Cloud!")
                _uiMessage.emit("Simulated Sync completed! Enter real API Key in Settings for live connection.")
                return@launch
            }
            
            try {
                val service = SupabaseSyncService.create(url)
                val authHeader = "Bearer $apiKey"
                
                // 1. Sync Projects
                val projects = allProjects.value
                if (projects.isNotEmpty()) {
                    val body = projects.map { it.toMap() }
                    val response = service.upsertData("projects", apiKey, authHeader, data = body)
                    if (!response.isSuccessful) {
                        throw Exception("Failed to sync Projects: Code ${response.code()}")
                    }
                }
                
                // 2. Sync Users
                val users = allUsers.value
                if (users.isNotEmpty()) {
                    val body = users.map { it.toMap() }
                    val response = service.upsertData("users", apiKey, authHeader, data = body)
                    if (!response.isSuccessful) {
                        throw Exception("Failed to sync Users: Code ${response.code()}")
                    }
                }

                // 3. Sync Inventory
                val inventory = allInventory.value
                if (inventory.isNotEmpty()) {
                    val body = inventory.map { it.toMap() }
                    val response = service.upsertData("inventory_items", apiKey, authHeader, data = body)
                    if (!response.isSuccessful) {
                        throw Exception("Failed to sync Inventory Items: Code ${response.code()}")
                    }
                }

                // 4. Sync Expenses
                val expenses = allExpenses.value
                if (expenses.isNotEmpty()) {
                    val body = expenses.map { it.toMap() }
                    val response = service.upsertData("expenses", apiKey, authHeader, data = body)
                    if (!response.isSuccessful) {
                        throw Exception("Failed to sync Expenses: Code ${response.code()}")
                    }
                }

                // 5. Sync Sales
                val sales = allSales.value
                if (sales.isNotEmpty()) {
                    val body = sales.map { it.toMap() }
                    val response = service.upsertData("sales", apiKey, authHeader, data = body)
                    if (!response.isSuccessful) {
                        throw Exception("Failed to sync Sales: Code ${response.code()}")
                    }
                }

                // 6. Sync Customers
                val customers = allCustomers.value
                if (customers.isNotEmpty()) {
                    val body = customers.map { it.toMap() }
                    val response = service.upsertData("customers", apiKey, authHeader, data = body)
                    if (!response.isSuccessful) {
                        throw Exception("Failed to sync Customers: Code ${response.code()}")
                    }
                }
                
                syncState.value = "SUCCESS"
                logActivity("MIGRATION SUCCESS: All local tables securely migrated live to Supabase Cloud!")
                _uiMessage.emit("Successfully synced databases with cloud servers.")
                
            } catch (e: Exception) {
                syncState.value = "ERROR"
                syncErrorMessage.value = e.localizedMessage ?: "Unknown Error"
                logActivity("MIGRATION ERROR Failed: ${e.message}")
                _uiMessage.emit("Sync Error: ${e.message}")
            }
        }
    }

    // Bidirectional sync: Pull from Supabase Cloud
    fun fetchFromSupabase() {
        viewModelScope.launch {
            syncState.value = "SYNCING"
            syncErrorMessage.value = ""
            logActivity("MIGRATION: Pulling online data down to local database...")
            
            val url = supabaseUrl.value
            val apiKey = supabaseAnonKey.value
            
            if (apiKey.isBlank()) {
                logActivity("MIGRATION: No live key found. Simulating data restore...")
                kotlinx.coroutines.delay(1200)
                syncState.value = "SUCCESS"
                logActivity("MIGRATION SUCCESS: Virtual restore finished successfully!")
                _uiMessage.emit("Simulated Download finish! Provide real API Key in Settings to pull.")
                return@launch
            }
            
            try {
                val service = SupabaseSyncService.create(url)
                val authHeader = "Bearer $apiKey"
                
                // 1. Fetch & Store projects
                val projResponse = service.fetchData("projects", apiKey, authHeader)
                if (projResponse.isSuccessful) {
                    projResponse.body()?.forEach { map ->
                        val proj = map.toProject()
                        repository.insertProject(proj)
                    }
                } else {
                    throw Exception("Failed to fetch Projects: Code ${projResponse.code()}")
                }

                // 2. Fetch & Store Users
                val userResponse = service.fetchData("users", apiKey, authHeader)
                if (userResponse.isSuccessful) {
                    userResponse.body()?.forEach { map ->
                        val u = map.toUser()
                        repository.insertUser(u)
                    }
                }

                // 3. Fetch & Store Inventory
                val invResponse = service.fetchData("inventory_items", apiKey, authHeader)
                if (invResponse.isSuccessful) {
                    invResponse.body()?.forEach { map ->
                        val item = map.toInventoryItem()
                        repository.insertInventoryItem(item)
                    }
                }

                // 4. Fetch & Store Expenses
                val expResponse = service.fetchData("expenses", apiKey, authHeader)
                if (expResponse.isSuccessful) {
                    expResponse.body()?.forEach { map ->
                        val exp = map.toExpense()
                        repository.insertExpense(exp)
                    }
                }

                // 5. Fetch & Store Sales
                val salesResponse = service.fetchData("sales", apiKey, authHeader)
                if (salesResponse.isSuccessful) {
                    salesResponse.body()?.forEach { map ->
                        val sale = map.toSale()
                        repository.insertSale(sale)
                    }
                }

                // 6. Fetch & Store Customers
                val custResponse = service.fetchData("customers", apiKey, authHeader)
                if (custResponse.isSuccessful) {
                    custResponse.body()?.forEach { map ->
                        val customer = map.toCustomer()
                        repository.insertCustomer(customer)
                    }
                }

                syncState.value = "SUCCESS"
                logActivity("MIGRATION SUCCESS: Downloaded all records from Cloud successfully!")
                _uiMessage.emit("Cloud data successfully integrated into local workspace.")
            } catch (e: Exception) {
                syncState.value = "ERROR"
                syncErrorMessage.value = e.localizedMessage ?: "Unknown Error"
                logActivity("MIGRATION ERROR Failed to read: ${e.message}")
                _uiMessage.emit("Download failed: ${e.message}")
            }
        }
    }
}

// Data holder structured purely for easy frontend dashboard & reporting binding
data class ProjectSummary(
    val project: Project = Project(name = "", pondName = "", pondSize = 0.0, fishType = "", stockQuantity = 0, startDate = "", estimatedHarvestDate = "", investmentAmount = 0.0),
    val totalSales: Double = 0.0,
    val totalApprovedExpenses: Double = 0.0,
    val totalUnapprovedExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val shareholders: List<User> = emptyList()
)
