package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val mobile: String,
    val email: String,
    val role: String, // "ADMIN", "MANAGER", "SHAREHOLDER"
    val password: String,
    val assignedProjectId: Int? = null,
    val sharePercentage: Double = 0.0,
    val investmentAmount: Double = 0.0,
    val status: String = "Active" // "Active", "Inactive"
) : Serializable

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pondName: String,
    val pondSize: Double, // in standard unit e.g. decimals / bigha / decimal / sq ft
    val fishType: String,
    val stockQuantity: Int,
    val startDate: String,
    val estimatedHarvestDate: String,
    val investmentAmount: Double,
    val status: String = "Active" // "Active", "Completed"
) : Serializable

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val category: String, // "Feed", "Medicine", "Equipment"
    val quantity: Double,
    val unit: String, // "kg", "pcs", "bags"
    val purchasePrice: Double,
    val supplier: String,
    val purchaseDate: String,
    val lowStockAlertLimit: Double
) : Serializable

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "Feed Cost", "Medicine Cost", "Labor Cost", "Electricity", "Oxygen", "Transportation", "Pond Maintenance", "Miscellaneous"
    val amount: Double,
    val date: String,
    val projectId: Int,
    val notes: String = "",
    val isApproved: Boolean = false, // Admin Expense Approval requirement
    val attachmentUrl: String? = null,
    // Expanded for Admin & User Petty Cash Shareholder Approvals and Partial Approvals
    val requesterId: Int = 0,
    val isPettyCash: Boolean = false,
    val approvedByShareholders: String = "", // Comma-separated list of shareholder user IDs who approved
    val approvalStatus: String = "PENDING" // "PENDING", "PARTIALLY_APPROVED", "APPROVED", "REJECTED"
) : Serializable

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleDate: String,
    val fishType: String,
    val quantity: Int,
    val weight: Double, // in kg
    val price: Double, // price per kg
    val totalPrice: Double, // quantity or weight * price. In fish business sales: weight * price
    val buyerName: String,
    val paymentMethod: String, // "Cash", "Bank", "Bkash", "Nagad"
    val invoiceNumber: String,
    val projectId: Int
) : Serializable

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mobile: String,
    val address: String,
    val purchaseHistory: Int = 0, // number of purchases
    val dueBalance: Double = 0.0,
    val notes: String = ""
) : Serializable

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val date: String,
    val type: String, // "low_inventory", "new_expense", "new_sale", "harvest_reminder", "profit_update"
    val isRead: Boolean = false
) : Serializable

@Entity(tableName = "shareholder_cancel_requests")
data class ShareholderCancelRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shareholderId: Int,
    val shareholderName: String,
    val sharePercentage: Double,
    val investmentAmount: Double,
    val buyerType: String, // "EXISTING_SHAREHOLDER", "PROJECT", "NEW_SHAREHOLDER"
    val buyerShareholderId: Int? = null, // If existing shareholder buys
    val buyerName: String, // Name of buyer (or "Project / Farm", or new user name)
    val buyerMobile: String? = null,
    val buyerEmail: String? = null,
    val buyerPassword: String? = null,
    val approvedUserIds: String = "", // Comma-separated user IDs who approved (except the canceled one)
    val status: String = "PENDING" // "PENDING", "APPROVED", "REJECTED"
) : Serializable
