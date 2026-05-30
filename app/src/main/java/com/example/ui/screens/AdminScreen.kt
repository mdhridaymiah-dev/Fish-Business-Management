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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: FarmViewModel, onAddUser: () -> Unit) {
    val users by viewModel.allUsers.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    var showUserFormDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }
    var activeAdminTab by remember { mutableStateOf("Enroll User") } // Tab Options: Enroll User, Expense Approval, Audit Logs

    var showRejectDialog by remember { mutableStateOf(false) }
    var expenseToReject by remember { mutableStateOf<Expense?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Admin control Console",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Configure user roles, authorize pending expense requests, and view system activity parameters.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Triple tab toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Enroll User", "Expense Approval", "Audit Logs").forEach { tab ->
                Button(
                    onClick = { activeAdminTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeAdminTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeAdminTab == tab) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Divider()

        // Tab views rendering
        when (activeAdminTab) {
            "Enroll User" -> {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enrolled Team profiles", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Button(
                            onClick = {
                                selectedUserForEdit = null
                                showUserFormDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create User Profile", fontSize = 11.sp)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(users) { usr ->
                            UserEnrollCard(
                                user = usr,
                                projects = projects,
                                onEdit = {
                                    selectedUserForEdit = usr
                                    showUserFormDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteUser(usr)
                                }
                            )
                        }
                    }
                }
            }

            "Expense Approval" -> {
                val pendingExpenses = remember(expenses) {
                    expenses.filter { !it.isApproved }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Cost Requests Pending Authorization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    if (pendingExpenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(54.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("All expenditures are approved. Stable metrics.", fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(pendingExpenses) { exp ->
                                val projName = projects.find { it.id == exp.projectId }?.name ?: "Unknown Pond"
                                PendingExpenseApprovalRow(
                                    expense = exp,
                                    projectName = projName,
                                    onApprove = {
                                        viewModel.approveExpense(exp)
                                    },
                                    onReject = {
                                        expenseToReject = exp
                                        showRejectDialog = true
                                    },
                                    onDelete = {
                                        viewModel.deleteExpense(exp)
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            "Audit Logs" -> {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active System Transactions Logging", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { viewModel.logActivity("System diagnostic check manually triggered.") }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Refresh, contentDescription = "Manual trigger refresh logging")
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(activityLogs) { log ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUserFormDialog) {
        UserFormDialog(
            user = selectedUserForEdit,
            projects = projects,
            viewModel = viewModel,
            onDismiss = { showUserFormDialog = false },
            onSave = { name, mobile, email, password, role, assignedProj, sharePercent, status ->
                val record = User(
                    id = selectedUserForEdit?.id ?: 0,
                    fullName = name,
                    mobile = mobile,
                    email = email,
                    password = password,
                    role = role,
                    assignedProjectId = assignedProj,
                    sharePercentage = sharePercent,
                    status = status
                )
                viewModel.saveUser(record) { success ->
                    if (success) showUserFormDialog = false
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
                viewModel.rejectExpense(expenseToReject!!, reason)
                showRejectDialog = false
                expenseToReject = null
            }
        )
    }
}

@Composable
fun UserEnrollCard(
    user: User,
    projects: List<Project>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val assignedProjName = remember(projects, user.assignedProjectId) {
        projects.find { it.id == user.assignedProjectId }?.name ?: "All Pond Operations (Root Access)"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Role Option: ${user.role} | Status: ${user.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Text("Login ID email: ${user.email} | Mobile: ${user.mobile}", fontSize = 11.sp)
            Text("Operation Scope: $assignedProjName", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            if (user.role == "SHAREHOLDER") {
                Text("Assigned Partnership Split: ${user.sharePercentage}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PendingExpenseApprovalRow(
    expense: Expense,
    projectName: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit,
    viewModel: com.example.ui.FarmViewModel
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${viewModel.t("Category", "খাত")}: ${expense.category} | ${viewModel.t("Pond", "পুকুর")}: $projectName", fontSize = 11.sp)
                    Text("${viewModel.t("Amount", "পরিমাণ")}: BDT ${expense.amount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(viewModel.t("Approve", "অনুমোদন"), fontSize = 9.sp, color = Color.White)
                }
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.t("Reject", "বাতিল"), fontSize = 9.sp, color = Color.White)
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.weight(1.3f).height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.t("Reject & Delete", "বাতিল ও মুছুন"), fontSize = 9.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun UserFormDialog(
    user: User?,
    projects: List<Project>,
    viewModel: com.example.ui.FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Int?, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(user?.fullName ?: "") }
    var mobile by remember { mutableStateOf(user?.mobile ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf(user?.password ?: "123456") }
    var role by remember { mutableStateOf(user?.role ?: "SHAREHOLDER") }
    var selectedProjId by remember { mutableStateOf(user?.assignedProjectId) }
    var sharePercentageStr by remember { mutableStateOf(user?.sharePercentage?.toString() ?: "0.0") }
    var status by remember { mutableStateOf(user?.status ?: "Active") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (user == null) viewModel.t("Log New User Profile", "নতুন অ্যাকাউন্ট তৈরি করুন") else viewModel.t("Adjust Profile configurations", "অ্যাকাউন্ট পরিবর্তন করুন"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(viewModel.t("Full Profile Name", "পূর্ণ নাম")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(viewModel.t("Login Email", "ইমেইল")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text(viewModel.t("Mobile Contact", "মোবাইল নম্বর")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(viewModel.t("Login Password", "পাসওয়ার্ড")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                // Role Options selector
                Text(viewModel.t("Assign Role Mode", "রোল বা দায়িত্ব নির্ধারণ করুন"), modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("MANAGER", "SHAREHOLDER").forEach { rOption ->
                        FilterChip(
                            selected = role == rOption,
                            onClick = { role = rOption },
                            label = { Text(viewModel.t(rOption, if(rOption == "ADMIN") "এডমিন" else if(rOption == "MANAGER") "ম্যানেজার" else "শেয়ারহোল্ডার"), fontSize = 9.sp) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }

                if (role != "ADMIN") {
                    Text(viewModel.t("Assign or Transfer Project/Pond:", "নির্ধারিত প্রজেক্ট নির্ধারণ বা অন্য প্রজেক্টে স্থানান্তর করুন:"))
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
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProjId == proj.id,
                                onClick = { selectedProjId = proj.id },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(proj.name, fontSize = 11.sp)
                        }
                    }
                }

                if (role == "SHAREHOLDER") {
                    OutlinedTextField(
                        value = sharePercentageStr,
                        onValueChange = { sharePercentageStr = it },
                        label = { Text(viewModel.t("Shareholder Equity percentage (%)", "শেয়ার অংশ (%)")) },
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.t("Account Status", "অ্যাকাউন্ট স্ট্যাটাস"))
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
                            label = { Text(viewModel.t("Inactive", "নিষ্ক্রিয়")) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareVal = if (role == "SHAREHOLDER") sharePercentageStr.toDoubleOrNull() ?: 0.0 else 0.0
                    val finalProjId = if (role == "ADMIN") null else selectedProjId
                    if (name.isNotBlank() && email.isNotBlank()) {
                        onSave(name, mobile, email, password, role, finalProjId, shareVal, status)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Save Account", "সংরক্ষণ করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}
