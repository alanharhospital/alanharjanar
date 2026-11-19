package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.UiState
import com.example.domain.model.UserNotification
import com.example.ui.components.StatusPill
import com.example.ui.theme.HospitalIndigo
import com.example.ui.theme.HospitalIndigoBg
import com.example.ui.theme.OpenAccessEmerald
import com.example.ui.theme.OpenAccessEmeraldBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<UserNotification>>,
    subscribedTopics: List<String>,
    onBack: () -> Unit,
    onCheckUpdatesNow: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.navNotifications) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCheckUpdatesNow,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("notifications_sync_now_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = strings.refresh)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (strings.isArabic) "تفعيل إشعارات الأبحاث الجديدة" else "Enable Push Alerts for New Publications",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (strings.isArabic) "اسمح للتطبيق بإرسال تنبيهات عند صدور أبحاث جديدة في تخصصاتك المختارة."
                                else "Allow system notifications when periodic background checks discover new studies in your subscribed specialties.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                modifier = Modifier.testTag("request_notification_permission_btn")
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (strings.isArabic) "تفعيل الإشعارات" else "Allow Notifications")
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (strings.isArabic) "المراقبة البحثية الآلية" else "Automatic Research Monitoring",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (subscribedTopics.isEmpty()) {
                                    if (strings.isArabic) "التخصصات الافتراضية: التهوية الميكانيكية، ARDS" else "Default monitors: Mechanical Ventilation, ARDS"
                                } else {
                                    subscribedTopics.joinToString(", ")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onCheckUpdatesNow) {
                            Text(if (strings.isArabic) "فحص الآن" else "Check Now")
                        }
                    }
                }
            }

            when (notificationsState) {
                is UiState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                text = notificationsState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val list = notificationsState.data
                    if (list.isEmpty()) {
                        item {
                            Card {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (strings.isArabic) "لا توجد إشعارات بحثية جديدة حالياً." else "No new research notifications yet. Tap Check Now to scan PubMed & Europe PMC.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else {
                        items(list, key = { it.id }) { notif ->
                            val container = if (notif.read) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!notif.read) onMarkRead(notif.id) },
                                colors = CardDefaults.cardColors(containerColor = container)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val (bg, fg) = if (notif.notificationType == "NEW_OPEN_ACCESS") {
                                            OpenAccessEmeraldBg to OpenAccessEmerald
                                        } else {
                                            HospitalIndigoBg to HospitalIndigo
                                        }
                                        StatusPill(notif.notificationType, bg, fg, if (notif.read) Icons.Filled.CheckCircle else null)
                                    }
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (notif.read) FontWeight.Normal else FontWeight.Bold
                                    )
                                    Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (notif.doi.isNotBlank()) {
                                        Text("DOI: ${notif.doi}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
