package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class FarmRepository(private val farmDao: FarmDao) {

    // Users
    val allUsers: Flow<List<User>> = farmDao.getAllUsersFlow()
    suspend fun getAllUsersDirect(): List<User> = farmDao.getAllUsersDirect()
    
    suspend fun getUserByEmail(email: String): User? = farmDao.getUserByEmail(email)
    suspend fun getUserByMobile(mobile: String): User? = farmDao.getUserByMobile(mobile)
    suspend fun insertUser(user: User): Long = farmDao.insertUser(user)
    suspend fun updateUser(user: User) = farmDao.updateUser(user)
    suspend fun deleteUser(user: User) = farmDao.deleteUser(user)

    // Projects
    val allProjects: Flow<List<Project>> = farmDao.getAllProjectsFlow()
    suspend fun getProjectById(id: Int): Project? = farmDao.getProjectById(id)
    suspend fun insertProject(project: Project): Long = farmDao.insertProject(project)
    suspend fun updateProject(project: Project) = farmDao.updateProject(project)
    suspend fun deleteProject(project: Project) = farmDao.deleteProject(project)

    // Inventory
    val allInventory: Flow<List<InventoryItem>> = farmDao.getAllInventoryFlow()
    suspend fun insertInventoryItem(item: InventoryItem): Long = farmDao.insertInventoryItem(item)
    suspend fun updateInventoryItem(item: InventoryItem) = farmDao.updateInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = farmDao.deleteInventoryItem(item)

    // Expenses
    val allExpenses: Flow<List<Expense>> = farmDao.getAllExpensesFlow()
    fun getExpensesForProject(projectId: Int): Flow<List<Expense>> = farmDao.getExpensesForProjectFlow(projectId)
    suspend fun insertExpense(expense: Expense): Long = farmDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = farmDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = farmDao.deleteExpense(expense)

    // Sales
    val allSales: Flow<List<Sale>> = farmDao.getAllSalesFlow()
    fun getSalesForProject(projectId: Int): Flow<List<Sale>> = farmDao.getSalesForProjectFlow(projectId)
    suspend fun insertSale(sale: Sale): Long = farmDao.insertSale(sale)
    suspend fun updateSale(sale: Sale) = farmDao.updateSale(sale)
    suspend fun deleteSale(sale: Sale) = farmDao.deleteSale(sale)

    // Customers
    val allCustomers: Flow<List<Customer>> = farmDao.getAllCustomersFlow()
    suspend fun insertCustomer(customer: Customer): Long = farmDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = farmDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = farmDao.deleteCustomer(customer)

    // Notifications
    val allNotifications: Flow<List<Notification>> = farmDao.getAllNotificationsFlow()
    suspend fun insertNotification(notification: Notification): Long = farmDao.insertNotification(notification)
    suspend fun markAllNotificationsAsRead() = farmDao.markAllNotificationsAsRead()
    suspend fun deleteNotification(notification: Notification) = farmDao.deleteNotification(notification)

    // Shareholder Cancel Requests
    val allCancelRequests: Flow<List<ShareholderCancelRequest>> = farmDao.getAllCancelRequestsFlow()
    suspend fun insertCancelRequest(request: ShareholderCancelRequest): Long = farmDao.insertCancelRequest(request)
    suspend fun updateCancelRequest(request: ShareholderCancelRequest) = farmDao.updateCancelRequest(request)
    suspend fun deleteCancelRequest(request: ShareholderCancelRequest) = farmDao.deleteCancelRequest(request)

    // Shareholder Objections
    val allObjections: Flow<List<Objection>> = farmDao.getAllObjectionsFlow()
    suspend fun insertObjection(objection: Objection): Long = farmDao.insertObjection(objection)
    suspend fun updateObjection(objection: Objection) = farmDao.updateObjection(objection)
    suspend fun deleteObjection(objection: Objection) = farmDao.deleteObjection(objection)

    // Seed method
    suspend fun seedDatabaseIfEmpty() {
        val projects = farmDao.getAllProjectsDirect()
        val users = farmDao.getAllUsersDirect()
        
        // Always ensure at least the Admin user exists so we don't lock out mdhridaymiah@gmail.com
        val hasAdmin = users.any { it.email.equals("mdhridaymiah@gmail.com", ignoreCase = true) || it.role == "ADMIN" }
        if (!hasAdmin) {
            farmDao.insertUser(
                User(
                    fullName = "Admin Hridoy",
                    mobile = "01700000001",
                    email = "mdhridaymiah@gmail.com", // User's email from request
                    role = "ADMIN",
                    password = "admin",
                    assignedProjectId = null,
                    sharePercentage = 0.0,
                    investmentAmount = 0.0,
                    status = "Active"
                )
            )
        }

        val hasManager = users.any { it.email.equals("manager@example.com", ignoreCase = true) || it.mobile == "01700000002" }
        if (!hasManager) {
            val p1Id = projects.getOrNull(0)?.id ?: 1
            farmDao.insertUser(
                User(
                    fullName = "Manager Karim",
                    mobile = "01700000002",
                    email = "manager@example.com",
                    role = "MANAGER",
                    password = "manager",
                    assignedProjectId = p1Id,
                    sharePercentage = 0.0,
                    investmentAmount = 0.0,
                    status = "Active"
                )
            )
        }

        val hasShareholder = users.any { it.email.equals("shareholder@example.com", ignoreCase = true) || it.mobile == "01700000003" }
        if (!hasShareholder) {
            val p1Id = projects.getOrNull(0)?.id ?: 1
            farmDao.insertUser(
                User(
                    fullName = "Rahim Shareholder",
                    mobile = "01700000003",
                    email = "shareholder@example.com",
                    role = "SHAREHOLDER",
                    password = "shareholder",
                    assignedProjectId = p1Id,
                    sharePercentage = 40.0,
                    investmentAmount = 150000.0,
                    status = "Active"
                )
            )
        }

        if (projects.isEmpty()) {
            val p1Id = farmDao.insertProject(
                Project(
                    name = "Pond A - Tilapia Culture",
                    pondName = "Pond #1 (North)",
                    pondSize = 50.0,
                    fishType = "Tilapia",
                    stockQuantity = 5000,
                    startDate = "2026-04-10",
                    estimatedHarvestDate = "2026-09-15",
                    investmentAmount = 120000.0,
                    status = "Active"
                )
            ).toInt()

            val p2Id = farmDao.insertProject(
                Project(
                    name = "Pond B - Rui Growing",
                    pondName = "Pond #2 (East)",
                    pondSize = 80.0,
                    fishType = "Rui (Carp)",
                    stockQuantity = 8000,
                    startDate = "2026-03-01",
                    estimatedHarvestDate = "2026-10-20",
                    investmentAmount = 250000.0,
                    status = "Active"
                )
            ).toInt()

            val p3Id = farmDao.insertProject(
                Project(
                    name = "Pond C - Catfish Harvest",
                    pondName = "Pond #3 (South)",
                    pondSize = 35.0,
                    fishType = "Catfish",
                    stockQuantity = 4000,
                    startDate = "2025-11-20",
                    estimatedHarvestDate = "2026-05-15",
                    investmentAmount = 90000.0,
                    status = "Completed"
                )
            ).toInt()

            // Seed Inventory
            farmDao.insertInventoryItem(
                InventoryItem(
                    itemName = "Floating Feed (Protein 32%)",
                    category = "Feed",
                    quantity = 45.0,
                    unit = "bags",
                    purchasePrice = 1850.0,
                    supplier = "Quality Feeds Ltd",
                    purchaseDate = "2026-05-15",
                    lowStockAlertLimit = 15.0
                )
            )

            farmDao.insertInventoryItem(
                InventoryItem(
                    itemName = "Aqua-Oxigen Tablets",
                    category = "Medicine",
                    quantity = 8.0,
                    unit = "pcs",
                    purchasePrice = 350.0,
                    supplier = "Square Pharma Animal",
                    purchaseDate = "2026-05-18",
                    lowStockAlertLimit = 10.0
                )
            )

            farmDao.insertInventoryItem(
                InventoryItem(
                    itemName = "Submersible Aerator Pump 2HP",
                    category = "Equipment",
                    quantity = 3.0,
                    unit = "pcs",
                    purchasePrice = 22000.0,
                    supplier = "Chittagong Machinery Store",
                    purchaseDate = "2026-04-05",
                    lowStockAlertLimit = 1.0
                )
            )

            // Seed Expenses
            farmDao.insertExpense(
                Expense(
                    title = "Opening Fingerlings Purchase",
                    category = "Miscellaneous",
                    amount = 30000.0,
                    date = "2026-04-12",
                    projectId = p1Id,
                    notes = "Tilapia fingerlings, 5000 units bought",
                    isApproved = true
                )
            )

            farmDao.insertExpense(
                Expense(
                    title = "20 Bags Feed Purchase",
                    category = "Feed Cost",
                    amount = 37000.0,
                    date = "2026-05-15",
                    projectId = p1Id,
                    notes = "Bought floating grower feed",
                    isApproved = true
                )
            )

            farmDao.insertExpense(
                Expense(
                    title = "Labor Wage (May)",
                    category = "Labor Cost",
                    amount = 15000.0,
                    date = "2026-05-25",
                    projectId = p1Id,
                    notes = "Monthly wage for Pond A caretaker",
                    isApproved = true
                )
            )

            farmDao.insertExpense(
                Expense(
                    title = "Pond Repair Work",
                    category = "Pond Maintenance",
                    amount = 12000.0,
                    date = "2026-05-26",
                    projectId = p1Id,
                    notes = "Embankment modification and net support",
                    isApproved = false
                )
            )

            farmDao.insertExpense(
                Expense(
                    title = "Fingerlings Seed - Pond B",
                    category = "Miscellaneous",
                    amount = 60000.0,
                    date = "2026-03-02",
                    projectId = p2Id,
                    notes = "Rui seed from local hatchery",
                    isApproved = true
                )
            )

            // Seed Sales
            farmDao.insertSale(
                Sale(
                    saleDate = "2026-05-10",
                    fishType = "Tilapia (Grade A)",
                    quantity = 1500,
                    weight = 1250.0,
                    price = 180.0,
                    totalPrice = 225000.0,
                    buyerName = "Kawran Bazar Wholesaler",
                    paymentMethod = "Cash",
                    invoiceNumber = "INV-2026-001",
                    projectId = p1Id
                )
            )

            farmDao.insertSale(
                Sale(
                    saleDate = "2026-05-28",
                    fishType = "Catfish (Completed Pond C)",
                    quantity = 3800,
                    weight = 1900.0,
                    price = 210.0,
                    totalPrice = 399000.0,
                    buyerName = "Mymensingh Fish Arat",
                    paymentMethod = "Bank",
                    invoiceNumber = "INV-2026-002",
                    projectId = p3Id
                )
            )

            // Seed Customers
            farmDao.insertCustomer(
                Customer(
                    name = "Kawran Bazar Wholesaler",
                    mobile = "01822334455",
                    address = "Kawran Bazar, Dhaka",
                    purchaseHistory = 4,
                    dueBalance = 25000.0,
                    notes = "Main fish merchant"
                )
            )

            farmDao.insertCustomer(
                Customer(
                    name = "Mymensingh Fish Arat",
                    mobile = "01911998877",
                    address = "Fish Market, Trishal",
                    purchaseHistory = 2,
                    dueBalance = 0.0,
                    notes = "Pays fully via bank transfer"
                )
            )

            // Seed Notifications
            farmDao.insertNotification(
                Notification(
                    title = "Low Medicine Inventory Alert",
                    message = "Aqua-Oxigen Tablets has 8.0 pcs. Only (Alert limit: 10.0). Reorder now!",
                    date = "2026-05-18",
                    type = "low_inventory"
                )
            )

            farmDao.insertNotification(
                Notification(
                    title = "New Unapproved Expense",
                    message = "Manager entered BDT 12,000 expense: Pond Repair Work. Approval pending.",
                    date = "2026-05-26",
                    type = "new_expense"
                )
            )

            farmDao.insertNotification(
                Notification(
                    title = "New Fish Growth Registered",
                    message = "Labeo rohita (Rui) in Pond B is showing sound average gain of 120 grams/month.",
                    date = "2026-05-29",
                    type = "harvest_reminder"
                )
            )
        }
    }
}
