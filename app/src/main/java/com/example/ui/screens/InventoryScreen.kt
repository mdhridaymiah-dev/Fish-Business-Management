package com.example.ui.screens

import androidx.compose.foundation.background
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
fun InventoryScreen(viewModel: FarmViewModel, onAddItem: () -> Unit) {
    val inventory by viewModel.allInventory.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "ADMIN"

    var showFormDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("All") }

    val visibleInventory = remember(inventory, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") inventory
        else inventory.filter { it.category == selectedCategoryFilter }
    }

    Scaffold(
        floatingActionButton = {
            if (userRole == "ADMIN" || userRole == "MANAGER") {
                FloatingActionButton(
                    onClick = {
                        selectedItemForEdit = null
                        showFormDialog = true
                    },
                    modifier = Modifier.testTag("add_inventory_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Inventory")
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
                text = viewModel.t("Inventory Stock Registers", "ইনভেন্টরি ও মালামাল স্টক"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = viewModel.t(
                    "Supervise aquaculture feed, pond treatments, aerators and general farm machinery/supplies catalogs.",
                    "মৎস্য খাদ্য, পুকুরের চুন/ঔষধ এবং এয়ারেটর ও জালসহ খামারের সকল উপকরণের মজুদ পর্যবেক্ষণ করুন।"
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Category filters bar bilingually mapped
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Feed", "Medicine", "Equipment").forEach { cat ->
                    val catLabel = when(cat) {
                        "All" -> viewModel.t("All", "সর্বমোট")
                        "Feed" -> viewModel.t("Feed", "খাদ্য")
                        "Medicine" -> viewModel.t("Medicine", "ওষুধ/চুন")
                        "Equipment" -> viewModel.t("Equipment", "যন্ত্রপাতি")
                        else -> cat
                    }
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(catLabel) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            if (visibleInventory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.t("No inventory records found in this category.", "এই ক্যাটাগরিতে কোনো তথ্য পাওয়া যায়নি।"),
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
                    items(visibleInventory) { item ->
                        InventoryItemRow(
                            item = item,
                            currency = currency,
                            userRole = userRole,
                            viewModel = viewModel,
                            onEdit = {
                                selectedItemForEdit = item
                                showFormDialog = true
                            },
                            onDelete = {
                                viewModel.deleteInventoryItem(item)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        InventoryFormDialog(
            item = selectedItemForEdit,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false },
            onSave = { name, cat, qty, unit, price, supplier, date ->
                val updatedItem = InventoryItem(
                    id = selectedItemForEdit?.id ?: 0,
                    itemName = name,
                    category = cat,
                    quantity = qty,
                    unit = unit,
                    purchasePrice = price,
                    supplier = supplier,
                    purchaseDate = date,
                    lowStockAlertLimit = 0.0 // Set to zero, low stock alerts are decommissioned
                )
                viewModel.saveInventoryItem(updatedItem) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }
}

@Composable
fun InventoryItemRow(
    item: InventoryItem,
    currency: String,
    userRole: String,
    viewModel: FarmViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                        text = item.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val bidiCat = when(item.category) {
                        "Feed" -> viewModel.t("Feed", "খাদ্য")
                        "Medicine" -> viewModel.t("Medicine", "ওষুধ/চুন")
                        "Equipment" -> viewModel.t("Equipment", "যন্ত্রপাতি")
                        else -> item.category
                    }
                    Text(
                        text = "${viewModel.t("Category", "ক্যাটাগরি")}: $bidiCat | ${viewModel.t("Supplier", "সরবরাহকারী")}: ${item.supplier}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(viewModel.t("Quantity", "মজুদ পরিমাণ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.quantity} ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text(viewModel.t("Purchase Value", "ক্রয়মূল্য"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${item.purchasePrice}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                Column {
                    Text(viewModel.t("Log Date", "লগ তারিখ"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.purchaseDate, fontSize = 11.sp)
                }

                if (userRole == "ADMIN" || userRole == "MANAGER") {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Stock", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        if (userRole == "ADMIN") {
                            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Item", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryFormDialog(
    item: InventoryItem?,
    viewModel: FarmViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf(item?.itemName ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Feed") }
    var qtyStr by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "bags") }
    var priceStr by remember { mutableStateOf(item?.purchasePrice?.toString() ?: "") }
    var supplier by remember { mutableStateOf(item?.supplier ?: "") }
    var date by remember { mutableStateOf(item?.purchaseDate ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) viewModel.t("Log New Inventory Stock", "নতুন মালামাল স্টক ভুক্ত করুন") else viewModel.t("Adjust Inventory Log", "মালামাল বিবরণ সংশোধন করুন"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(viewModel.t("Item Name", "আইটেম বা মালামালের নাম")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                Text(viewModel.t("Select Category", "ক্যাটাগরি নির্বাচন করুন"), modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Feed", "Medicine", "Equipment").forEach { cat ->
                        val chipLabel = when(cat) {
                            "Feed" -> viewModel.t("Feed", "খাদ্য")
                            "Medicine" -> viewModel.t("Medicine", "ওষুধ/চুন")
                            "Equipment" -> viewModel.t("Equipment", "যন্ত্রপাতি")
                            else -> cat
                        }
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(chipLabel) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text(viewModel.t("Qty Amount", "আইটেম পরিমাণ")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(viewModel.t("Unit (eg. bags, pcs)", "একক (যেমন: বস্তা, পিস)")) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(viewModel.t("Total Purchase Price", "ক্রয় মূল্য")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text(viewModel.t("Supplier Name", "সরবরাহকারী প্রতিষ্ঠান")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text(viewModel.t("Date (YYYY-MM-DD)", "ক্রয় তারিখ (বছর-মাস-দিন)")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qtyVal = qtyStr.toDoubleOrNull() ?: 0.0
                    val priceVal = priceStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && date.isNotBlank()) {
                        onSave(name, category, qtyVal, unit, priceVal, supplier, date)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(viewModel.t("Save Item", "সংরক্ষণ করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text(viewModel.t("Cancel", "বাতিল"))
            }
        }
    )
}
