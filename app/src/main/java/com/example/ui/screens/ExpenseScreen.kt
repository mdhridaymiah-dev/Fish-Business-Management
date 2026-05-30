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
fun ExpensesScreen(viewModel: FarmViewModel, onAddExpense: () -> Unit) {
    val expenses by viewModel.allExpenses.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "SHAREHOLDER"
    val userProjId = currentUser?.assignedProjectId

    var showFormDialog by remember { mutableStateOf(false) }
    var selectedExpenseForEdit by remember { mutableStateOf<Expense?>(null) }

    var showRejectDialog by remember { mutableStateOf(false) }
    var expenseToReject by remember { mutableStateOf<Expense?>(null) }
    var isRejectByShareholder by remember { mutableStateOf(false) }
    
    // Admins see all expenses, others see their project-specific expenses.
    // If one user rejects, other users who have NOT approved should NOT see this request anymore
    val visibleExpenses = remember(expenses, userRole, userProjId, currentUser) {
        val activeUserVal = currentUser
        val baseList = if (userRole == "ADMIN") expenses
        else if (userProjId != null) expenses.filter { it.projectId == userProjId }
        else expenses

        baseList.filter { expense ->
            if (expense.approvalStatus == "REJECTED") {
                val hasApproved = activeUserVal != null && expense.approvedByShareholders.split(",").contains(activeUserVal.id.toString())
                val isRequester = activeUserVal != null && expense.requesterId == activeUserVal.id
                val isAdmin = userRole == "ADMIN"
                
                isAdmin || isRequester || hasApproved
            } else {
                true
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            // Admin, Manager, and Shareholder/Users can ALL request a purchase/petty-cash
            FloatingActionButton(
                onClick = {
                    selectedExpenseForEdit = null
                    showFormDialog = true
                },
                modifier = Modifier.testTag("add_expense_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
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
                text = viewModel.t("Expense Registry & Approvals", "ক্রয় রিকোয়েস্ট ও খরচ অনুমোদন ট্র্যাকার"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.t(
                    "Admin purchases require Shareholder approvals. Shareholders can request Petty Cash purchases. Once approved by all respective shareholders, it shows as an active cost entry.",
                    "প্রধান অ্যাডমিনের ক্রয়ের ক্ষেত্রে শেয়ারহোল্ডারদের অনুমোদন আবশ্যক। ইউজার আইডি থেকে পেটিক্যাশ আকারে রিকোয়েস্ট করা যাবে যা সকলের অনুমোদনের পর খরচ হিসেবে সাব্যস্ত হবে।"
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (visibleExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "No expenses",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.t("No recorded cost or purchase requests found.", "কোনো ব্যয় বা ক্রয় রিকোয়েস্ট পাওয়া যায়নি।"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(visibleExpenses) { expense ->
                        val projName = projects.find { it.id == expense.projectId }?.name ?: "Pond Project"
                        val requesterName = users.find { it.id == expense.requesterId }?.fullName ?: viewModel.t("Manager", "খামার ম্যানেজার")
                        
                        ExpenseCard(
                            expense = expense,
                            projectName = projName,
                            requesterName = requesterName,
                            currency = currency,
                            currentUser = currentUser,
                            users = users,
                            viewModel = viewModel,
                            onEdit = {
                                selectedExpenseForEdit = expense
                                showFormDialog = true
                            },
                            onApproveToggle = {
                                currentUser?.let { user ->
                                    val alreadyApproved = expense.approvedByShareholders
                                        .split(",")
                                        .contains(user.id.toString())
                                    viewModel.approveExpenseByShareholder(expense, user.id, !alreadyApproved)
                                }
                            },
                            onRejectToggle = {
                                expenseToReject = expense
                                isRejectByShareholder = true
                                showRejectDialog = true
                            },
                            onAdminDirectApprove = {
                                viewModel.approveExpense(expense)
                            },
                            onAdminReject = {
                                expenseToReject = expense
                                isRejectByShareholder = false
                                showRejectDialog = true
                            },
                            onDelete = {
                                viewModel.deleteExpense(expense)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        ExpenseFormDialog(
            expense = selectedExpenseForEdit,
            projects = projects,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false },
            onSave = { title, cat, amount, date, projId, notes, isPettyCashVal ->
                val requesterId = currentUser?.id ?: 0
                val updatedExpense = Expense(
                    id = selectedExpenseForEdit?.id ?: 0,
                    title = title,
                    category = cat,
                    amount = amount,
                    date = date,
                    projectId = projId,
                    notes = notes,
                    isPettyCash = isPettyCashVal,
                    requesterId = selectedExpenseForEdit?.requesterId ?: requesterId,
                    isApproved = false,
                    approvalStatus = "PENDING"
                )
                viewModel.saveExpense(updatedExpense) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }

    if (showRejectDialog && expenseToReject != null) {
        RejectionReasonDialog(
            viewModel = viewModel,
            onDismiss = {
                showRejectDialog = false
                expenseToReject = null
            },
            onConfirm = { reason ->
                val currentExp = expenseToReject!!
                if (isRejectByShareholder) {
                    currentUser?.let { user ->
                        viewModel.rejectExpenseByShareholder(currentExp, user.id, reason)
                    }
                } else {
                    viewModel.rejectExpense(currentExp, reason)
                }
                showRejectDialog = false
                expenseToReject = null
            }
        )
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    projectName: String,
    requesterName: String,
    currency: String,
    currentUser: User?,
    users: List<User>,
    viewModel: FarmViewModel,
    onEdit: () -> Unit,
    onApproveToggle: () -> Unit,
    onRejectToggle: () -> Unit,
    onAdminDirectApprove: () -> Unit,
    onAdminReject: () -> Unit,
    onDelete: () -> Unit
) {
    val projectShareholders = remember(users, expense.projectId) {
        users.filter { 
            it.role == "SHAREHOLDER" && it.status == "Active" && (expense.projectId == 0 || it.assignedProjectId == expense.projectId)
        }
    }
    
    val approverIds = remember(expense.approvedByShareholders) {
        expense.approvedByShareholders.split(",").filter { it.isNotEmpty() }.toSet()
    }
    val approversCount = approverIds.size
    val totalRequired = if (projectShareholders.isNotEmpty()) projectShareholders.size else 1
    val isUserShareholder = currentUser?.role == "SHAREHOLDER"
    val hasApprovedAlready = currentUser != null && approverIds.contains(currentUser.id.toString())

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
                    Text(
                        text = expense.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${viewModel.t("Pond Project", "পুকুর প্রজেক্ট")}: $projectName",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${viewModel.t("Requester", "আবেদনকারী")}: $requesterName" + 
                            if (expense.isPettyCash) " (${viewModel.t("Petty Cash", "পেটিক্যাশ রিকোয়েস্ট")})" else "",
                        fontSize = 11.sp,
                        color = Color(0xFF004D40)
                    )
                }

                // Interactive Dynamic Multi-Level Approval State pill
                val pillColor = when(expense.approvalStatus) {
                    "APPROVED" -> Color(0xFFE8F5E9)
                    "PARTIALLY_APPROVED" -> Color(0xFFE0F7FA)
                    "REJECTED" -> Color(0xFFFFEBEE)
                    else -> Color(0xFFFFF3E0)
                }
                val pillText = when(expense.approvalStatus) {
                    "APPROVED" -> viewModel.t("APPROVED", "অনুমোদিত")
                    "PARTIALLY_APPROVED" -> viewModel.t("PARTIALLY APPROVED", "আংশিক অনুমোদিত")
                    "REJECTED" -> viewModel.t("REJECTED", "প্রত্যাখ্যাত")
                    else -> viewModel.t("PENDING", "পেন্ডিং")
                }
                val textColor = when(expense.approvalStatus) {
                    "APPROVED" -> Color(0xFF2E7D32)
                    "PARTIALLY_APPROVED" -> Color(0xFF006064)
                    "REJECTED" -> Color(0xFFC62828)
                    else -> Color(0xFFE65100)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(pillColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$pillText ($approversCount/$totalRequired)",
                        color = textColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(viewModel.t("Category Tag", "খরচ খাত"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = when(expense.category) {
                            "Feed Cost" -> viewModel.t("Feed Cost", "খাদ্য ক্রয়")
                            "Medicine Cost" -> viewModel.t("Medicine Cost", "ওষুধ ও চুন ক্রয়")
                            "Labor Cost" -> viewModel.t("Labor Cost", "শ্রমিক মজুরি")
                            "Electricity" -> viewModel.t("Electricity", "বিদ্যুৎ বিল")
                            "Oxygen" -> viewModel.t("Oxygen", "অক্সিজেন সাপ্লাই")
                            "Pond Maintenance" -> viewModel.t("Pond Maintenance", "পুকুর সংস্কার")
                            else -> expense.category
                        }, 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 12.sp
                    )
                }
                Column {
                    Text(viewModel.t("Spent Amount", "খরচের পরিমাণ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${expense.amount}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column {
                    Text(viewModel.t("Date", "তারিখ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(expense.date, fontSize = 11.sp)
                }
            }

            if (expense.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${viewModel.t("Invoice details", "ইনভয়েস বিবরণী")}: ${expense.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (expense.approvalStatus == "REJECTED" && expense.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF16E63)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = viewModel.t("Rejection remarks / কারণ বিবরণী:", "প্রত্যাখ্যানের বিবরণ ও রিমার্কস:"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = expense.rejectionReason,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Quick approvals or editing actions
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shareholder interactive approval click trigger
                if (isUserShareholder) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onApproveToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasApprovedAlready) Color(0xFF2E7D32) else Color(0xFF006C6C)
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize().height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (hasApprovedAlready) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (hasApprovedAlready) viewModel.t("Approved ✅", "অনুমোদিত ✅") else viewModel.t("Approve", "অনুমোদন"),
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (expense.approvalStatus != "REJECTED" && !hasApprovedAlready) {
                            Button(
                                onClick = onRejectToggle,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE65100)
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize().height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = viewModel.t("Reject", "প্রত্যাখ্যান"),
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Button(
                                onClick = onDelete,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFC62828)
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize().height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = viewModel.t("Reject & Delete", "বাতিল ও মুছুন"),
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else if (currentUser?.role == "ADMIN" && !expense.isApproved) {
                    // Admin pending review of application
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onAdminDirectApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.minimumInteractiveComponentSize().height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(viewModel.t("Approve", "অনুমোদন"), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        if (expense.approvalStatus != "REJECTED") {
                            Button(
                                onClick = onAdminReject,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.minimumInteractiveComponentSize().height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(viewModel.t("Reject", "প্রত্যাখ্যান"), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.minimumInteractiveComponentSize().height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(viewModel.t("Reject & Delete", "বাতিল ও মুছুন"), fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentUser?.role == "ADMIN" || currentUser?.id == expense.requesterId) {
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Cost", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (currentUser?.role == "ADMIN" && expense.isApproved) {
                        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Cost", tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseFormDialog(
    expense: Expense?,
    projects: List<Project>,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, Int, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: "Feed Cost") }
    var amountStr by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(expense?.date ?: "") }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }
    var isPettyCash by remember { mutableStateOf(expense?.isPettyCash ?: false) }

    var selectedProjectId by remember { mutableStateOf(expense?.projectId ?: projects.firstOrNull()?.id ?: 0) }

    val categoriesList = listOf(
        "Feed Cost",
        "Medicine Cost",
        "Labor Cost",
        "Electricity",
        "Oxygen",
        "Pond Maintenance"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (expense == null) viewModel.t("Request New Purchase", "নতুন খরচের আবেদন করুন") else viewModel.t("Re-adjust Expense Ledger", "খরচের হিসাব এন্ট্রি পরিবর্তন"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(viewModel.t("Expense Title (eg. Tank Starter Feed)", "ব্যয় বিবরণী (যেমন: ফিড খাবার ক্রয়)")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isPettyCash,
                        onCheckedChange = { isPettyCash = it },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.t("Request as Petty Cash purchase?", "পেটিক্যাশ আকারে আবেদন করবেন?"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(viewModel.t("Select Target Pond Project", "সংশ্লিষ্ট পুকুর প্রজেক্ট নির্বাচন"))
                projects.forEach { proj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedProjectId == proj.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedProjectId = proj.id }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProjectId == proj.id,
                            onClick = { selectedProjectId = proj.id },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(proj.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(viewModel.t("Expense Category Sector", "ব্যয় খাত নির্ধারণ"))
                Box(modifier = Modifier.fillMaxWidth()) {
                    var expanded by remember { mutableStateOf(false) }
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(
                            text = when(category) {
                                "Feed Cost" -> viewModel.t("Feed Cost", "খাদ্য ক্রয়")
                                "Medicine Cost" -> viewModel.t("Medicine Cost", "ওষুধ ও চুন ক্রয়")
                                "Labor Cost" -> viewModel.t("Labor Cost", "শ্রমিক মজুরি")
                                "Electricity" -> viewModel.t("Electricity", "বিদ্যুৎ বিল")
                                "Oxygen" -> viewModel.t("Oxygen", "অক্সিজেন সাপ্লাই")
                                "Pond Maintenance" -> viewModel.t("Pond Maintenance", "পুকুর সংস্কার")
                                else -> category
                            }, 
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        when(cat) {
                                            "Feed Cost" -> viewModel.t("Feed Cost", "খাদ্য ক্রয়")
                                            "Medicine Cost" -> viewModel.t("Medicine Cost", "ওষুধ ও চুন ক্রয়")
                                            "Labor Cost" -> viewModel.t("Labor Cost", "শ্রমিক মজুরি")
                                            "Electricity" -> viewModel.t("Electricity", "বিদ্যুৎ বিল")
                                            "Oxygen" -> viewModel.t("Oxygen", "অক্সিজেন সাপ্লাই")
                                            "Pond Maintenance" -> viewModel.t("Pond Maintenance", "পুকুর সংস্কার")
                                            else -> cat
                                        }
                                    ) 
                                },
                                onClick = {
                                    category = cat
                                    expanded = false
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(viewModel.t("Amount", "টাকার পরিমাণ")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(viewModel.t("Date (YYYY-MM-DD)", "তারিখ (বছর-মাস-দিন)")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(viewModel.t("Cost Description / Voucher Info", "বিল ও মেমো ভাউচার প্রমাণ বিবরণ")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && selectedProjectId != 0 && date.isNotBlank()) {
                        onSave(title, category, amountVal, date, selectedProjectId, notes, isPettyCash)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Submit Request", "আবেদন জমা দিন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}

@Composable
fun RejectionReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    viewModel: FarmViewModel
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = viewModel.t("Enter Rejection Reason", "প্রত্যাখ্যানের সুনির্দিষ্ট কারণ লিখুন"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = viewModel.t(
                        "Please state why you are rejecting this request so that other users can understand the context in the remarks panel:",
                        "দয়া করে এই আবেদনটি প্রত্যাখ্যান বা বাতিল করার কারণটি লিখুন যাতে অন্যান্য ইউজার রিমার্কস প্যানেলে এর বিবরণ দেখতে পারেন:"
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(viewModel.t("Rejection Reason", "প্রত্যাখ্যানের কারণ / মন্তব্য")) },
                    placeholder = { Text(viewModel.t("e.g. Budget exceeded, incorrect information etc.", "যেমন: অতিরিক্ত বাজেট, ভুল তথ্য এন্ট্রি ইত্যাদি")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Confirm Reject", "প্রত্যাখ্যান নিশ্চিত করুন"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}
