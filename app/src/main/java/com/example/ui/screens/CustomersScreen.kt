package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(viewModel: FarmViewModel, onAddCustomer: () -> Unit) {
    val customers by viewModel.allCustomers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    val currency = viewModel.currencySymbol.collectAsState().value
    val userRole = currentUser?.role ?: "ADMIN"

    var showFormDialog by remember { mutableStateOf(false) }
    var selectedCustomerForEdit by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        floatingActionButton = {
            if (userRole == "ADMIN" || userRole == "MANAGER") {
                FloatingActionButton(
                    onClick = {
                        selectedCustomerForEdit = null
                        showFormDialog = true
                    },
                    modifier = Modifier.testTag("add_customer_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Customer")
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
                "Customer Portfolio Registry",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Supervise wholesalers and direct purchase history, contact channels, and outstanding due balances.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContactPage,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No customers enrolled in system registry.",
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
                    items(customers) { client ->
                        CustomerCard(
                            customer = client,
                            currency = currency,
                            userRole = userRole,
                            onEdit = {
                                selectedCustomerForEdit = client
                                showFormDialog = true
                            },
                            onDelete = {
                                viewModel.deleteCustomer(client)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        CustomerFormDialog(
            customer = selectedCustomerForEdit,
            onDismiss = { showFormDialog = false },
            onSave = { name, mobile, address, purchases, due, notes ->
                val updatedCust = Customer(
                    id = selectedCustomerForEdit?.id ?: 0,
                    name = name,
                    mobile = mobile,
                    address = address,
                    purchaseHistory = purchases,
                    dueBalance = due,
                    notes = notes
                )
                viewModel.saveCustomer(updatedCust) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: Customer,
    currency: String,
    userRole: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val showWarningColor = customer.dueBalance > 0.0

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
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mobile: ${customer.mobile} | Address: ${customer.address}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (userRole == "ADMIN" || userRole == "MANAGER") {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Cust", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        if (userRole == "ADMIN") {
                            IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Cust", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Deals Concluded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${customer.purchaseHistory} Purchases", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Outstanding Balance Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currency ${customer.dueBalance}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (showWarningColor) Color(0xFFC62828) else Color(0xFF2E7D32))
                }
            }

            if (customer.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Customer Notes: ${customer.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var mobile by remember { mutableStateOf(customer?.mobile ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var buyHistoryStr by remember { mutableStateOf(customer?.purchaseHistory?.toString() ?: "0") }
    var dueStr by remember { mutableStateOf(customer?.dueBalance?.toString() ?: "0.0") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (customer == null) "Enroll New Fish Customer" else "Revise Buyer Contact File")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Physical address") },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = buyHistoryStr,
                        onValueChange = { buyHistoryStr = it },
                        label = { Text("Purchase Count History") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    OutlinedTextField(
                        value = dueStr,
                        onValueChange = { dueStr = it },
                        label = { Text("Outstanding Due Debt") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference notes") },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buysVal = buyHistoryStr.toIntOrNull() ?: 0
                    val dueVal = dueStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && mobile.isNotBlank()) {
                        onSave(name, mobile, address, buysVal, dueVal, notes)
                    }
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Confirm Portfolio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("Cancel")
            }
        }
    )
}
