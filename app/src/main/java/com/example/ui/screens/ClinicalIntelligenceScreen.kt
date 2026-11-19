package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.ClinicalCaseAnalysisResult
import com.example.domain.model.ClinicalCaseInput
import com.example.domain.model.ClinicalCaseSession
import com.example.domain.model.DiagnosisKnowledgeReport
import com.example.domain.model.EvidenceComparisonResult
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.LibraryItem
import com.example.domain.model.MissingPriority
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SafetyAlertLevel
import com.example.domain.model.SafetyScreenResult
import com.example.domain.model.SavedClinicalTopic
import com.example.domain.model.TimeFilter
import com.example.domain.model.UiState
import com.example.domain.taxonomy.MedicalTaxonomy
import com.example.ui.components.ResearchArticleCard
import com.example.ui.components.StatusPill
import com.example.ui.components.shareArticleText
import com.example.ui.theme.ClinicalMonoFamily
import com.example.ui.theme.HospitalIndigo
import com.example.ui.theme.HospitalIndigoBg
import com.example.ui.theme.MedicalNavy
import com.example.ui.theme.OpenAccessEmerald
import com.example.ui.theme.OpenAccessEmeraldBg
import com.example.ui.theme.PreprintAmber
import com.example.ui.theme.PreprintAmberBg
import com.example.ui.theme.PureWhite
import com.example.ui.theme.UrgentCrimson
import com.example.ui.theme.UrgentCrimsonBg

@Composable
fun ClinicalIntelligenceScreen(
    caseInput: ClinicalCaseInput,
    safetyScreen: SafetyScreenResult,
    caseAnalysisState: UiState<ClinicalCaseAnalysisResult>?,
    caseSupportingStudies: List<ResearchArticle>,
    diagnosisReportState: UiState<DiagnosisKnowledgeReport>?,
    diagnosisStudies: List<ResearchArticle>,
    evidenceComparisonState: UiState<EvidenceComparisonResult>?,
    latestClinicalStudiesState: UiState<List<ResearchArticle>>?,
    savedCasesState: UiState<List<ClinicalCaseSession>>,
    savedTopicsState: UiState<List<SavedClinicalTopic>>,
    guidelines: List<GuidelineRecord>,
    libraryItems: List<LibraryItem>,
    reportExportMessage: String?,
    onUpdateCaseInput: ((ClinicalCaseInput) -> ClinicalCaseInput) -> Unit,
    onClearCaseInput: () -> Unit,
    onAnalyzeCase: () -> Unit,
    onDeleteSavedCase: (String) -> Unit,
    onExploreDiagnosis: (String, Boolean) -> Unit,
    onToggleTopicBookmark: (String, String, Boolean) -> Unit,
    onFetchLatestEvidence: (String, TimeFilter) -> Unit,
    onCompareEvidence: (String) -> Unit,
    onExportReportPdf: () -> Unit,
    onClearExportMessage: () -> Unit,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleArticleBookmark: (ResearchArticle, LibraryItem?) -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var diagnosisQuery by remember { mutableStateOf("") }

    val subTabs = if (strings.isArabic) {
        listOf(
            "اللوحة السريرية",
            "محلل الحالات",
            "مستكشف التشخيص",
            "الإرشادات والبروتوكولات",
            "أحدث الأدلة والمقارنة",
            "التشخيص التفريقي وقائمة التقييم"
        )
    } else {
        listOf(
            "Dashboard",
            "Case Analyzer",
            "Diagnosis Explorer",
            "Guidelines & Protocols",
            "Evidence & Compare",
            "Differential & Checklist"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            edgePadding = 12.dp
        ) {
            subTabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = { Text(label) },
                    modifier = Modifier.testTag("clinical_subtab_$idx")
                )
            }
        }

        if (!reportExportMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reportExportMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearExportMessage) {
                        Text(if (strings.isArabic) "إغلاق" else "Dismiss")
                    }
                }
            }
        }

        when (selectedSubTab) {
            0 -> ClinicalDashboardTab(
                savedCasesState = savedCasesState,
                savedTopicsState = savedTopicsState,
                onSearchDiagnosis = { query ->
                    diagnosisQuery = query
                    onExploreDiagnosis(query, false)
                    selectedSubTab = 2
                },
                onOpenCaseAnalyzer = { selectedSubTab = 1 },
                onOpenGuidelines = { selectedSubTab = 3 },
                onOpenLatestEvidence = { selectedSubTab = 4 },
                onDeleteSavedCase = onDeleteSavedCase,
                onToggleTopicBookmark = onToggleTopicBookmark
            )
            1 -> CaseAnalyzerTab(
                caseInput = caseInput,
                safetyScreen = safetyScreen,
                caseAnalysisState = caseAnalysisState,
                supportingStudies = caseSupportingStudies,
                libraryItems = libraryItems,
                onUpdateCaseInput = onUpdateCaseInput,
                onClearCaseInput = onClearCaseInput,
                onAnalyzeCase = onAnalyzeCase,
                onExportReportPdf = onExportReportPdf,
                onOpenDifferentialTab = { selectedSubTab = 5 },
                onOpenArticle = onOpenArticle,
                onToggleArticleBookmark = onToggleArticleBookmark
            )
            2 -> DiagnosisExplorerTab(
                query = diagnosisQuery,
                onQueryChange = { diagnosisQuery = it },
                diagnosisReportState = diagnosisReportState,
                diagnosisStudies = diagnosisStudies,
                libraryItems = libraryItems,
                onExplore = { bookmark -> onExploreDiagnosis(diagnosisQuery, bookmark) },
                onOpenArticle = onOpenArticle,
                onToggleArticleBookmark = onToggleArticleBookmark
            )
            3 -> GuidelinesAndProtocolsTab(guidelines = guidelines)
            4 -> LatestEvidenceAndComparisonTab(
                latestStudiesState = latestClinicalStudiesState,
                evidenceComparisonState = evidenceComparisonState,
                libraryItems = libraryItems,
                onFetchEvidence = onFetchLatestEvidence,
                onCompareEvidence = onCompareEvidence,
                onOpenArticle = onOpenArticle,
                onToggleArticleBookmark = onToggleArticleBookmark
            )
            5 -> DifferentialAndChecklistTab(
                caseAnalysisState = caseAnalysisState,
                onGoToCaseAnalyzer = { selectedSubTab = 1 },
                onExportReportPdf = onExportReportPdf
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClinicalDashboardTab(
    savedCasesState: UiState<List<ClinicalCaseSession>>,
    savedTopicsState: UiState<List<SavedClinicalTopic>>,
    onSearchDiagnosis: (String) -> Unit,
    onOpenCaseAnalyzer: () -> Unit,
    onOpenGuidelines: () -> Unit,
    onOpenLatestEvidence: () -> Unit,
    onDeleteSavedCase: (String) -> Unit,
    onToggleTopicBookmark: (String, String, Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    var quickQuery by remember { mutableStateOf("") }
    val savedCases = (savedCasesState as? UiState.Success)?.data.orEmpty()
    val savedTopics = (savedTopicsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_clinical_intel),
                    contentDescription = strings.clinicalTitle,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(MedicalNavy.copy(alpha = 0.92f), MedicalNavy.copy(alpha = 0.74f))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Text(
                            text = strings.clinicalTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.clinicalSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Clinical Knowledge-Support Disclaimer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PreprintAmberBg)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = PreprintAmber)
                    Text(
                        text = strings.clinicalDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = PreprintAmber
                    )
                }
            }
        }

        // Quick Search Diagnosis
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (strings.isArabic) "استكشاف تشخيص أو حالة تنفسية / حرجة" else "Explore a Diagnosis or Clinical Condition",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedTextField(
                        value = quickQuery,
                        onValueChange = { quickQuery = it },
                        label = {
                            Text(if (strings.isArabic) "مثال: ARDS, COPD, Sepsis, Ventilator Weaning..." else "e.g., ARDS, Sepsis, COPD Exacerbation, Pulmonary Embolism")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("clinical_dash_search_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (quickQuery.isNotBlank()) onSearchDiagnosis(quickQuery) },
                            modifier = Modifier.testTag("clinical_dash_explore_btn")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "استكشاف التشخيص" else "Explore Diagnosis")
                        }
                        OutlinedButton(
                            onClick = onOpenCaseAnalyzer,
                            modifier = Modifier.testTag("clinical_dash_case_btn")
                        ) {
                            Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "وصف حالة سريرية" else "Analyze Case")
                        }
                    }
                }
            }
        }

        // Separate Respiratory Care vs Critical Care Clinical Knowledge Sections
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.respiratoryCare,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        MedicalTaxonomy.protocolTopicsRespiratory.take(5).forEach { topic ->
                            Text(
                                text = "• $topic",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSearchDiagnosis(topic) }
                                    .padding(vertical = 3.dp)
                            )
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = strings.criticalCare,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        MedicalTaxonomy.protocolTopicsCriticalCare.take(5).forEach { topic ->
                            Text(
                                text = "• $topic",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSearchDiagnosis(topic) }
                                    .padding(vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Module Links
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onOpenGuidelines, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.LocalLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (strings.isArabic) "البروتوكولات" else "Protocols")
                }
                OutlinedButton(onClick = onOpenLatestEvidence, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (strings.isArabic) "أحدث الأدلة" else "Latest Evidence")
                }
            }
        }

        // Saved & Recently Viewed Clinical Topics
        if (savedTopics.isNotEmpty()) {
            item {
                Text(
                    text = if (strings.isArabic) "المواضيع السريرية المحفوظة والمشاهدة حديثاً" else "Saved & Recently Viewed Clinical Topics",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    savedTopics.take(10).forEach { topic ->
                        FilterChip(
                            selected = topic.bookmarked,
                            onClick = { onSearchDiagnosis(topic.topicName) },
                            label = { Text(topic.topicName) },
                            leadingIcon = {
                                IconButton(
                                    onClick = { onToggleTopicBookmark(topic.topicName, topic.specialtyTrack, !topic.bookmarked) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(Icons.Filled.Bookmark, contentDescription = "Toggle bookmark", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Saved De-identified Case Sessions
        item {
            Text(
                text = if (strings.isArabic) "جلسات الحالات المحفوظة (مجهولة الهوية)" else "Saved De-Identified Clinical Case Sessions",
                style = MaterialTheme.typography.titleMedium
            )
        }
        if (savedCases.isEmpty()) {
            item {
                Card {
                    Text(
                        text = if (strings.isArabic) "لا توجد جلسات حالات محفوظة بعد. استخدم محلل الحالات لإنشاء تقييم علمي جديد."
                        else "No saved clinical case sessions yet. Use Case Analyzer to run an evidence-based educational assessment.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(savedCases, key = { it.id }) { session ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.caseTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${session.preliminaryDiagnosis} • Safety: ${session.safetyAlertLevel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (session.aiReportSummary.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = session.aiReportSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteSavedCase(session.id) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete case session")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaseAnalyzerTab(
    caseInput: ClinicalCaseInput,
    safetyScreen: SafetyScreenResult,
    caseAnalysisState: UiState<ClinicalCaseAnalysisResult>?,
    supportingStudies: List<ResearchArticle>,
    libraryItems: List<LibraryItem>,
    onUpdateCaseInput: ((ClinicalCaseInput) -> ClinicalCaseInput) -> Unit,
    onClearCaseInput: () -> Unit,
    onAnalyzeCase: () -> Unit,
    onExportReportPdf: () -> Unit,
    onOpenDifferentialTab: () -> Unit,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleArticleBookmark: (ResearchArticle, LibraryItem?) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bookmarkedMap = remember(libraryItems) { libraryItems.associateBy { it.articleKey } }

    var expandA by remember { mutableStateOf(true) }
    var expandB by remember { mutableStateOf(true) }
    var expandC by remember { mutableStateOf(true) }
    var expandD by remember { mutableStateOf(false) }
    var expandE by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Zero-PII Privacy Banner
        item {
            Card(colors = CardDefaults.cardColors(containerColor = HospitalIndigoBg)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = HospitalIndigo)
                    Text(
                        text = strings.privacyNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = HospitalIndigo
                    )
                }
            }
        }

        // Real-time Deterministic Clinical Safety Alert Banner
        if (safetyScreen.overallLevel != SafetyAlertLevel.NORMAL) {
            item {
                val bg = if (safetyScreen.overallLevel == SafetyAlertLevel.URGENT) UrgentCrimsonBg else PreprintAmberBg
                val fg = if (safetyScreen.overallLevel == SafetyAlertLevel.URGENT) UrgentCrimson else PreprintAmber
                Card(colors = CardDefaults.cardColors(containerColor = bg)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = fg)
                            Text(
                                text = strings.urgentSafetyTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = fg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(text = strings.urgentSafetyAdvice, style = MaterialTheme.typography.bodySmall, color = fg)
                        safetyScreen.flags.forEach { flag ->
                            Text(
                                text = "• ${flag.parameter} (${flag.observedValue}): ${flag.clinicalConcern}",
                                style = MaterialTheme.typography.labelMedium,
                                color = fg
                            )
                        }
                    }
                }
            }
        }

        // Section A: Basic Patient Info (Optional, De-identified)
        item {
            CollapsibleCaseSection(
                title = if (strings.isArabic) "أ. المعلومات الأساسية للمريض (اختيارية ومجهولة الهوية)" else "A. Basic Patient Information (Optional, De-Identified)",
                expanded = expandA,
                onToggle = { expandA = !expandA }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.ageYears,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(ageYears = v) } },
                        label = { Text(if (strings.isArabic) "العمر" else "Age") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.demographicsNote,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(demographicsNote = v) } },
                        label = { Text(if (strings.isArabic) "ملاحظات ديموغرافية" else "Sex / Context") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = caseInput.chiefComplaint,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(chiefComplaint = v) } },
                    label = { Text(if (strings.isArabic) "الشكوى الرئيسية" else "Chief Complaint") },
                    modifier = Modifier.fillMaxWidth().testTag("case_chief_complaint")
                )
                OutlinedTextField(
                    value = caseInput.preliminaryDiagnosis,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(preliminaryDiagnosis = v) } },
                    label = { Text(if (strings.isArabic) "التشخيص المبدئي" else "Preliminary Diagnosis / Impression") },
                    modifier = Modifier.fillMaxWidth().testTag("case_prelim_dx")
                )
                OutlinedTextField(
                    value = caseInput.medicalHistory,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(medicalHistory = v) } },
                    label = { Text(if (strings.isArabic) "التاريخ المرضي" else "Medical History") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.comorbidities,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(comorbidities = v) } },
                    label = { Text(if (strings.isArabic) "الأمراض المصاحبة" else "Comorbidities") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section B: Clinical Assessment
        item {
            CollapsibleCaseSection(
                title = if (strings.isArabic) "ب. التقييم السريري والعلامات الحيوية" else "B. Clinical Assessment & Vital Signs",
                expanded = expandB,
                onToggle = { expandB = !expandB }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.heartRateBpm,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(heartRateBpm = v) } },
                        label = { Text("HR (bpm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.respiratoryRateBpm,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(respiratoryRateBpm = v) } },
                        label = { Text("RR (/min)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.spo2Percent,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(spo2Percent = v) } },
                        label = { Text("SpO2 (%)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.bloodPressureMmHg,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(bloodPressureMmHg = v) } },
                        label = { Text("BP (mmHg)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.temperatureCelsius,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(temperatureCelsius = v) } },
                        label = { Text("Temp (°C)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = caseInput.consciousnessLevel,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(consciousnessLevel = v) } },
                    label = { Text(if (strings.isArabic) "مستوى الوعي (GCS / AVPU)" else "Consciousness Level (GCS / AVPU)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = caseInput.physicalExamFindings,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(physicalExamFindings = v) } },
                    label = { Text(if (strings.isArabic) "نتائج الفحص السريري" else "Physical Examination Findings") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section C: Respiratory & ABG Assessment
        item {
            CollapsibleCaseSection(
                title = if (strings.isArabic) "ج. التقييم التنفسي وغازات الدم الشرياني (ABG)" else "C. Respiratory, ABG & Ventilator Assessment",
                expanded = expandC,
                onToggle = { expandC = !expandC }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.abgPh,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(abgPh = v) } },
                        label = { Text("pH") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.paco2MmHg,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(paco2MmHg = v) } },
                        label = { Text("PaCO2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.pao2MmHg,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(pao2MmHg = v) } },
                        label = { Text("PaO2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.hco3MeqL,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(hco3MeqL = v) } },
                        label = { Text("HCO3-") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.sao2Percent,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(sao2Percent = v) } },
                        label = { Text("SaO2%") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.lactateMmolL,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(lactateMmolL = v) } },
                        label = { Text("Lactate") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.fio2Fraction,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(fio2Fraction = v) } },
                        label = { Text("FiO2") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.peepCmH2o,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(peepCmH2o = v) } },
                        label = { Text("PEEP") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.tidalVolumeMl,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(tidalVolumeMl = v) } },
                        label = { Text("Vt (mL)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = caseInput.ventilatorMode,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(ventilatorMode = v) } },
                        label = { Text(if (strings.isArabic) "نمط التنفس" else "Vent Mode") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = caseInput.airwayStatus,
                        onValueChange = { v -> onUpdateCaseInput { it.copy(airwayStatus = v) } },
                        label = { Text(if (strings.isArabic) "مجرى الهواء" else "Airway Status") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = caseInput.oxygenDeliveryMethod,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(oxygenDeliveryMethod = v) } },
                    label = { Text(if (strings.isArabic) "وسيلة توصيل الأكسجين (HFNC, NIV, ETT...)" else "Oxygen Delivery Method (NC, Mask, HFNC, NIV, ETT)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Section D: Laboratory Investigations
        item {
            CollapsibleCaseSection(
                title = if (strings.isArabic) "د. الفحوصات المخبرية" else "D. Laboratory Investigations",
                expanded = expandD,
                onToggle = { expandD = !expandD }
            ) {
                OutlinedTextField(
                    value = caseInput.cbc,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(cbc = v) } },
                    label = { Text("CBC") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.electrolytes,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(electrolytes = v) } },
                    label = { Text(if (strings.isArabic) "الشوارد" else "Electrolytes") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.renalFunction,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(renalFunction = v) } },
                    label = { Text(if (strings.isArabic) "وظائف الكلى" else "Renal Function") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.liverFunction,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(liverFunction = v) } },
                    label = { Text(if (strings.isArabic) "وظائف الكبد" else "Liver Function") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.inflammatoryMarkers,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(inflammatoryMarkers = v) } },
                    label = { Text(if (strings.isArabic) "مؤشرات الالتهاب (CRP, PCT, D-dimer)" else "Inflammatory Markers (CRP, PCT, D-dimer)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.microbiology,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(microbiology = v) } },
                    label = { Text(if (strings.isArabic) "الميكروبيولوجيا والزرع" else "Microbiology") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.otherLabs,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(otherLabs = v) } },
                    label = { Text(if (strings.isArabic) "فحوصات أخرى" else "Other Investigations") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section E: Imaging & Additional Info
        item {
            CollapsibleCaseSection(
                title = if (strings.isArabic) "هـ. الأشعة والتقارير الإضافية" else "E. Imaging & Additional Information",
                expanded = expandE,
                onToggle = { expandE = !expandE }
            ) {
                OutlinedTextField(
                    value = caseInput.chestXray,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(chestXray = v) } },
                    label = { Text(if (strings.isArabic) "الأشعة السينية للصدر (CXR)" else "Chest X-ray Findings") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.ctFindings,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(ctFindings = v) } },
                    label = { Text(if (strings.isArabic) "الأشعة المقطعية (CT)" else "CT Findings") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.ultrasoundFindings,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(ultrasoundFindings = v) } },
                    label = { Text(if (strings.isArabic) "الموجات فوق الصوتية (POCUS)" else "Lung / Bedside Ultrasound") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.echoFindings,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(echoFindings = v) } },
                    label = { Text(if (strings.isArabic) "تخطيط صدى القلب" else "Echocardiography Findings") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caseInput.otherReports,
                    onValueChange = { v -> onUpdateCaseInput { it.copy(otherReports = v) } },
                    label = { Text(if (strings.isArabic) "تقارير أخرى" else "Other Reports") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Action Buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onAnalyzeCase,
                    modifier = Modifier.weight(1f).testTag("case_analyze_submit_btn")
                ) {
                    Icon(Icons.Filled.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (strings.isArabic) "تحليل الحالة علمياً" else "Analyze Case")
                }
                OutlinedButton(
                    onClick = onClearCaseInput,
                    modifier = Modifier.testTag("case_clear_btn")
                ) {
                    Text(if (strings.isArabic) "مسح" else "Clear")
                }
            }
        }

        // Case Analysis Result
        when (caseAnalysisState) {
            null -> Unit
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
                            text = caseAnalysisState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val res = caseAnalysisState.data
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = res.identifiedTopic,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onExportReportPdf,
                                    modifier = Modifier.weight(1f).testTag("case_export_pdf_btn")
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (strings.isArabic) "تصدير التقرير PDF" else "Export Report PDF")
                                }
                                OutlinedButton(
                                    onClick = onOpenDifferentialTab,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (strings.isArabic) "التشخيص التفريقي" else "Differentials (${res.differentialDiagnoses.size})")
                                }
                            }
                            ReportSectionBlock(
                                title = if (strings.isArabic) "ملخص البيانات المتوفرة" else "Available Clinical Data Summary",
                                body = res.availableInformationSummary
                            )
                            ReportSectionBlock(
                                title = if (strings.isArabic) "الدليل الطبي الراسخ" else "Established Medical Evidence",
                                body = res.establishedMedicalEvidence
                            )
                            ReportSectionBlock(
                                title = if (strings.isArabic) "ملخص الإرشادات المنشورة" else "Published Guidelines Summary",
                                body = res.publishedGuidelinesSummary
                            )
                            ReportSectionBlock(
                                title = if (strings.isArabic) "نتائج الأبحاث الحديثة" else "Recent Research Findings",
                                body = res.researchFindingsSummary
                            )
                            Card(colors = CardDefaults.cardColors(containerColor = HospitalIndigoBg)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (strings.isArabic) "التفسير التعليمي المدعوم بالذكاء الاصطناعي" else "AI-Assisted Educational Interpretation (Distinct from Published Guidelines)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = HospitalIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = res.aiEducationalInterpretation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = HospitalIndigo
                                    )
                                }
                            }
                            if (res.missingInformation.isNotEmpty()) {
                                Text(
                                    text = if (strings.isArabic) "البيانات السريرية المفقودة" else "Missing Clinical Information Detector",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                res.missingInformation.forEach { miss ->
                                    val (bg, fg) = when (miss.priority) {
                                        MissingPriority.ESSENTIAL -> UrgentCrimsonBg to UrgentCrimson
                                        MissingPriority.IMPORTANT -> PreprintAmberBg to PreprintAmber
                                        MissingPriority.OPTIONAL -> OpenAccessEmeraldBg to OpenAccessEmerald
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatusPill(miss.priority.name, bg, fg)
                                        Text("${miss.fieldName}: ${miss.whyItMatters}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            if (res.traceableReferences.isNotEmpty()) {
                                Text(
                                    text = if (strings.isArabic) "المراجع العلمية الموثقة" else "Traceable Scientific References",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                res.traceableReferences.forEach { ref ->
                                    Text(
                                        text = "• $ref",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = ClinicalMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                items(supportingStudies.take(5), key = { "case_study_${it.articleKey}" }) { article ->
                    val lib = bookmarkedMap[article.articleKey]
                    ResearchArticleCard(
                        article = article,
                        isBookmarked = lib?.bookmarked == true,
                        onClick = { onOpenArticle(article) },
                        onToggleBookmark = { onToggleArticleBookmark(article, lib) },
                        onShare = { shareArticleText(context, article) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleCaseSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse section" else "Expand section"
                )
            }
            if (expanded) {
                content()
            }
        }
    }
}

@Composable
fun ReportSectionBlock(title: String, body: String) {
    if (body.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
