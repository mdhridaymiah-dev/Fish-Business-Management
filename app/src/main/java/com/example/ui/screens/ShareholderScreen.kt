package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareholdersScreen(viewModel: FarmViewModel, onAddUser: () -> Unit) {
    val users by viewModel.allUsers.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val sales by viewModel.allSales.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val cancelRequests by viewModel.allCancelRequests.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val activeUserVal = currentUser
    val userRole = activeUserVal?.role ?: "SHAREHOLDER"
    
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var shareholderToCancel by remember { mutableStateOf<User?>(null) }

    var showRejectRequestDialog by remember { mutableStateOf(false) }
    var requestToReject by remember { mutableStateOf<ShareholderCancelRequest?>(null) }

    var activeSubTab by remember { mutableStateOf("ledger") }
    var showRaiseObjectionDialog by remember { mutableStateOf(false) }
    var showReplyDialog by remember { mutableStateOf(false) }
    var objectionToReply by remember { mutableStateOf<Objection?>(null) }

    // Active shareholders list
    val activeShareholders = remember(users) {
        users.filter { it.role == "SHAREHOLDER" && it.status == "Active" }
    }

    val scopedShareholders = remember(activeShareholders, activeUserVal, userRole) {
        if (userRole == "ADMIN") activeShareholders
        else if (activeUserVal?.assignedProjectId != null) {
            activeShareholders.filter { it.assignedProjectId == activeUserVal.assignedProjectId }
        } else activeShareholders
    }

    Scaffold(
        floatingActionButton = {
            if (userRole == "ADMIN") {
                FloatingActionButton(
                    onClick = {
                        selectedUserForEdit = null
                        showFormDialog = true
                    },
                    modifier = Modifier.testTag("add_shareholder_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Shareholder")
                }
            }
        }
    ) { paddingVal ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVal)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = viewModel.t("Shareholder Register & Equity Dividends", "শেয়ারহোল্ডার পার্টনার তালিকা ও বিনিয়োগ"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.t(
                    "Observe equity splits, initial seed capitals, and automated proportional profit allocation shares directly bilingually.",
                    "অংশীদারদের মূলধন, অংশীদারিত্ব বা শেয়ারের হার এবং খামারের নিট লাভ বন্টন বিবরণ দেখুন।"
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Dynamic Share Summary Card bilingually mapped
            ShareSummaryMetricsCard(
                shareholders = scopedShareholders,
                projects = projects,
                currency = currency,
                viewModel = viewModel
            )

            // Notice panel if the logged-in user is currently queued for cancellation
            currentUser?.let { user ->
                val currentNotice = cancelRequests.find { it.shareholderId == user.id && (it.status == "PENDING" || it.status == "REJECTED") }
                if (currentNotice != null) {
                    val isRejected = currentNotice.status == "REJECTED"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isRejected) Color(0xFFFFF3E0) else Color(0xFFFFCDD2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isRejected) Icons.Default.Cancel else Icons.Default.Warning,
                                contentDescription = "Notice",
                                tint = if (isRejected) Color(0xFFE81111) else Color.Red
                            )
                            Column {
                                Text(
                                    text = if (isRejected) {
                                        viewModel.t("MEMBERSHIP CANCELLATION PROPOSAL REJECTED", "সদস্যপদ বাতিল প্রস্তাব প্রত্যাখ্যান করা হয়েছে")
                                    } else {
                                        viewModel.t("OFFICIAL MEMBERSHIP CANCELLATION NOTICE", "সদস্যপদ বাতিলকরণের নোটিশ")
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isRejected) Color(0xFFB71C1C) else Color(0xFFC62828)
                                )
                                Text(
                                    text = if (isRejected) {
                                        viewModel.t(
                                            "Your cancellation request was rejected. Details:\n${currentNotice.rejectionReason}",
                                            "আপনার সদস্যপদ বাতিলের আবেদনটি বাতিল করা হয়েছে। বিস্তারিত তথ্য নিচে দেওয়া হলো:\n${currentNotice.rejectionReason}"
                                        )
                                    } else {
                                        viewModel.t(
                                            "An action is in progress to cancel your shareholder subscription. Shares will transfer to ${currentNotice.buyerName}.",
                                            "আপনার শেয়ারহোল্ডার বাতিল করার একটি রিকোয়েস্ট প্রক্রিয়াধীন রয়েছে। আপনার শেয়ারটি '${currentNotice.buyerName}' এর নিকট স্থানান্তরিত হবে।"
                                        )
                                    },
                                    fontSize = 11.sp,
                                    color = if (isRejected) Color.DarkGray else Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            }

            // Pending Cancel Requests block for other shareholders to approve
            val otherCancelRequests = cancelRequests.filter { req ->
                if (req.status != "PENDING" || req.shareholderId == activeUserVal?.id) {
                    false
                } else if (userRole == "ADMIN") {
                    true
                } else {
                    val targetUser = users.find { it.id == req.shareholderId }
                    targetUser != null && activeUserVal != null && targetUser.assignedProjectId == activeUserVal.assignedProjectId
                }
            }
            if (otherCancelRequests.isNotEmpty()) {
                Text(
                    text = viewModel.t("Action Approvals Required", "অনুমোদন প্রত্যাশী প্রস্তাবসমূহ"),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.Red
                )
                otherCancelRequests.forEach { req ->
                    val approvedList = req.approvedUserIds.split(",").filter { it.isNotEmpty() }
                    val hasApproved = activeUserVal != null && approvedList.contains(activeUserVal.id.toString())
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = viewModel.t("Membership Cancel & Transfer", "সদস্যপদ বাতিল ও শেয়ার হস্তান্তর"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF8F00)
                                    )
                                    Text(
                                        text = "${viewModel.t("Resigning", "বাতিল গ্রাহক")}: ${req.shareholderName} (${req.sharePercentage}%)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${viewModel.t("Transferee Buyer", "হস্তান্তর ক্রেতা")}: ${req.buyerName} (${req.buyerType})",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                if (userRole == "SHAREHOLDER" && !hasApproved) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.approveCancelRequest(req, activeUserVal?.id ?: 0) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(viewModel.t("Approve", "অনুমোদন"), fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = {
                                                requestToReject = req
                                                showRejectRequestDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(viewModel.t("Reject", "প্রত্যাখ্যান"), fontSize = 10.sp, color = Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.deleteCancelRequest(req) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(viewModel.t("Reject & Delete", "বাতিল ও মুছুন"), fontSize = 10.sp, color = Color.White)
                                        }
                                    }
                                } else if (hasApproved) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(viewModel.t("Approved ✅", "অনুমোদিত ✅"), fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val objections by viewModel.allObjections.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabButton(
                    text = viewModel.t("Capital Ledger", "শেয়ারহোল্ডার পার্টনার তালিকা"),
                    selected = activeSubTab == "ledger",
                    onClick = { activeSubTab = "ledger" },
                    modifier = Modifier.weight(1.3f)
                )
                TabButton(
                    text = viewModel.t("Objections (${objections.size})", "তথ্য গরমিল আপত্তি (${objections.size})"),
                    selected = activeSubTab == "objections",
                    onClick = { activeSubTab = "objections" },
                    modifier = Modifier.weight(1.3f)
                )
            }

            if (activeSubTab == "ledger") {
                if (scopedShareholders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = viewModel.t("No active shareholders seeded or found.", "কোনো শেয়ারহোল্ডার রেকর্ড পাওয়া যায়নি।"),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = viewModel.t("Individual Holdings Ledger", "শেয়ারহোল্ডার ক্যাপিটাল লেজার"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(scopedShareholders) { partner ->
                            ShareholderItemCard(
                                partner = partner,
                                projects = projects,
                                sales = sales,
                                expenses = expenses,
                                currency = currency,
                                userRole = userRole,
                                viewModel = viewModel,
                                onEdit = {
                                    selectedUserForEdit = partner
                                    showFormDialog = true
                                },
                                onCancelRequest = {
                                    shareholderToCancel = partner
                                    showCancelDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteUser(partner)
                                }
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.t("Public Objections Ledger", "তথ্য গরমিল আপত্তি ও সমাধান"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (userRole == "SHAREHOLDER") {
                        Button(
                            onClick = { showRaiseObjectionDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.NewReleases, contentDescription = "Raise Objection", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(viewModel.t("Raise Objection", "আপত্তি জানান"), fontSize = 11.sp)
                        }
                    }
                }

                if (objections.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Celebration, contentDescription = "No Objections", tint = Color.Gray, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(viewModel.t("No active discrepancy objections raised. Excellent parity!", "কোন গরমিল বা আপত্তি পাওয়া যায়নি। সকল তথ্য সঠিক আছে।"), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(objections) { objection ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = objection.projectName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${viewModel.t("By:", "দাখিলকারী:")} ${objection.shareholderName} | ${objection.date}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }

                                        val (statusText, statusBg, statusColor) = when (objection.status) {
                                            "PENDING" -> Triple(viewModel.t("Pending Objection", "অনিষ্পন্ন আপত্তি"), Color(0xFFFFEBEE), Color(0xFFC62828))
                                            "REPLIED" -> Triple(viewModel.t("Admin Replied", "জবাব দেওয়া হয়েছে"), Color(0xFFE3F2FD), Color(0xFF1565C0))
                                            else -> Triple(viewModel.t("Resolved ✅", "নিষ্পত্তি হয়েছে ✅"), Color(0xFFE8F5E9), Color(0xFF2E7D32))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = objection.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = objection.description,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (objection.referenceType.isNotBlank()) {
                                        Text(
                                            text = "${viewModel.t("Category:", "গরমিল বিভাগ:")} ${objection.referenceType}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    objection.adminReply?.let { reply ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFF9C4))
                                                .padding(10.dp)
                                        ) {
                                            Text(viewModel.t("Admin clarification response:", "এডমিনের স্পষ্টীকরণ জবাব:"), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFF57F17))
                                            Text(reply, fontSize = 12.sp, color = Color.Black)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons depending on status and role
                                    if (userRole == "ADMIN" && objection.status == "PENDING") {
                                        Button(
                                            onClick = {
                                                objectionToReply = objection
                                                showReplyDialog = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(viewModel.t("Reply Clarification", "জবাব প্রদান করুন"), fontSize = 11.sp, color = Color.White)
                                        }
                                    }

                                    val isCreator = activeUserVal != null && activeUserVal.id == objection.shareholderId
                                    if (isCreator && objection.status == "REPLIED") {
                                        Button(
                                            onClick = {
                                                viewModel.resolveObjection(objection)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text(viewModel.t("Confirm & Settle Objection", "নিষ্পত্তি নিশ্চিত করুন"), fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRaiseObjectionDialog) {
        RaiseObjectionDialog(
            projects = projects,
            viewModel = viewModel,
            onDismiss = { showRaiseObjectionDialog = false },
            onSave = { projId, projName, titleText, descText, refT ->
                viewModel.raiseObjection(projId, projName, titleText, descText, refT)
                showRaiseObjectionDialog = false
            }
        )
    }

    if (showReplyDialog && objectionToReply != null) {
        ObjectionReplyDialog(
            objection = objectionToReply!!,
            viewModel = viewModel,
            onDismiss = {
                showReplyDialog = false
                objectionToReply = null
            },
            onConfirm = { replyTxt ->
                viewModel.replyToObjection(objectionToReply!!, replyTxt)
                showReplyDialog = false
                objectionToReply = null
            }
        )
    }

    if (showFormDialog) {
        ShareholderFormDialog(
            partner = selectedUserForEdit,
            projects = projects,
            users = users,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false },
            onSave = { name, email, mobile, password, projId, percentage, invest, statusMsg ->
                val updatedUser = User(
                    id = selectedUserForEdit?.id ?: 0,
                    fullName = name,
                    email = email,
                    mobile = mobile,
                    password = password,
                    role = "SHAREHOLDER",
                    assignedProjectId = projId,
                    sharePercentage = percentage,
                    investmentAmount = invest,
                    status = statusMsg
                )
                viewModel.saveUser(updatedUser) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }

    if (showCancelDialog && shareholderToCancel != null) {
        ShareholderCancelDialog(
            shareholder = shareholderToCancel!!,
            activeShareholders = activeShareholders.filter { it.id != shareholderToCancel!!.id },
            viewModel = viewModel,
            onDismiss = { showCancelDialog = false },
            onConfirm = { type, buyerShareId, buyerName, bMobile, bEmail, bPassword ->
                viewModel.createCancelRequest(
                    shareholderId = shareholderToCancel!!.id,
                    buyerType = type,
                    buyerShareholderId = buyerShareId,
                    buyerName = buyerName,
                    buyerMobile = bMobile,
                    buyerEmail = bEmail,
                    buyerPassword = bPassword
                ) { success ->
                    if (success) showCancelDialog = false
                }
            }
        )
    }

    if (showRejectRequestDialog && requestToReject != null) {
        RejectionReasonDialog(
            viewModel = viewModel,
            onDismiss = {
                showRejectRequestDialog = false
                requestToReject = null
            },
            onConfirm = { reason ->
                viewModel.rejectCancelRequest(requestToReject!!, activeUserVal?.id ?: 0, reason)
                showRejectRequestDialog = false
                requestToReject = null
            }
        )
    }
}

@Composable
fun ShareSummaryMetricsCard(
    shareholders: List<User>,
    projects: List<Project>,
    currency: String,
    viewModel: FarmViewModel
) {
    val totalEquityCapital = shareholders.sumOf { it.investmentAmount }
    val averagePartnership = shareholders.size

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(viewModel.t("Total Equity Capital Funding", "মোট অংশীদারি মূলধন"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$currency $totalEquityCapital", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(viewModel.t("Distributed between $averagePartnership partners", "মোট $averagePartnership জন শেয়ারহোল্ডার পার্টনার"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Icon(
                imageVector = Icons.Default.Wallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun ShareholderItemCard(
    partner: User,
    projects: List<Project>,
    sales: List<Sale>,
    expenses: List<Expense>,
    currency: String,
    userRole: String,
    viewModel: FarmViewModel,
    onEdit: () -> Unit,
    onCancelRequest: () -> Unit,
    onDelete: () -> Unit
) {
    val projectAssigned = remember(projects, partner.assignedProjectId) {
        projects.find { it.id == partner.assignedProjectId }
    }

    val profitCalculations = remember(sales, expenses, partner) {
        val projId = partner.assignedProjectId
        if (projId != null) {
            val projSales = sales.filter { it.projectId == projId }.sumOf { it.totalPrice }
            val approvedProjExpenses = expenses.filter { it.projectId == projId && it.isApproved }.sumOf { it.amount }
            val netProfit = projSales - approvedProjExpenses
            val shareSlice = netProfit * (partner.sharePercentage / 100.0)
            shareSlice
        } else {
            0.0
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = partner.fullName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (partner.status == "Active") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (partner.status == "Active") viewModel.t("Active", "সক্রিয়") else viewModel.t("Inactive", "স্থগিত"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (partner.status == "Active") Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                    Text(
                        text = "${viewModel.t("Contact", "যোগাযোগ")}: ${partner.mobile} | ${partner.email}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (userRole == "ADMIN") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onCancelRequest,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.padding(end = 4.dp).minimumInteractiveComponentSize()
                        ) {
                            Text(viewModel.t("Cancel Member", "বাতিল এবং ট্রান্সফার"), fontSize = 10.sp, color = Color.White)
                        }
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Share", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(viewModel.t("Pond Project Assigned", "নিযুক্ত পুকুর প্রজেক্ট"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(projectAssigned?.name ?: viewModel.t("No proj assigned", "নিযুক্ত নয়"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(viewModel.t("Equity Ratio %", "শেয়ার অনুপাত"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${partner.sharePercentage}%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(viewModel.t("Total Capital Funded", "বিনিয়োগকৃত মোট মূলধন"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${partner.investmentAmount}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(viewModel.t("Real-Time Net Dividend", "আয় বন্টন অনুপাত"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val color = if (profitCalculations >= 0.0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    Text("$currency ${"%.1f".format(profitCalculations)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
                }
            }
        }
    }
}

@Composable
fun ShareholderCancelDialog(
    shareholder: User,
    activeShareholders: List<User>,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, String, String?, String?, String?) -> Unit
) {
    var buyerType by remember { mutableStateOf("EXISTING_SHAREHOLDER") } // EXISTING_SHAREHOLDER, PROJECT, NEW_SHAREHOLDER
    var selectedBuyerId by remember { mutableStateOf<Int?>(activeShareholders.firstOrNull()?.id) }
    
    // New shareholder inputs
    var newName by remember { mutableStateOf("") }
    var newMobile by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(viewModel.t("Initiate Share Transfer & Cancel", "সদস্যপদ বাতিল ও শেয়ার হস্তান্তর"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${viewModel.t("Canceling Partner", "স্থগিত অংশীদার")}: ${shareholder.fullName} (${shareholder.sharePercentage}%)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                
                Text(viewModel.t("Who is buying/absorbing the shares?", "শেয়ারটি কে ক্রয়/গ্রহণ করছে?"), fontWeight = FontWeight.Bold)
                
                listOf(
                    "EXISTING_SHAREHOLDER" to viewModel.t("Existing Partner", "বিদ্যমান অংশীদার"),
                    "PROJECT" to viewModel.t("Venture Project Reserve", "প্রজেক্টের নিজস্ব তহবিল"),
                    "NEW_SHAREHOLDER" to viewModel.t("New Enrolling Shareholder", "নতুন শেয়ারহোল্ডার পার্টনার")
                ).forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { buyerType = type }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = buyerType == type, onClick = { buyerType = type })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 12.sp)
                    }
                }

                if (buyerType == "EXISTING_SHAREHOLDER" && activeShareholders.isNotEmpty()) {
                    Text(viewModel.t("Select purchasing partner", "ক্রেতা নির্বাচন করুন"))
                    activeShareholders.forEach { part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (selectedBuyerId == part.id) Color(0xFFCCE8E8) else Color.Transparent)
                                .clickable { selectedBuyerId = part.id }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedBuyerId == part.id, onClick = { selectedBuyerId = part.id })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(part.fullName, fontSize = 12.sp)
                        }
                    }
                }

                if (buyerType == "NEW_SHAREHOLDER") {
                    Text(viewModel.t("Provide New Shareholder credentials", "নতুন শেয়ারহোল্ডারের বিবরণী প্রদান করুন"))
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(viewModel.t("New Partner Name", "নাম")) }, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize())
                    OutlinedTextField(value = newMobile, onValueChange = { newMobile = it }, label = { Text(viewModel.t("Mobile", "মোবাইল")) }, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize())
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text(viewModel.t("Email", "ইমেইল")) }, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize())
                    OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text(viewModel.t("Local Login Password", "লগইন পাসওয়ার্ড")) }, modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buyerName = when(buyerType) {
                        "EXISTING_SHAREHOLDER" -> activeShareholders.find { it.id == selectedBuyerId }?.fullName ?: "Existing Partner"
                        "PROJECT" -> viewModel.t("Project Asset Reserve", "খামার নিজস্ব প্রকল্প")
                        "NEW_SHAREHOLDER" -> newName
                        else -> ""
                    }
                    onConfirm(buyerType, selectedBuyerId, buyerName, newMobile, newEmail, newPassword)
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Issue Proposal Notice", "বাতিল প্রস্তাব পেশ করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Discard", "বাতিল"))
            }
        }
    )
}

@Composable
fun ShareholderFormDialog(
    partner: User?,
    projects: List<Project>,
    users: List<User>,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int?, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(partner?.fullName ?: "") }
    var email by remember { mutableStateOf(partner?.email ?: "") }
    var mobile by remember { mutableStateOf(partner?.mobile ?: "") }
    var password by remember { mutableStateOf(partner?.password ?: "1234") }
    var percentageStr by remember { mutableStateOf(partner?.sharePercentage?.toString() ?: "") }
    var investStr by remember { mutableStateOf(partner?.investmentAmount?.toString() ?: "") }
    var status by remember { mutableStateOf(partner?.status ?: "Active") }

    var selectedProjId by remember { mutableStateOf(partner?.assignedProjectId ?: projects.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (partner == null) viewModel.t("Enroll New Partner Shareholder", "নতুন অংশীদার শেয়ারহোল্ডার নথিভুক্ত করুন") else viewModel.t("Re-adjust Partnership Split", "অংশীদারিত্ব সমন্বয় করুন"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(viewModel.t("Full Name", "নাম")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(viewModel.t("Email Address", "ইমেইল")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text(viewModel.t("Mobile", "মোবাইল")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(viewModel.t("Local Login Password", "লগইন পাসওয়ার্ড")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                Text(viewModel.t("Assign Associated Culture Pond Project", "সংশ্লিষ্ট পুকুর প্রজেক্ট বরাদ্দ করুন"))
                projects.forEach { proj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedProjId == proj.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedProjId = proj.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProjId == proj.id,
                            onClick = { selectedProjId = proj.id },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(proj.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = percentageStr,
                        onValueChange = { percentageStr = it },
                        label = { Text(viewModel.t("Equity Ratio %", "শেয়ার অংশ অনুপাত %")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = investStr,
                        onValueChange = { investStr = it },
                        label = { Text(viewModel.t("Capital Fund BDT", "বিনিয়োগকৃত টাকা BDT")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.t("Partner Account Status", "অংশীদার একাউন্ট স্ট্যাটাস"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status == "Active",
                            onClick = { status = "Active" },
                            label = { Text(viewModel.t("Active", "সক্রিয়")) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        FilterChip(
                            selected = status == "Inactive",
                            onClick = { status = "Inactive" },
                            label = { Text(viewModel.t("Inactive", "স্থগিত")) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val percentVal = percentageStr.toDoubleOrNull() ?: 0.0
                    val investVal = investStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onSave(name, email, mobile, password, selectedProjId, percentVal, investVal, status)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Save Partner info", "তথ্যভুক্ত করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Abort", "বাতিল"))
            }
        }
    )
}

@Composable
fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(40.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun RaiseObjectionDialog(
    projects: List<Project>,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (Int, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedProjId by remember { mutableStateOf(projects.firstOrNull()?.id ?: 0) }
    var refType by remember { mutableStateOf("GENERAL") } // "GENERAL", "SALE", "EXPENSE"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(viewModel.t("Raise Discrepancy Objection", "তথ্য গরমিল আপত্তি জানান"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(viewModel.t("Which project has discrepancy?", "কোন প্রজেক্টের তথ্যে গরমিল রয়েছে?"))
                projects.forEach { proj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedProjId == proj.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedProjId = proj.id }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProjId == proj.id,
                            onClick = { selectedProjId = proj.id }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(proj.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(viewModel.t("Objection Topic (eg. Feed calculation, Fish Weight)", "আপত্তির বিষয় বা টাইটেল")) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(viewModel.t("Provide detailed explanation", "বিস্তারিত গরমিল বিবরণ")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Text(viewModel.t("Related Topic Category", "সম্পর্কিত বিষয়"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GENERAL", "SALE", "EXPENSE").forEach { cat ->
                        FilterChip(
                            selected = refType == cat,
                            onClick = { refType = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val projName = projects.find { it.id == selectedProjId }?.name ?: "Unknown"
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSave(selectedProjId, projName, title, description, refType)
                    }
                }
            ) {
                Text(viewModel.t("Submit Objection", "আপত্তি দাখিল করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}

@Composable
fun ObjectionReplyDialog(
    objection: Objection,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(viewModel.t("Admin Response / Clarification", "আপত্তির স্পষ্টীকরণ ও জবাব দিন"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${viewModel.t("Topic:", "আপত্তির বিষয়:")} ${objection.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${viewModel.t("Description:", "অভিযোগকারী বর্ণনা:")} ${objection.description}",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text(viewModel.t("Type response reply details", "সমাধানমূলক মন্তব্য বা জবাব লিখুন")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (replyText.isNotBlank()) {
                        onConfirm(replyText)
                    }
                }
            ) {
                Text(viewModel.t("Submit Reply", "জবাব প্রদান করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}
