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

    val currency = viewModel.currencySymbol.collectAsState().value
    val activeUserVal = currentUser
    val userRole = activeUserVal?.role ?: "SHAREHOLDER"
    
    var showFormDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var shareholderToCancel by remember { mutableStateOf<User?>(null) }

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
                val currentNotice = cancelRequests.find { it.shareholderId == user.id && it.status == "PENDING" }
                if (currentNotice != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = "Notice", tint = Color.Red)
                            Column {
                                Text(
                                    text = viewModel.t("OFFICIAL MEMBERSHIP CANCELLATION NOTICE", "সদস্যপদ বাতিলকরণের নোটিশ"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = viewModel.t(
                                        "An action is in progress to cancel your shareholder subscription. Shares will transfer to ${currentNotice.buyerName}.",
                                        "আপনার শেয়ারহোল্ডার বাতিল করার একটি রিকোয়েস্ট প্রক্রিয়াধীন রয়েছে। আপনার শেয়ারটি '${currentNotice.buyerName}' এর নিকট স্থানান্তরিত হবে।"
                                    ),
                                    fontSize = 11.sp,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                        }
                    }
                }
            }

            // Pending Cancel Requests block for other shareholders to approve
            val otherCancelRequests = cancelRequests.filter { 
                it.status == "PENDING" && it.shareholderId != activeUserVal?.id 
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
                                    Button(
                                        onClick = { viewModel.approveCancelRequest(req, currentUser?.id ?: 0) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(viewModel.t("Approve", "অনুমোদন দিন"), fontSize = 11.sp, color = Color.White)
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
        }
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
