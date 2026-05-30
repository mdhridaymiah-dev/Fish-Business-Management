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

    var showUserFormDialog by remember { mutableStateOf(false) }
    var selectedUserForEdit by remember { mutableStateOf<User?>(null) }
    var activeAdminTab by remember { mutableStateOf("Enroll User") } // Tab Options: Enroll User, Expense Approval, Audit Logs

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
                                    }
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
    onApprove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Category: ${expense.category} | Pond: $projectName", fontSize = 11.sp)
                Text("Requested Cost Value: BDT ${expense.amount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Approve", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun UserFormDialog(
    user: User?,
    projects: List<Project>,
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
            Text(if (user == null) "Log New User Profile" else "Adjust Profile configurations")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Profile Name") },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Login Email ID") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Contact") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Login Password") },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                // Role Options selector
                Text("Assign Role Mode", modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ADMIN", "MANAGER", "SHAREHOLDER").forEach { rOption ->
                        FilterChip(
                            selected = role == rOption,
                            onClick = { role = rOption },
                            label = { Text(rOption, fontSize = 9.sp) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }

                if (role != "ADMIN") {
                    Text("Select Assigned Operational Pond Project:")
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
                        label = { Text("Shareholder Equity percentage (%)") },
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Status")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status == "Active",
                            onClick = { status = "Active" },
                            label = { Text("Active") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        FilterChip(
                            selected = status == "Inactive",
                            onClick = { status = "Inactive" },
                            label = { Text("Inactive") },
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
                Text("Enroll User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("Cancel")
            }
        }
    )
}
