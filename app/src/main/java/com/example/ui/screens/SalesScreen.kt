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
fun SalesScreen(viewModel: FarmViewModel, onAddSale: () -> Unit) {
    val sales by viewModel.allSales.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "ADMIN"
    val userProjId = currentUser?.assignedProjectId

    var showFormDialog by remember { mutableStateOf(false) }
    var selectedSaleForEdit by remember { mutableStateOf<Sale?>(null) }
    var activeInvoiceDetails by remember { mutableStateOf<Sale?>(null) }

    // Scoped Data Visibility based on User Role Rules
    val visibleSales = remember(sales, userRole, userProjId) {
        if (userRole == "ADMIN") sales
        else if (userProjId != null) sales.filter { it.projectId == userProjId }
        else sales
    }

    Scaffold(
        floatingActionButton = {
            if (userRole == "ADMIN" || userRole == "MANAGER") {
                FloatingActionButton(
                    onClick = {
                        selectedSaleForEdit = null
                        showFormDialog = true
                    },
                    modifier = Modifier.testTag("add_sale_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Sale")
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
                text = viewModel.t("Sales & Mature Harvest Revenue", "মাছ বিক্রয় ও আয় রেজিস্টার"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.t(
                    "Keep record of grown fish sales to buyers. Revenue is credited to associated projects to compute real-time net partner dividends.",
                    "খামারের পরিপক্ক মাছ বড় বড় পাইকারি ক্রেতাদের কাছে বিক্রয় করার স্লিপ রেজিস্টার। বিক্রয়লব্ধ আয় সরাসরি সংশ্লিষ্ট প্রজেক্ট ফান্ডে যুক্ত হয়ে পার্টনারদের লভ্যাংশ বৃদ্ধি করে।"
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (visibleSales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.t("No sales record found.", "বিক্রয়ের কোনো রেকর্ড পাওয়া যায়নি।"),
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
                    items(visibleSales) { sale ->
                        val projName = projects.find { it.id == sale.projectId }?.name ?: "Pond Project"
                        SaleItemCard(
                            sale = sale,
                            projectName = projName,
                            currency = currency,
                            userRole = userRole,
                            viewModel = viewModel,
                            onEdit = {
                                selectedSaleForEdit = sale
                                showFormDialog = true
                            },
                            onInvoiceTap = {
                                activeInvoiceDetails = sale
                            },
                            onDelete = {
                                viewModel.deleteSale(sale)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        SaleFormDialog(
            sale = selectedSaleForEdit,
            projects = projects.filter { userRole == "ADMIN" || it.id == userProjId },
            viewModel = viewModel,
            onDismiss = { showFormDialog = false },
            onSave = { date, fish, qty, weight, price, buyer, payment, invoice, projId ->
                val totalVal = weight * price
                val updatedSale = Sale(
                    id = selectedSaleForEdit?.id ?: 0,
                    saleDate = date,
                    fishType = fish,
                    quantity = qty,
                    weight = weight,
                    price = price,
                    totalPrice = totalVal,
                    buyerName = buyer,
                    paymentMethod = payment,
                    invoiceNumber = invoice,
                    projectId = projId
                )
                viewModel.saveSale(updatedSale) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }

    if (activeInvoiceDetails != null) {
        val projName = projects.find { it.id == activeInvoiceDetails!!.projectId }?.name ?: "Project Asset"
        InvoiceDetailsDialog(
            sale = activeInvoiceDetails!!,
            projectName = projName,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { activeInvoiceDetails = null }
        )
    }
}

@Composable
fun SaleItemCard(
    sale: Sale,
    projectName: String,
    currency: String,
    userRole: String,
    viewModel: FarmViewModel,
    onEdit: () -> Unit,
    onInvoiceTap: () -> Unit,
    onDelete: () -> Unit
) {
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
                        text = "${sale.fishType} ${viewModel.t("Sales", "বিক্রয়")} (${viewModel.t("Invoice", "মেমো")}: ${sale.invoiceNumber})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${viewModel.t("Source Project", "উৎস পুকুর প্রকল্প")}: $projectName",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onInvoiceTap, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.Receipt, contentDescription = "View Invoice", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(viewModel.t("Total Weight", "মোট ওজন"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${sale.weight} Kg", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text(viewModel.t("Rate / Kg", "কেজি প্রতি দর"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${sale.price}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(viewModel.t("Revenue Received", "মোট প্রাপ্ত মূল্য"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${sale.totalPrice}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${viewModel.t("Buyer", "ক্রেতা / আড়ত")}: ${sale.buyerName} | ${viewModel.t("Method", "পদ্ধতি")}: ${sale.paymentMethod}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                )

                if (userRole == "ADMIN" || userRole == "MANAGER") {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Sale", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        if (userRole == "ADMIN") {
                            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Sale", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaleFormDialog(
    sale: Sale?,
    projects: List<Project>,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Double, Double, String, String, String, Int) -> Unit
) {
    var date by remember { mutableStateOf(sale?.saleDate ?: "") }
    var fishType by remember { mutableStateOf(sale?.fishType ?: "") }
    var qtyStr by remember { mutableStateOf(sale?.quantity?.toString() ?: "") }
    var weightStr by remember { mutableStateOf(sale?.weight?.toString() ?: "") }
    var priceStr by remember { mutableStateOf(sale?.price?.toString() ?: "") }
    var buyer by remember { mutableStateOf(sale?.buyerName ?: "") }
    var paymentMethod by remember { mutableStateOf(sale?.paymentMethod ?: "Cash") }
    var invoice by remember { mutableStateOf(sale?.invoiceNumber ?: "INV-2026-${(100..999).random()}") }

    var selectedProjectId by remember { mutableStateOf(sale?.projectId ?: projects.firstOrNull()?.id ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (sale == null) viewModel.t("Log Mature Harvest Sales", "নতুন মাছ বিক্রয় স্লিপ লিপিবন্ধ করুন") else viewModel.t("Update Sales details", "বিক্রয় বিবরণ সংশোধন"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fishType,
                    onValueChange = { fishType = it },
                    label = { Text(viewModel.t("Fish Species Sold Name (eg. Rui, Catfish)", "বিক্রিত মাছের প্রজাতি")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                Text(viewModel.t("Associated Pond Project", "উৎস মৎস্য প্রজেক্ট বা পুকুর"), modifier = Modifier.padding(top = 4.dp))
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
                            .padding(8.dp),
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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text(viewModel.t("Approx Pcs Count", "আনুমানিক মাছের সংখ্যা")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text(viewModel.t("Total Weight (Kg)", "বিক্রিত মোট ওজন (Kg)")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text(viewModel.t("Rate / Kg", "কেজি প্রতি দর BDT")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = invoice,
                        onValueChange = { invoice = it },
                        label = { Text(viewModel.t("Invoice Number", "বিল মেমো নাম্বার")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = buyer,
                        onValueChange = { buyer = it },
                        label = { Text(viewModel.t("Buyer Name / Client", "ক্রেতা / পাইকার বা আড়তের নাম")) },
                        modifier = Modifier.weight(1.2f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(viewModel.t("Date (YYYY-MM-DD)", "বিক্রয় তারিখ")) },
                        modifier = Modifier.weight(0.8f).minimumInteractiveComponentSize()
                    )
                }

                Text(viewModel.t("Payment Method Option", "টাকা আদায়ের মাধ্যম"))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "Bank", "Bkash", "Nagad").forEach { mode ->
                        FilterChip(
                            selected = paymentMethod == mode,
                            onClick = { paymentMethod = mode },
                            label = { Text(mode) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qtyVal = qtyStr.toIntOrNull() ?: 1
                    val weightVal = weightStr.toDoubleOrNull() ?: 0.0
                    val priceVal = priceStr.toDoubleOrNull() ?: 0.0
                    if (fishType.isNotBlank() && selectedProjectId != 0 && date.isNotBlank()) {
                        onSave(date, fishType, qtyVal, weightVal, priceVal, buyer, paymentMethod, invoice, selectedProjectId)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Save Sale Log", "সংরক্ষণ করুন"))
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
fun InvoiceDetailsDialog(
    sale: Sale,
    projectName: String,
    currency: String,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(viewModel.t("AquaFarm Official Invoice Bill", "খামার বিক্রয় অফিশিয়াল মেমো"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                Text("${viewModel.t("Invoice No", "ভাউচার নং")}: ${sale.invoiceNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Invoice Date:", "বিক্রয় তারিখ:"), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Text(sale.saleDate, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Source Project:", "উৎস পুকুর প্রজেক্ট:"), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Text(projectName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Purchaser Name:", "ক্রেতা / আড়তের নাম:"), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Text(sale.buyerName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Species Harvested Type:", "বিক্রিত মাছের প্রজাতি:"), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Text(sale.fishType, fontSize = 12.sp)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Quantity Count:", "মোট মাছের সংখ্যা (আনুমানিক):"), fontSize = 12.sp)
                        Text("${sale.quantity} Pcs", fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Total Bill Weight:", "মোট ওজন পরিমাণ:"), fontSize = 12.sp)
                        Text("${sale.weight} Kg", fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Billed Price / Kg:", "কেজি প্রতি দর:"), fontSize = 12.sp)
                        Text("$currency ${sale.price} / Kg", fontSize = 12.sp)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(viewModel.t("Net Bill Value:", "মোট আদায়কৃত মূল্য বিলে:"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$currency ${sale.totalPrice}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.t("Payment Method Setup:", "টাকা আদায়ের মাধ্যম:"), fontSize = 12.sp)
                        Text(sale.paymentMethod, fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Acknowledge Invoice Receipt", "ইনভয়েস বুক বন্ধ করুন"))
            }
        }
    )
}
