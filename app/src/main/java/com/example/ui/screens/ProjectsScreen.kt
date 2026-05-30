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
fun ProjectsScreen(viewModel: FarmViewModel, onAddProject: () -> Unit) {
    val projects by viewModel.allProjects.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "ADMIN"
    val userProjId = currentUser?.assignedProjectId

    var showFormDialog by remember { mutableStateOf(false) }
    var selectedProjectForEdit by remember { mutableStateOf<Project?>(null) }

    // Filter projects based on user assignment scoped visibility
    val visibleProjects = remember(projects, userRole, userProjId) {
        if (userRole == "ADMIN") projects
        else if (userProjId != null) projects.filter { it.id == userProjId }
        else emptyList()
    }

    Scaffold(
        floatingActionButton = {
            if (userRole == "ADMIN") {
                FloatingActionButton(
                    onClick = {
                        selectedProjectForEdit = null
                        showFormDialog = true
                    },
                    modifier = Modifier.testTag("add_project_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Project")
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
                text = viewModel.t("Fish Culture Projects (Ponds)", "মৎস্য প্রকল্প ও পুকুর ড্যাশবোর্ড"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.t(
                    "Organize multiple culture ponds, oversee stocking parameters, assign managers, and log estimated mature harvest goals.",
                    "খামারের বিভিন্ন পুকুরে চলমান বড় জাতের মৎস্য চাষ প্রজেক্ট রেকর্ড, পুকুরের সাইজ, মাছ ছাড়ার পরিমাপ এবং চাষের বিস্তারিত বিবরণ পরিচালনা করুন।"
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (visibleProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Water,
                            contentDescription = "No Projects",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.t("No active fish culture projects found.", "কোনো মৎস্য চাষ প্রজেক্ট পাওয়া যায়নি।"),
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
                    items(visibleProjects) { project ->
                        ProjectItemCard(
                            project = project,
                            currency = currency,
                            userRole = userRole,
                            users = users,
                            viewModel = viewModel,
                            onEdit = {
                                selectedProjectForEdit = project
                                showFormDialog = true
                            },
                            onDelete = {
                                viewModel.deleteProject(project)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        ProjectFormDialog(
            project = selectedProjectForEdit,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false },
            onSave = { name, pond, size, fish, qty, start, harvest, invest, statusMsg, manualExp ->
                val newProj = Project(
                    id = selectedProjectForEdit?.id ?: 0,
                    name = name,
                    pondName = pond,
                    pondSize = size,
                    fishType = fish,
                    stockQuantity = qty,
                    startDate = start,
                    estimatedHarvestDate = harvest,
                    investmentAmount = invest,
                    status = statusMsg,
                    manualExpense = manualExp
                )
                viewModel.saveProject(newProj) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }
}

@Composable
fun ProjectItemCard(
    project: Project,
    currency: String,
    userRole: String,
    users: List<User>,
    viewModel: FarmViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val projectManager = remember(users, project.id) {
        users.find { it.assignedProjectId == project.id && it.role == "MANAGER" }
    }
    val shareholdersCount = remember(users, project.id) {
        users.count { it.assignedProjectId == project.id && it.role == "SHAREHOLDER" && it.status == "Active" }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
                        text = project.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${viewModel.t("Pond Location", "পুকুরের নাম")}: ${project.pondName} (${project.pondSize} ${viewModel.t("Decimals", "শতাংশ")})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (project.status == "Active") Color(0xFFE8F5E9) else Color(0xFFECEFF1)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (project.status == "Active") viewModel.t("Active", "চলতি চাষ") else viewModel.t("Completed", "সম্পন্ন"),
                        color = if (project.status == "Active") Color(0xFF2E7D32) else Color(0xFF607D8B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(viewModel.t("Fish Species", "মাছের প্রজাতি"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(project.fishType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text(viewModel.t("Stock Qty", "মজুদ পোনা সংখ্যা"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${project.stockQuantity} ${viewModel.t("Pcs", "টি")}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text(viewModel.t("Initial Investment", "প্রারম্ভিক বিনিয়োগ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${project.investmentAmount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Approved Expenses & Admin Manually Entered Total Expenses
            val projExpenses = remember(viewModel.allExpenses.collectAsState().value, project.id) {
                viewModel.allExpenses.value
                    .filter { it.projectId == project.id && (it.isApproved || it.approvalStatus == "APPROVED") }
                    .sumOf { it.amount }
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFEBEE).copy(alpha = 0.5f)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(viewModel.t("Approved Vouchered Expense", "অনুমোদিত ভাউচার খরচ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency $projExpenses", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(viewModel.t("Admin Documented Total Expense", "মোট খামার চাষাবিল খরচ (এন্ট্রিকৃত)"), fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    Text("$currency ${project.manualExpense}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFC62828))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(viewModel.t("Culture Start Date", "চাষ ও পোনা ছাড়ার তারিখ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(project.startDate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(viewModel.t("Estimated Harvest Date", "সম্ভাব্য আহরণ/বিক্রয়ের সময়"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(project.estimatedHarvestDate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val mgrName = projectManager?.fullName ?: viewModel.t("Vacant", "খালি")
                    Text("${viewModel.t("Manager Assigned", "নিযুক্ত ম্যানেজার")}: $mgrName", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("${viewModel.t("Partners Count", "অংশীদার সংখ্যা")}: $shareholdersCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (userRole == "ADMIN") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectFormDialog(
    project: Project?,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, Int, String, String, Double, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(project?.name ?: "") }
    var pondName by remember { mutableStateOf(project?.pondName ?: "") }
    var pondSizeStr by remember { mutableStateOf(project?.pondSize?.toString() ?: "") }
    var fishType by remember { mutableStateOf(project?.fishType ?: "") }
    var stockQtyStr by remember { mutableStateOf(project?.stockQuantity?.toString() ?: "") }
    var startDate by remember { mutableStateOf(project?.startDate ?: "") }
    var harvestDate by remember { mutableStateOf(project?.estimatedHarvestDate ?: "") }
    var investStr by remember { mutableStateOf(project?.investmentAmount?.toString() ?: "") }
    var status by remember { mutableStateOf(project?.status ?: "Active") }
    var manualExpenseStr by remember { mutableStateOf(project?.manualExpense?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (project == null) viewModel.t("Initiate New Fish Project", "নতুন মৎস্য চাষ প্রকল্প সূচিত করুন") else viewModel.t("Re-adjust Project parameters", "প্রজেক্টের তথ্য সংশোধন করুন"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(viewModel.t("Project Name (eg. Tilapia Cultivation)", "প্রকল্পের নাম (যেমন: কার্প ও তেলাপিয়া চাষ)")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                OutlinedTextField(
                    value = pondName,
                    onValueChange = { pondName = it },
                    label = { Text(viewModel.t("Pond Identification / ID", "পুকুরের নাম বা পরিচিতি")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pondSizeStr,
                        onValueChange = { pondSizeStr = it },
                        label = { Text(viewModel.t("Size (Decimals)", "আয়তন (শতাংশ)")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = fishType,
                        onValueChange = { fishType = it },
                        label = { Text(viewModel.t("Fish Species Type", "মাছের প্রজাতি বা জাত")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stockQtyStr,
                        onValueChange = { stockQtyStr = it },
                        label = { Text(viewModel.t("Pona Stock Quantity", "পোনা ছাড়ার সংখ্যা")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = investStr,
                        onValueChange = { investStr = it },
                        label = { Text(viewModel.t("Budget Investment", "বরাদ্দকৃত বাজেট")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = manualExpenseStr,
                        onValueChange = { manualExpenseStr = it },
                        label = { Text(viewModel.t("Total Expense (মোট খরচ) BDT", "মোট চাষ খরচ (BDT) (এন্ট্রি)")) },
                        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text(viewModel.t("Start Date (YYYY-MM-DD)", "শুরুর তারিখ (বছর-মাস-দিন)")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = harvestDate,
                        onValueChange = { harvestDate = it },
                        label = { Text(viewModel.t("Harvest Date (YYYY-MM-DD)", "আহরণের সম্ভাব্য তারিখ")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.t("Venture Cultivation Status", "চাষ কার্যক্রম স্ট্যাটাস"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status == "Active",
                            onClick = { status = "Active" },
                            label = { Text(viewModel.t("Active", "চলমান চাষ")) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        FilterChip(
                            selected = status == "Completed",
                            onClick = { status = "Completed" },
                            label = { Text(viewModel.t("Completed", "সম্পন্ন")) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sizeVal = pondSizeStr.toDoubleOrNull() ?: 0.0
                    val qtyVal = stockQtyStr.toIntOrNull() ?: 0
                    val investVal = investStr.toDoubleOrNull() ?: 0.0
                    val manualExpVal = manualExpenseStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && fishType.isNotBlank() && startDate.isNotBlank()) {
                        onSave(name, pondName, sizeVal, fishType, qtyVal, startDate, harvestDate, investVal, status, manualExpVal)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Save Project info", "তথ্যভুক্ত করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}
