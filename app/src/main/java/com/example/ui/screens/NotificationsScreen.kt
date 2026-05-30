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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: FarmViewModel) {
    val notificationList by viewModel.allNotifications.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val userRole = currentUser?.role ?: "SHAREHOLDER"
    val userProjId = currentUser?.assignedProjectId

    val visibleNotifications = remember(notificationList, userRole, userProjId) {
        if (userRole == "ADMIN") {
            notificationList
        } else {
            notificationList.filter { it.projectId == null || it.projectId == userProjId }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    viewModel.t("System Notifications Hub", "সিস্টেম নোটিফিকেশন হাব"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    viewModel.t("Real-time alerts, stock limits warns, sale alerts, and cost updates.", "রিয়েল-টাইম অ্যালার্ট, স্টক লিমিট ওয়ার্নিং, সেলস অ্যালার্ট এবং খরচের আপডেট।"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (visibleNotifications.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.markNotificationsRead() },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(viewModel.t("Clear Badges", "ব্যাজ পরিষ্কার করুন"), fontSize = 12.sp)
                }
            }
        }

        Divider()

        if (visibleNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AllInbox,
                        contentDescription = "Inbox empty",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        viewModel.t("No active notifications in the pipeline.", "পাইপলাইনে কোনো সক্রিয় নোটিফিকেশন নেই।"),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        viewModel.t("Excellent. All stocking levels and expenses are in stable check status.", "চমৎকার। সব স্টক লেভেল এবং ব্যয় নিয়ন্ত্রণের মধ্যে রয়েছে।"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleNotifications) { alert ->
                    NotificationCard(
                        alert = alert,
                        onDismissTap = {
                            viewModel.dismissNotification(alert)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    alert: Notification,
    onDismissTap: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when(alert.type) {
                "low_inventory" -> Color(0xFFFFEBEE) // light alert red
                "new_expense" -> Color(0xFFFFF3E0) // light alert orange
                "new_sale" -> Color(0xFFE8F5E9) // light alert green
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when(alert.type) {
                        "low_inventory" -> Icons.Default.Warning
                        "new_expense" -> Icons.Default.ReceiptLong
                        "new_sale" -> Icons.Default.TrendingUp
                        "harvest_reminder" -> Icons.Default.Alarm
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when(alert.type) {
                        "low_inventory" -> Color(0xFFC62828)
                        "new_expense" -> Color(0xFFEF6C00)
                        "new_sale" -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = alert.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = alert.message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = alert.date,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
                    )
                }
            }

            IconButton(onClick = onDismissTap, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss Alerts", modifier = Modifier.size(16.dp))
            }
        }
    }
}
