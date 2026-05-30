package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FarmViewModel
import com.example.ui.ProjectSummary

@Composable
fun DashboardScreen(viewModel: FarmViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val inventory by viewModel.allInventory.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()
    val sales by viewModel.allSales.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "ADMIN"
    val userProjId = currentUser?.assignedProjectId

    // Scope data visibility based on user roles
    val visibleProjects = remember(projects, userRole, userProjId) {
        if (userRole == "ADMIN") projects 
        else if (userRole == "MANAGER" && userProjId != null) projects.filter { it.id == userProjId }
        else if (userRole == "SHAREHOLDER" && userProjId != null) projects.filter { it.id == userProjId }
        else emptyList()
    }

    val visibleSales = remember(sales, visibleProjects) {
        val projIds = visibleProjects.map { it.id }.toSet()
        sales.filter { projIds.contains(it.projectId) }
    }

    val visibleExpenses = remember(expenses, visibleProjects) {
        val projIds = visibleProjects.map { it.id }.toSet()
        expenses.filter { projIds.contains(it.projectId) }
    }

    // Calculated metrics
    val totalStock = visibleProjects.sumOf { it.stockQuantity }
    val activePondsCount = visibleProjects.count { it.status == "Active" }
    
    // Inventory Calculations
    val feedItem = inventory.find { it.category == "Feed" }
    val feedStock = feedItem?.quantity ?: 0.0
    val feedUnit = feedItem?.unit ?: "bags"

    val totalSalesVal = visibleSales.sumOf { it.totalPrice }
    val totalApprovedExpensesVal = visibleExpenses.filter { it.isApproved }.sumOf { it.amount }
    val unapprovedExpensesVal = visibleExpenses.filter { !it.isApproved }.sumOf { it.amount }
    val netProfitVal = totalSalesVal - totalApprovedExpensesVal

    // Shareholder summary specifics
    val mySharePercentage = currentUser?.sharePercentage ?: 0.0
    val myInvestment = currentUser?.investmentAmount ?: 0.0
    val myProfitShare = if (userRole == "SHAREHOLDER" && mySharePercentage > 0.0) {
        netProfitVal * (mySharePercentage / 100.0)
    } else 0.0

    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val userName = currentUser?.fullName ?: "User"
        
        // Bento Welcome & Header Area
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(
                text = "${viewModel.t("Welcome back", "স্বাগতম")}, $userName",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF3F4948)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = viewModel.t("Farm Overview & Analytics", "খামারের সার্বিক অবস্থা"),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF191C1C)
            )
        }

        // Active Mature Grown Fish Sale/Harvest Suggestions instead of low stock alerts
        val matureProjects = visibleProjects.filter { it.status == "Active" && it.stockQuantity > 0 }
        if (matureProjects.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White, shape = RoundedCornerShape(19.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Water,
                            contentDescription = "Mature Fish Ready",
                            tint = Color(0xFF006A6A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.t("MATURE FISH READY FOR SALE", "মাছ বিক্রয়ের উপযোগী হয়েছে"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF004D40)
                        )
                        val matureProjNames = matureProjects.joinToString(", ") { it.name }
                        Text(
                            text = viewModel.t(
                                "Cultivated fishes in '$matureProjNames' have reached maturity and are ready to sell for high valuation.",
                                "প্রজেক্ট '$matureProjNames' এর মাছগুলো পরিপক্ক হয়েছে এবং বাজারদরে বিক্রয় করার উপযোগী হয়েছে।"
                            ),
                            fontSize = 11.sp,
                            color = Color(0xFF004D40)
                        )
                    }
                    Button(
                        onClick = { viewModel.navigateTo("sales") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A6A)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Text(viewModel.t("Sell", "বিক্রয়"), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Widgets Metrics Rows (Bento Grid Layout)
        if (isTablet) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(2f)) {
                    MetricWidget(
                        title = viewModel.t("Net Profit Status", "মোট নিট লাভ (বর্তমান)"),
                        value = "$currency $netProfitVal",
                        subtitle = viewModel.t("Calculated PNL after approved costs", "অনুমোদিত খরচ পরবর্তী মোট লভ্যাংশ"),
                        icon = Icons.Default.AccountBalanceWallet,
                        containerColor = Color(0xFFCCE8E8)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetricWidget(
                        title = viewModel.t("Pond Status", "সক্রিয় পুকুর সংখ্যা"),
                        value = viewModel.t("$activePondsCount Active", "$activePondsCount টি উন্মুক্ত"),
                        subtitle = viewModel.t("Out of ${visibleProjects.size} Ponds", "মোট ${visibleProjects.size} টি পুকুরের মধ্যে"),
                        icon = Icons.Default.GridView,
                        containerColor = Color.White,
                        isOutlined = true
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    MetricWidget(
                        title = viewModel.t("Grown Fish Stock", "মোট মাছের সংখ্যা (মজুদ)"),
                        value = viewModel.t("$totalStock Units", "$totalStock টি"),
                        subtitle = viewModel.t("Cultivating live stock size", "খামারে চলমান জীবন্ত মাছ"),
                        icon = Icons.Default.Water,
                        containerColor = Color.White,
                        isOutlined = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetricWidget(
                        title = viewModel.t("Chemical / Feed Stock", "খাদ্য মজুদ"),
                        value = "$feedStock $feedUnit",
                        subtitle = viewModel.t("Pond consumables catalog size", "মজুদ খাদ্য ব্যাগ সংখ্যা"),
                        icon = Icons.Default.Inventory,
                        containerColor = Color.White,
                        isOutlined = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MetricWidget(
                        title = viewModel.t("Gross Revenue", "মোট বিক্রয়লব্ধ আয়"),
                        value = "$currency $totalSalesVal",
                        subtitle = viewModel.t("Gross revenue generated", "মোট উৎপাদন বিক্রয়লব্ধ সংগৃহীত টাকা"),
                        icon = Icons.Default.TrendingUp,
                        containerColor = Color.White,
                        isOutlined = true
                    )
                }
            }
            MetricWidget(
                title = viewModel.t("Total Expenses Incurred", "অনুমোদিত মোট খামার খরচ"),
                value = "$currency $totalApprovedExpensesVal",
                subtitle = viewModel.t("Expenditures approved by shareholders", "শেয়ারহোল্ডারদের দ্বারা অনুমোদিত মোট ক্রয় ও পরিচালনা ব্যয়"),
                icon = Icons.Default.ReceiptLong,
                containerColor = Color(0xFF191C1C),
                isDark = true
            )
        } else {
            // Mobile Stacked List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(
                    title = viewModel.t("Net Profit Status", "মোট নিট লাভ (বর্তমান)"),
                    value = "$currency $netProfitVal",
                    subtitle = viewModel.t("Calculated PNL after approved costs", "অনুমোদিত খরচ পরবর্তী মোট লভ্যাংশ"),
                    icon = Icons.Default.AccountBalanceWallet,
                    containerColor = Color(0xFFCCE8E8)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricWidget(
                            title = viewModel.t("Active Ponds", "সক্রিয় পুকুর"),
                            value = viewModel.t("$activePondsCount Active", "$activePondsCount টি"),
                            subtitle = viewModel.t("Out of ${visibleProjects.size}", "মোট ${visibleProjects.size} এর মধ্যে"),
                            icon = Icons.Default.GridView,
                            containerColor = Color.White,
                            isOutlined = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricWidget(
                            title = viewModel.t("Grown Stock", "মাছের মজুদ"),
                            value = viewModel.t("$totalStock Units", "$totalStock টি"),
                            subtitle = viewModel.t("Current live stock", "জীবন্ত খামার মজুদ"),
                            icon = Icons.Default.Water,
                            containerColor = Color.White,
                            isOutlined = true
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricWidget(
                            title = viewModel.t("Feed Stock", "খাদ্য মজুদ"),
                            value = "$feedStock $feedUnit",
                            subtitle = viewModel.t("Inventory", "মজুদ বিবরণ"),
                            icon = Icons.Default.Inventory,
                            containerColor = Color.White,
                            isOutlined = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricWidget(
                            title = viewModel.t("Gross Revenue", "আয়"),
                            value = "$currency $totalSalesVal",
                            subtitle = viewModel.t("Total sales", "বিক্রয়লব্ধ টাকা"),
                            icon = Icons.Default.TrendingUp,
                            containerColor = Color.White,
                            isOutlined = true
                        )
                    }
                }

                MetricWidget(
                    title = viewModel.t("Total Expenses Incurred", "অনুমোদিত খামার খরচ"),
                    value = "$currency $totalApprovedExpensesVal",
                    subtitle = viewModel.t("Total approved expenditures incurred", "অনুমোদিত ক্রয় খরচ"),
                    icon = Icons.Default.ReceiptLong,
                    containerColor = Color(0xFF191C1C),
                    isDark = true
                )
            }
        }

        // Shareholder Personal Summary
        if (userRole == "SHAREHOLDER" && mySharePercentage > 0.0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF191C1C)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Shareholder division",
                        tint = Color(0xFFCCE8E8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = viewModel.t("MY SHAREHOLDER ACCOUNT SUMMARY", "আমার শেয়ার বিবরণী হিসাব"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBEC8C8),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(viewModel.t("Your Share", "আমার অংশ"), fontSize = 10.sp, color = Color(0xFFBEC8C8))
                                Text("$mySharePercentage%", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                            }
                            Column {
                                Text(viewModel.t("Investment", "বিনিয়োগ"), fontSize = 10.sp, color = Color(0xFFBEC8C8))
                                Text("$currency $myInvestment", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                            }
                            Column {
                                Text(viewModel.t("Profit Share", "আয় অংশীদারিত্ব"), fontSize = 10.sp, color = Color(0xFFBEC8C8))
                                val pnlColor = if (myProfitShare >= 0) Color(0xFFCCE8E8) else Color(0xFFFF8A80)
                                Text("$currency ${"%.1f".format(myProfitShare)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = pnlColor)
                            }
                        }
                    }
                }
            }
        }

        // Advanced Visualizations & Analytics charts bilingually rendered
        if (isTablet) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    GraphCard(
                        title = viewModel.t("Monthly Sales vs Expense Trend", "মাসিক বিক্রয় ও খরচের তুলনামূলক গ্রাফ"),
                        dataPoints = listOf(35f, 50f, 40f, 85f, totalSalesVal.toFloat() / 5000f),
                        viewModel = viewModel
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    GrowthTrendCard(
                        title = viewModel.t("Pond Fish Biomass Growth Rate (Avg / grams)", "মাছ বৃদ্ধির গড় ওজন হার (গ্রাম হিসেবে)"),
                        dataPoints = listOf(10f, 45f, 90f, 150f, 280f),
                        viewModel = viewModel
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                GraphCard(
                    title = viewModel.t("Monthly Sales vs Expense Trend", "মাসিক বিক্রয় ও খরচের তুলনামূলক গ্রাফ"),
                    dataPoints = listOf(35f, 50f, 40f, 85f, totalSalesVal.toFloat() / 5000f),
                    viewModel = viewModel
                )
                GrowthTrendCard(
                    title = viewModel.t("Fish Biomass Growth Rate (Avg / grams)", "মাছ বৃদ্ধির গড় ওজন হার (গ্রাম হিসেবে)"),
                    dataPoints = listOf(10f, 45f, 90f, 150f, 280f),
                    viewModel = viewModel
                )
            }
        }

        // Today's System Activity Logs Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E3E3))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF006A6A), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.t("Today's Farm Activity Log", "আজকের খামারের কার্যক্রম রেজিস্ট্রি"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF191C1C)
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE0E3E3))
                if (activityLogs.isEmpty()) {
                    Text(
                        text = viewModel.t("No activities logged today yet.", "আজকে এখন পর্যন্ত কোনো কার্যক্রম রেকর্ড করা হয়নি।"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4F6363)
                    )
                } else {
                    activityLogs.take(5).forEach { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4F6363),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricWidget(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    isDark: Boolean = false,
    isOutlined: Boolean = false
) {
    if (isOutlined) {
        OutlinedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color.White,
                contentColor = Color(0xFF191C1C)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E3E3)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F6363), letterSpacing = 1.1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF191C1C))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF4F6363))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFE0F3F1), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF006A6A), modifier = Modifier.size(18.dp))
                }
            }
        }
    } else if (isDark) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191C1C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBEC8C8), letterSpacing = 1.2.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFFCCE8E8))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF006A6A), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF002020))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF002020))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF006A6A), fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF006A6A), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun GraphCard(title: String, dataPoints: List<Float>, viewModel: FarmViewModel) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E3E3)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF191C1C))
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val width = size.width
                val height = size.height
                val spacing = width / (dataPoints.size + 1)
                
                for (i in 1..4) {
                    val gridHeight = height * (i / 5f)
                    drawLine(
                        color = Color(0xFFE0E3E3).copy(alpha = 0.5f),
                        start = Offset(0f, gridHeight),
                        end = Offset(width, gridHeight),
                        strokeWidth = 2f
                    )
                }

                dataPoints.forEachIndexed { idx, value ->
                    val x = spacing * (idx + 1)
                    val barHeight = (value / 100f) * height
                    val y = height - barHeight
                    
                    drawLine(
                        color = Color(0xFF006A6A),
                        start = Offset(x, height),
                        end = Offset(x, y),
                        strokeWidth = 32f
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(viewModel.t("Jan", "জানু"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Feb", "ফেব্রু"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Mar", "মার্চ"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Apr", "এপ্রিল"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("May (Live)", "মে (চলতি)"), fontSize = 10.sp, color = Color(0xFF006A6A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GrowthTrendCard(title: String, dataPoints: List<Float>, viewModel: FarmViewModel) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E3E3)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF191C1C))
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val width = size.width
                val height = size.height
                val spacing = width / (dataPoints.size - 1)
                val maxVal = 300f
                
                val path = Path()
                
                dataPoints.forEachIndexed { idx, gVal ->
                    val x = spacing * idx
                    val y = height - (gVal / maxVal) * height
                    if (idx == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    drawCircle(
                        color = Color(0xFF006A6A),
                        radius = 8f,
                        center = Offset(x, y)
                    )
                }

                drawPath(
                    path = path,
                    color = Color(0xFF006A6A),
                    style = Stroke(width = 6f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(viewModel.t("Week 1", "সপ্তাহ ১"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Week 4", "সপ্তাহ ৪"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Week 8", "সপ্তাহ ৮"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Week 12", "সপ্তাহ ১২"), fontSize = 10.sp, color = Color(0xFF4F6363))
                Text(viewModel.t("Week 16 (Now)", "সপ্তাহ ১৬ (বর্তমান)"), fontSize = 10.sp, color = Color(0xFF006A6A), fontWeight = FontWeight.Bold)
            }
        }
    }
}
