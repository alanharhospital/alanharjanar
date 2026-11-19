package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalAppStrings
import com.example.data.local.LocalAppPreferences
import com.example.data.local.SyncLogEntity
import com.example.domain.model.EditorialPost
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.HospitalConfig
import com.example.domain.model.ScientificCategory
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.ui.components.HospitalLogoBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.HospitalIndigo
import com.example.ui.theme.HospitalIndigoBg

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileAdminScreen(
    hospitalConfig: HospitalConfig,
    localPrefs: LocalAppPreferences,
    userProfileState: UiState<UserProfile?>,
    isAdmin: Boolean,
    adminUsers: List<UserProfile>,
    syncLogs: List<SyncLogEntity>,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetThemeMode: (String) -> Unit,
    onUpdateNotificationPrefs: (Boolean, Boolean) -> Unit,
    onUpdateSyncSettings: (String, Int) -> Unit,
    onUploadHospitalLogo: (Uri) -> Unit,
    onPublishEditorial: (EditorialPost) -> Unit,
    onUpsertCategory: (ScientificCategory) -> Unit,
    onUpsertGuideline: (GuidelineRecord) -> Unit,
    onClearSyncLogs: () -> Unit
) {
    val strings = LocalAppStrings.current
    val profile = (userProfileState as? UiState.Success)?.data

    var unpaywallDraft by remember(localPrefs.unpaywallEmail) { mutableStateOf(localPrefs.unpaywallEmail) }
    var syncHoursDraft by remember(localPrefs.syncFrequencyHours) { mutableStateOf(localPrefs.syncFrequencyHours) }

    // Admin Editor States
    var edTitle by remember { mutableStateOf("") }
    var edTitleAr by remember { mutableStateOf("") }
    var edSummary by remember { mutableStateOf("") }
    var edSpecialty by remember { mutableStateOf("Mechanical Ventilation") }
    var edIsHospitalProtocol by remember { mutableStateOf(false) }

    var catNameEn by remember { mutableStateOf("") }
    var catNameAr by remember { mutableStateOf("") }
    var catTrack by remember { mutableStateOf("RESPIRATORY_CARE") }

    var guideTitle by remember { mutableStateOf("") }
    var guideOrg by remember { mutableStateOf("Al-Anhar Hospital") }
    var guideTopic by remember { mutableStateOf("Mechanical Ventilation") }
    var guideRecs by remember { mutableStateOf("") }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onUploadHospitalLogo(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) "${strings.navProfile} • Admin" else strings.navProfile) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.signOutButton)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Clinician Identity Card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HospitalLogoBadge(config = hospitalConfig, size = 54.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName?.ifBlank { "Clinician" } ?: "Clinician",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = profile?.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isAdmin) {
                                Spacer(Modifier.height(4.dp))
                                StatusPill(
                                    text = strings.adminDashboard,
                                    containerColor = HospitalIndigoBg,
                                    contentColor = HospitalIndigo,
                                    icon = Icons.Filled.AdminPanelSettings
                                )
                            }
                        }
                    }
                }
            }

            // Language & Theme Controls
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (strings.isArabic) "اللغة والمظهر" else "Language & Appearance",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = localPrefs.language == "en",
                                onClick = { onSetLanguage("en") },
                                label = { Text("English (LTR)") },
                                modifier = Modifier.testTag("pref_lang_en")
                            )
                            FilterChip(
                                selected = localPrefs.language == "ar",
                                onClick = { onSetLanguage("ar") },
                                label = { Text("العربية (RTL)") },
                                modifier = Modifier.testTag("pref_lang_ar")
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "system" to (if (strings.isArabic) "تلقائي" else "System"),
                                "light" to (if (strings.isArabic) "فاتح" else "Light"),
                                "dark" to (if (strings.isArabic) "داكن (ليلي)" else "Dark (ICU)")
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = localPrefs.themeMode == key,
                                    onClick = { onSetThemeMode(key) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("pref_theme_$key")
                                )
                            }
                        }
                    }
                }
            }

            // Research Sync & Alerts Preferences
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (strings.isArabic) "إعدادات المزامنة البحثية و Unpaywall" else "Research Monitor & Unpaywall Settings",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (strings.isArabic) "إشعارات الأبحاث الفورية" else "Push Research Alerts", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = profile?.pushEnabled ?: true,
                                onCheckedChange = { onUpdateNotificationPrefs(it, profile?.emailDigestEnabled ?: false) }
                            )
                        }
                        OutlinedTextField(
                            value = unpaywallDraft,
                            onValueChange = { unpaywallDraft = it },
                            label = { Text("Unpaywall Contact Email (Legal Open-Access API)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(6, 12, 24, 48).forEach { hrs ->
                                FilterChip(
                                    selected = syncHoursDraft == hrs,
                                    onClick = { syncHoursDraft = hrs },
                                    label = { Text("${hrs}h") }
                                )
                            }
                        }
                        Button(onClick = { onUpdateSyncSettings(unpaywallDraft, syncHoursDraft) }) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "حفظ إعدادات المزامنة" else "Save Sync Settings")
                        }
                    }
                }
            }

            // Hospital Admin Dashboard (Visible ONLY to authenticated verified Admin)
            if (isAdmin) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HospitalIndigoBg)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = HospitalIndigo)
                                Text(
                                    text = strings.adminDashboard,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = HospitalIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (strings.isArabic) "رفع الشعار الرسمي لمستشفى الأنهار التخصصي (لا يتم تعديل الشعار تلقائياً)" else "Upload Official Al-Anhar Hospital Logo via Photo Picker",
                                style = MaterialTheme.typography.bodySmall,
                                color = HospitalIndigo
                            )
                            Button(
                                onClick = {
                                    logoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("admin_upload_logo_btn")
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (strings.isArabic) "اختيار ورفع شعار المستشفى" else "Upload Official Hospital Logo")
                            }
                        }
                    }
                }

                // Admin: Publish Editorial / Local Hospital Protocol
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Publish Editorial or Local Hospital Protocol", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(value = edTitle, onValueChange = { edTitle = it }, label = { Text("Title (English)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = edTitleAr, onValueChange = { edTitleAr = it }, label = { Text("Title (Arabic, optional)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = edSpecialty, onValueChange = { edSpecialty = it }, label = { Text("Specialty") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = edSummary, onValueChange = { edSummary = it }, label = { Text("Summary / Body") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tag as Local Al-Anhar Hospital Protocol", style = MaterialTheme.typography.bodySmall)
                                Switch(checked = edIsHospitalProtocol, onCheckedChange = { edIsHospitalProtocol = it })
                            }
                            Button(
                                onClick = {
                                    if (edTitle.isNotBlank() && edSummary.isNotBlank()) {
                                        onPublishEditorial(
                                            EditorialPost(
                                                title = edTitle,
                                                titleAr = edTitleAr,
                                                summary = edSummary,
                                                body = edSummary,
                                                specialty = edSpecialty,
                                                contentType = if (edIsHospitalProtocol) "LOCAL_HOSPITAL_PROTOCOL" else "EDITORIAL_ANNOUNCEMENT",
                                                authorName = profile?.displayName ?: "Hospital Editorial Board",
                                                hospitalProtocol = edIsHospitalProtocol,
                                                publicationDate = "2026-11-19",
                                                verifiedDate = "2026-11-19"
                                            )
                                        )
                                        edTitle = ""
                                        edTitleAr = ""
                                        edSummary = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Publish to Journal")
                            }
                        }
                    }
                }

                // Admin: Add Scientific Category
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Manage Scientific Categories", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(value = catNameEn, onValueChange = { catNameEn = it }, label = { Text("Category Name (English)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = catNameAr, onValueChange = { catNameAr = it }, label = { Text("Category Name (Arabic)") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = catTrack == "RESPIRATORY_CARE", onClick = { catTrack = "RESPIRATORY_CARE" }, label = { Text("Respiratory") })
                                FilterChip(selected = catTrack == "CRITICAL_CARE", onClick = { catTrack = "CRITICAL_CARE" }, label = { Text("Critical Care") })
                            }
                            Button(onClick = {
                                if (catNameEn.isNotBlank()) {
                                    onUpsertCategory(
                                        ScientificCategory(
                                            nameEn = catNameEn,
                                            nameAr = catNameAr.ifBlank { catNameEn },
                                            parentTrack = catTrack,
                                            searchQueryTerms = catNameEn,
                                            sortOrder = 99L,
                                            active = true
                                        )
                                    )
                                    catNameEn = ""
                                    catNameAr = ""
                                }
                            }) {
                                Text("Save Category")
                            }
                        }
                    }
                }

                // Admin: Add Protocol / Guideline Record
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Publish Clinical Protocol / Guideline Record", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(value = guideTitle, onValueChange = { guideTitle = it }, label = { Text("Protocol / Guideline Title") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = guideOrg, onValueChange = { guideOrg = it }, label = { Text("Issuing Body (e.g., Al-Anhar Hospital / ATS)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = guideTopic, onValueChange = { guideTopic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = guideRecs, onValueChange = { guideRecs = it }, label = { Text("Key Recommendations") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Button(onClick = {
                                if (guideTitle.isNotBlank() && guideRecs.isNotBlank()) {
                                    val isLocal = guideOrg.contains("Anhar", ignoreCase = true)
                                    onUpsertGuideline(
                                        GuidelineRecord(
                                            title = guideTitle,
                                            organization = guideOrg,
                                            topic = guideTopic,
                                            recommendations = guideRecs,
                                            latestUpdateDate = "2026-11-19",
                                            lastVerifiedDate = "2026-11-19",
                                            protocolScope = if (isLocal) "LOCAL_HOSPITAL_PROTOCOL" else "INTERNATIONAL_GUIDELINE"
                                        )
                                    )
                                    guideTitle = ""
                                    guideRecs = ""
                                }
                            }) {
                                Text("Save Guideline / Protocol")
                            }
                        }
                    }
                }

                // Admin: Users & Sync Logs
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Registered Clinicians (${adminUsers.size}) & Sync Logs (${syncLogs.size})", style = MaterialTheme.typography.titleSmall)
                        if (syncLogs.isNotEmpty()) {
                            OutlinedButton(onClick = onClearSyncLogs) { Text("Clear Logs") }
                        }
                    }
                }
                items(syncLogs.take(15), key = { it.id }) { log ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("${log.sourceName} • ${log.status} (${log.discoveredCount} items)", style = MaterialTheme.typography.labelMedium)
                            Text(log.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Explicit Sign Out Button at bottom for easy reach
            item {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("profile_sign_out_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.signOutButton)
                }
            }
        }
    }
}
