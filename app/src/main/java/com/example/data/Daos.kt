package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    // --- Users ---
    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersDirect(): List<User>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    // --- Projects ---
    @Query("SELECT * FROM projects")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects")
    suspend fun getAllProjectsDirect(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    // --- Inventory ---
    @Query("SELECT * FROM inventory")
    fun getAllInventoryFlow(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    // --- Expenses ---
    @Query("SELECT * FROM expenses")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId")
    fun getExpensesForProjectFlow(projectId: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId")
    suspend fun getExpensesForProjectDirect(projectId: Int): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // --- Sales ---
    @Query("SELECT * FROM sales")
    fun getAllSalesFlow(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE projectId = :projectId")
    fun getSalesForProjectFlow(projectId: Int): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE projectId = :projectId")
    suspend fun getSalesForProjectDirect(projectId: Int): List<Sale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Update
    suspend fun updateSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)

    // --- Customers ---
    @Query("SELECT * FROM customers")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // --- Notifications ---
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotificationsFlow(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Delete
    suspend fun deleteNotification(notification: Notification)

    // --- Shareholder Cancel Requests ---
    @Query("SELECT * FROM shareholder_cancel_requests")
    fun getAllCancelRequestsFlow(): Flow<List<ShareholderCancelRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCancelRequest(request: ShareholderCancelRequest): Long

    @Update
    suspend fun updateCancelRequest(request: ShareholderCancelRequest)

    @Delete
    suspend fun deleteCancelRequest(request: ShareholderCancelRequest)
}
