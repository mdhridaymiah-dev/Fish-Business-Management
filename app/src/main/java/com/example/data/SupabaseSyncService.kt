package com.example.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface SupabaseSyncService {
    @POST("{table}")
    suspend fun upsertData(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body data: List<Map<String, Any?>>
    ): Response<Unit>

    @GET("{table}")
    suspend fun fetchData(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<List<Map<String, Any?>>>

    companion object {
        fun create(baseUrl: String): SupabaseSyncService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            // Safe fallback Base URL validation for Retrofit
            val finalUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            
            return Retrofit.Builder()
                .baseUrl(finalUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(SupabaseSyncService::class.java)
        }
    }
}

// Extension Utilities for safe mapping between Room Entities and Raw Maps
fun User.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "fullName" to fullName,
    "mobile" to mobile,
    "email" to email,
    "role" to role,
    "password" to password,
    "assignedProjectId" to assignedProjectId,
    "sharePercentage" to sharePercentage,
    "investmentAmount" to investmentAmount,
    "status" to status
)

fun Project.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "pondName" to pondName,
    "pondSize" to pondSize,
    "fishType" to fishType,
    "stockQuantity" to stockQuantity,
    "startDate" to startDate,
    "estimatedHarvestDate" to estimatedHarvestDate,
    "investmentAmount" to investmentAmount,
    "status" to status
)

fun InventoryItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "itemName" to itemName,
    "category" to category,
    "quantity" to quantity,
    "unit" to unit,
    "purchasePrice" to purchasePrice,
    "supplier" to supplier,
    "purchaseDate" to purchaseDate,
    "lowStockAlertLimit" to lowStockAlertLimit
)

fun Expense.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "category" to category,
    "amount" to amount,
    "date" to date,
    "projectId" to projectId,
    "notes" to notes,
    "isApproved" to isApproved,
    "requesterId" to requesterId,
    "isPettyCash" to isPettyCash,
    "approvedByShareholders" to approvedByShareholders,
    "approvalStatus" to approvalStatus
)

fun Sale.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "saleDate" to saleDate,
    "fishType" to fishType,
    "quantity" to quantity,
    "weight" to weight,
    "price" to price,
    "totalPrice" to totalPrice,
    "buyerName" to buyerName,
    "paymentMethod" to paymentMethod,
    "invoiceNumber" to invoiceNumber,
    "projectId" to projectId
)

fun Customer.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "mobile" to mobile,
    "address" to address,
    "purchaseHistory" to purchaseHistory,
    "dueBalance" to dueBalance,
    "notes" to notes
)

// Inbound entity parsers from JSON maps
fun Map<String, Any?>.toUser(): User = User(
    id = (this["id"] as? Number)?.toInt() ?: 0,
    fullName = this["fullName"] as? String ?: "",
    mobile = this["mobile"] as? String ?: "",
    email = this["email"] as? String ?: "",
    role = this["role"] as? String ?: "",
    password = this["password"] as? String ?: "",
    assignedProjectId = (this["assignedProjectId"] as? Number)?.toInt(),
    sharePercentage = (this["sharePercentage"] as? Number)?.toDouble() ?: 0.0,
    investmentAmount = (this["investmentAmount"] as? Number)?.toDouble() ?: 0.0,
    status = this["status"] as? String ?: "Active"
)

fun Map<String, Any?>.toProject(): Project = Project(
    id = (this["id"] as? Number)?.toInt() ?: 0,
    name = this["name"] as? String ?: "",
    pondName = this["pondName"] as? String ?: "",
    pondSize = (this["pondSize"] as? Number)?.toDouble() ?: 0.0,
    fishType = this["fishType"] as? String ?: "",
    stockQuantity = (this["stockQuantity"] as? Number)?.toInt() ?: 0,
    startDate = this["startDate"] as? String ?: "",
    estimatedHarvestDate = this["estimatedHarvestDate"] as? String ?: "",
    investmentAmount = (this["investmentAmount"] as? Number)?.toDouble() ?: 0.0,
    status = this["status"] as? String ?: "Active"
)

fun Map<String, Any?>.toInventoryItem(): InventoryItem = InventoryItem(
    id = (this["id"] as? Number)?.toInt() ?: 0,
    itemName = this["itemName"] as? String ?: "",
    category = this["category"] as? String ?: "",
    quantity = (this["quantity"] as? Number)?.toDouble() ?: 0.0,
    unit = this["unit"] as? String ?: "",
    purchasePrice = (this["purchasePrice"] as? Number)?.toDouble() ?: 0.0,
    supplier = this["supplier"] as? String ?: "",
    purchaseDate = this["purchaseDate"] as? String ?: "",
    lowStockAlertLimit = (this["lowStockAlertLimit"] as? Number)?.toDouble() ?: 0.0
)

fun Map<String, Any?>.toExpense(): Expense = Expense(
    id = (this["id"] as? Number)?.toInt() ?: 0,
    title = this["title"] as? String ?: "",
    category = this["category"] as? String ?: "",
    amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
    date = this["date"] as? String ?: "",
    projectId = (this["projectId"] as? Number)?.toInt() ?: 0,
    notes = this["notes"] as? String ?: "",
    isApproved = this["isApproved"] as? Boolean ?: false,
    attachmentUrl = this["attachmentUrl"] as? String,
    requesterId = (this["requesterId"] as? Number)?.toInt() ?: 0,
    isPettyCash = this["isPettyCash"] as? Boolean ?: false,
    approvedByShareholders = this["approvedByShareholders"] as? String ?: "",
    approvalStatus = this["approvalStatus"] as? String ?: "PENDING"
)

fun Map<String, Any?>.toSale(): Sale = Sale(
    id = (this["id"] as? Number)?.toInt() ?: 0,
    saleDate = this["saleDate"] as? String ?: "",
    fishType = this["fishType"] as? String ?: "",
    quantity = (this["quantity"] as? Number)?.toInt() ?: 0,
    weight = (this["weight"] as? Number)?.toDouble() ?: 0.0,
    price = (this["price"] as? Number)?.toDouble() ?: 0.0,
    totalPrice = (this["totalPrice"] as? Number)?.toDouble() ?: 0.0,
    buyerName = this["buyerName"] as? String ?: "",
    paymentMethod = this["paymentMethod"] as? String ?: "",
    invoiceNumber = this["invoiceNumber"] as? String ?: "",
    projectId = (this["projectId"] as? Number)?.toInt() ?: 0
)

fun Map<String, Any?>.toCustomer(): Customer = Customer(
    id = (this["id"] as? Number)?.toInt() ?: 0,
    name = this["name"] as? String ?: "",
    mobile = this["mobile"] as? String ?: "",
    address = this["address"] as? String ?: "",
    purchaseHistory = (this["purchaseHistory"] as? Number)?.toInt() ?: 0,
    dueBalance = (this["dueBalance"] as? Number)?.toDouble() ?: 0.0,
    notes = this["notes"] as? String ?: ""
)
