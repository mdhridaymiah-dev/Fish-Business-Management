package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: FarmViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Theme and Currency global selectors
    val isDarkMode by viewModel.darkMode.collectAsState()
    val currencySelected by viewModel.currencySymbol.collectAsState()
    val isBengali by viewModel.isBengali.collectAsState()

    var farmName by remember { mutableStateOf("Hridoy Aqua Breeding & Culture Limited") }
    var locationRegion by remember { mutableStateOf("Mymensingh Divisional, Bangladesh") }
    var contactPhone by remember { mutableStateOf("+880 1700 000001") }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            viewModel.t("Settings & System Setup", "সেটিংস ও সিস্টেম সেটআপ"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            viewModel.t("Configure user profile settings, visual themes switcher, currency indicators, and manage database security backups.", "ইউজার প্রোফাইল সেটিংস, থিম সুইচার, কারেন্সি ইন্ডিকেটর এবং ডাটাবেজ ব্যাকআপ পরিচালনা করুন।"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Divider()

        // User profile summary
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(viewModel.t("Personalized Active Account Profile", "অ্যাক্টিভ ইউজার অ্যাকাউন্ট প্রোফাইল"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("${viewModel.t("Full Name:", "পূর্ণ নাম:")} ${currentUser?.fullName}", fontSize = 13.sp)
                Text("${viewModel.t("Registered Mobile:", "নিবন্ধিত মোবাইল:")} ${currentUser?.mobile}", fontSize = 13.sp)
                Text("${viewModel.t("Logged Email:", "লগইন ইমেইল:")} ${currentUser?.email}", fontSize = 13.sp)
                Text("${viewModel.t("Venture System Role Mode:", "সিস্টেম রোল বা ভূমিকা:")} ${currentUser?.role}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Company/Farm settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(viewModel.t("Corporate Company / Farm Information", "করপোরেট কোম্পানি / খামার পরিচিতি"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text(viewModel.t("Farm Corporate Name", "খামারের নাম")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                OutlinedTextField(
                    value = locationRegion,
                    onValueChange = { locationRegion = it },
                    label = { Text(viewModel.t("Physical Location Region", "খামারের ভৌগলিক অবস্থান দেশ/অঞ্চল")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text(viewModel.t("Support Contact Phone", "যোগাযোগের ফোন নম্বর")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize()
                )
            }
        }

        // Appearance configs
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(viewModel.t("Display & System Configuration", "ডিসপ্লে ও সাধারণ সিস্টেম বিন্যাস"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                
                // Light & Dark theme toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(viewModel.t("Theme Mode (Light / Dark)", "থিম মোড (লাইট / ডার্ক)"), fontSize = 13.sp)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.darkMode.value = it },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }

                // Currency symbol configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.t("Currency Identifier Symbol Option", "কারেনসি বা মুদ্রার প্রতীকসমূহ"), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("৳", "$", "Rs").forEach { symbol ->
                            FilterChip(
                                selected = currencySelected == symbol,
                                onClick = { viewModel.currencySymbol.value = symbol },
                                label = { Text(symbol) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Supabase Cloud Sync & Backup Integration
        val urlVal by viewModel.supabaseUrl.collectAsState()
        val projIdVal by viewModel.supabaseProjectId.collectAsState()
        val keyVal by viewModel.supabaseAnonKey.collectAsState()
        val syncStateVal by viewModel.syncState.collectAsState()
        val syncErrVal by viewModel.syncErrorMessage.collectAsState()

        var showApiKeyVisible by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.t("Cloud Database Integration", "ক্লাউড ডাটাবেজ ইন্টিগ্রেশন"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Badge(
                        containerColor = when (syncStateVal) {
                            "SYNCING" -> MaterialTheme.colorScheme.tertiary
                            "SUCCESS" -> Color(0xFF2E7D32)
                            "ERROR" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when (syncStateVal) {
                                "SYNCING" -> viewModel.t("SYNCING...", "সিঙ্ক করা হচ্ছে...")
                                "SUCCESS" -> viewModel.t("CONNECTED", "সংযুক্ত")
                                "ERROR" -> viewModel.t("SYNC ERROR", "ত্রুটি")
                                else -> viewModel.t("OFFLINE SYNC", "অফলাইন সিঙ্ক")
                            },
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = viewModel.t(
                        "Synchronize your ERP tables with Supabase and secure your records globally.",
                        "সুপাবেস ক্লাউডের মাধ্যমে আপনার সম্পূর্ণ ERP মডিউল লাইভ ডাটাবেজের সাথে সিঙ্ক করুন।"
                    ),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = urlVal,
                    onValueChange = { viewModel.supabaseUrl.value = it },
                    label = { Text(viewModel.t("Supabase API URL", "সুপাবেস API ইউআরএল")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )

                OutlinedTextField(
                    value = projIdVal,
                    onValueChange = { viewModel.supabaseProjectId.value = it },
                    label = { Text(viewModel.t("Supabase Project ID", "সুপাবেস প্রজেক্ট আইডি")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )

                OutlinedTextField(
                    value = keyVal,
                    onValueChange = { viewModel.supabaseAnonKey.value = it },
                    label = { Text(viewModel.t("Supabase Anon API Key", "সুপাবেস পাবলিক সেগমেন্ট কী (Anon API Key)")) },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showApiKeyVisible = !showApiKeyVisible }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(
                                imageVector = if (showApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showApiKeyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                if (syncStateVal == "SYNCING") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                } else if (syncStateVal == "ERROR" && syncErrVal.isNotBlank()) {
                    Text(
                        text = "${viewModel.t("Sync Failed", "সিঙ্ক ত্রুটি")}: $syncErrVal",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncToSupabase() },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.t("Push Table", "আপলোড করুন"), fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.fetchFromSupabase() },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.t("Pull Data", "ডাউনলোড করুন"), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
