package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.FarmViewModel
import com.example.ui.ProjectSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: FarmViewModel) {
    val projects by viewModel.allProjects.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val sales by viewModel.allSales.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val inventory by viewModel.allInventory.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "ADMIN"
    val userProjId = currentUser?.assignedProjectId

    var selectedProjectIdForReporting by remember { mutableStateOf<Int?>(null) } // null = All Projects
    var dateStartFilter by remember { mutableStateOf("") }
    var dateEndFilter by remember { mutableStateOf("") }
    var activeReportTypeTab by remember { mutableStateOf("Profit & Loss") } // Tab Options: Profit & Loss, Sales, Expenses, Shareholders, Stock Inventory

    var exportToastMessage by remember { mutableStateOf("") }

    // Scope project access based on data visibility rules
    val availableProjects = remember(projects, userRole, userProjId) {
        if (userRole == "ADMIN") projects
        else if (userProjId != null) projects.filter { it.id == userProjId }
        else emptyList()
    }

    // Set initial project filter if user is manager or shareholder
    LaunchedEffect(userProjId, userRole) {
        if (userRole != "ADMIN" && userProjId != null) {
            selectedProjectIdForReporting = userProjId
        }
    }

    // Filtered lists
    val filteredSales = remember(sales, selectedProjectIdForReporting, dateStartFilter, dateEndFilter) {
        sales.filter { sale ->
            val matchesProj = (selectedProjectIdForReporting == null || sale.projectId == selectedProjectIdForReporting)
            val matchesStart = (dateStartFilter.isEmpty() || sale.saleDate >= dateStartFilter)
            val matchesEnd = (dateEndFilter.isEmpty() || sale.saleDate <= dateEndFilter)
            matchesProj && matchesStart && matchesEnd
        }
    }

    val filteredExpenses = remember(expenses, selectedProjectIdForReporting, dateStartFilter, dateEndFilter) {
        expenses.filter { exp ->
            val matchesProj = (selectedProjectIdForReporting == null || exp.projectId == selectedProjectIdForReporting)
            val matchesStart = (dateStartFilter.isEmpty() || exp.date >= dateStartFilter)
            val matchesEnd = (dateEndFilter.isEmpty() || exp.date <= dateEndFilter)
            matchesProj && matchesStart && matchesEnd
        }
    }

    // Profit sharing values
    val reportTurnover = filteredSales.sumOf { it.totalPrice }
    val reportTotalApprovedCosts = filteredExpenses.filter { it.isApproved }.sumOf { it.amount }
    val reportTotalPendingCosts = filteredExpenses.filter { !it.isApproved }.sumOf { it.amount }
    val reportNetPnL = reportTurnover - reportTotalApprovedCosts

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Advanced Farm Reporting",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Compile historical profit/loss audit reports, stock registers, and dividend allocations.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Date and Project filters block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Compile Filters Control", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                if (userRole == "ADMIN") {
                    Text("Select Aqua Project Folder:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectIdForReporting == null,
                            onClick = { selectedProjectIdForReporting = null },
                            label = { Text("All Ponds") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        availableProjects.forEach { proj ->
                            FilterChip(
                                selected = selectedProjectIdForReporting == proj.id,
                                onClick = { selectedProjectIdForReporting = proj.id },
                                label = { Text(proj.pondName) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dateStartFilter,
                        onValueChange = { dateStartFilter = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    OutlinedTextField(
                        value = dateEndFilter,
                        onValueChange = { dateEndFilter = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }
            }
        }

        // Output formatting selection tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Profit & Loss", "Sales", "Expenses", "Stock Inventory").forEach { tab ->
                Button(
                    onClick = { activeReportTypeTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeReportTypeTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tab,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeReportTypeTab == tab) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Export and direct print simulated triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { exportToastMessage = "Preserved compilation report exported safely as standard PDF!" },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export PDF", fontSize = 11.sp, color = Color.White)
            }
            Button(
                onClick = { exportToastMessage = "Compiled spreadsheet sheet exported safely as Microsoft Excel (.xlsx)!" },
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Excel", fontSize = 11.sp, color = Color.White)
            }
        }

        Divider()

        // Primary rendering block based on selected tabs
        when (activeReportTypeTab) {
            "Profit & Loss" -> {
                PnLReportView(
                    turnover = reportTurnover,
                    approvedCosts = reportTotalApprovedCosts,
                    pendingCosts = reportTotalPendingCosts,
                    netPnL = reportNetPnL,
                    currency = currency
                )
            }
            "Sales" -> {
                RowBreakdownView(
                    title = "Historical Wholesales Ledger",
                    items = filteredSales.map { "${it.saleDate} | ${it.fishType}: ${it.weight} kg sold to ${it.buyerName} -> $currency ${it.totalPrice}" }
                )
            }
            "Expenses" -> {
                RowBreakdownView(
                    title = "Approved Expenditures Audit",
                    items = filteredExpenses.map { "${it.date} | ${it.title} (${it.category}): BDT ${it.amount} [${if (it.isApproved) "Approved" else "Pending"}]" }
                )
            }
            "Stock Inventory" -> {
                RowBreakdownView(
                    title = "Static Stock Valuation Log",
                    items = inventory.map { "${it.itemName} (${it.category}): ${it.quantity} ${it.unit} in store (Valued BDT ${it.purchasePrice * it.quantity})" }
                )
            }
        }

        // Shareholder Profit Allocations Report Widget
        ShareholderProfitAllocationsReport(
            users = users,
            sales = sales,
            expenses = expenses,
            currency = currency,
            userRole = userRole,
            currentUser = currentUser
        )
    }

    if (exportToastMessage.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { exportToastMessage = "" },
            title = { Text("Report Compiling Success") },
            text = { Text(exportToastMessage) },
            confirmButton = {
                TextButton(onClick = { exportToastMessage = "" }, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Text("Excellent")
                }
            }
        )
    }
}

@Composable
fun PnLReportView(
    turnover: Double,
    approvedCosts: Double,
    pendingCosts: Double,
    netPnL: Double,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Venture Income statement (Profit & Loss)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Divider()
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gross Revenue Turnover (+):", fontSize = 13.sp)
                Text("$currency $turnover", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Settled / Approved Costs (-):", fontSize = 13.sp)
                Text("$currency $approvedCosts", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cost requested under approval review:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$currency $pendingCosts", fontSize = 12.sp, color = Color(0xFFEF6C00))
            }
            Divider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Net Profiteering Balance:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val color = if (netPnL >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                Text("$currency $netPnL", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
            }
        }
    }
}

@Composable
fun RowBreakdownView(title: String, items: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Divider()
            if (items.isEmpty()) {
                Text("No matching ledger items found under this compile.", fontSize = 12.sp)
            } else {
                items.forEach { row ->
                    Text("• $row", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun ShareholderProfitAllocationsReport(
    users: List<User>,
    sales: List<Sale>,
    expenses: List<Expense>,
    currency: String,
    userRole: String,
    currentUser: User?
) {
    val shareholders = remember(users, userRole, currentUser) {
        val allShares = users.filter { it.role == "SHAREHOLDER" }
        if (userRole == "ADMIN") allShares
        else if (currentUser?.assignedProjectId != null) {
            allShares.filter { it.assignedProjectId == currentUser.assignedProjectId }
        } else emptyList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Proportional Dividend Distributions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Divider()
            
            if (shareholders.isEmpty()) {
                Text("No shareholders found associated with filtered scopes.", fontSize = 12.sp)
            } else {
                shareholders.forEach { partner ->
                    val projId = partner.assignedProjectId
                    val projSales = if (projId != null) sales.filter { it.projectId == projId }.sumOf { it.totalPrice } else 0.0
                    val approvedProjExpenses = if (projId != null) expenses.filter { it.projectId == projId && it.isApproved }.sumOf { it.amount } else 0.0
                    val netProfit = projSales - approvedProjExpenses
                    val sharePercentageOfBottomProfit = netProfit * (partner.sharePercentage / 100.0)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(partner.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Capital Base: $currency ${partner.investmentAmount} (${partner.sharePercentage}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "$currency ${"%.1f".format(sharePercentageOfBottomProfit)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (sharePercentageOfBottomProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                }
            }
        }
    }
}
