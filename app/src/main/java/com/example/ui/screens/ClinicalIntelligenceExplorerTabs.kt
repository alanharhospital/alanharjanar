package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.ClinicalCaseAnalysisResult
import com.example.domain.model.DiagnosisKnowledgeReport
import com.example.domain.model.EvidenceComparisonResult
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.LibraryItem
import com.example.domain.model.ResearchArticle
import com.example.domain.model.TimeFilter
import com.example.domain.model.UiState
import com.example.domain.taxonomy.MedicalTaxonomy
import com.example.ui.components.ResearchArticleCard
import com.example.ui.components.StatusPill
import com.example.ui.components.openExternalUrl
import com.example.ui.components.shareArticleText
import com.example.ui.theme.ClinicalMonoFamily
import com.example.ui.theme.HospitalIndigo
import com.example.ui.theme.HospitalIndigoBg
import com.example.ui.theme.OpenAccessEmerald
import com.example.ui.theme.OpenAccessEmeraldBg
import com.example.ui.theme.PreprintAmber
import com.example.ui.theme.PreprintAmberBg

@Composable
fun DiagnosisExplorerTab(
    query: String,
    onQueryChange: (String) -> Unit,
    diagnosisReportState: UiState<DiagnosisKnowledgeReport>?,
    diagnosisStudies: List<ResearchArticle>,
    libraryItems: List<LibraryItem>,
    onExplore: (Boolean) -> Unit,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleArticleBookmark: (ResearchArticle, LibraryItem?) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bookmarkedMap = remember(libraryItems) { libraryItems.associateBy { it.articleKey } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (strings.isArabic) "مستكشف التشخيص الذكي (تقرير من 17 محوراً)" else "Intelligent Diagnosis Explorer (17-Point Clinical Knowledge Report)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = {
                            Text(if (strings.isArabic) "أدخل تشخيصاً، شكوى، أو حالة تنفسية/حرجة" else "Enter diagnosis, complaint, or respiratory/ICU condition")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("diagnosis_explorer_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onExplore(false) },
                            modifier = Modifier.weight(1f).testTag("diagnosis_explore_btn")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "إنشاء التقرير" else "Generate Report")
                        }
                        OutlinedButton(onClick = { onExplore(true) }) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (strings.isArabic) "حفظ" else "Save Topic")
                        }
                    }
                }
            }
        }

        when (diagnosisReportState) {
            null -> {
                item {
                    Text(
                        text = if (strings.isArabic) "أمثلة سريعة:" else "Quick Clinical Topics:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    val quickTopics = listOf("ARDS", "COPD Exacerbation", "Septic Shock", "Acute Respiratory Failure", "Pulmonary Embolism", "Ventilator-Associated Pneumonia")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickTopics) { topic ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onQueryChange(topic)
                                    onExplore(false)
                                },
                                label = { Text(topic) }
                            )
                        }
                    }
                }
            }
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
                            text = diagnosisReportState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val r = diagnosisReportState.data
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HospitalIndigoBg)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(r.topicTitle, style = MaterialTheme.typography.titleMedium, color = HospitalIndigo, fontWeight = FontWeight.Bold)
                            Text(
                                text = r.criteriaVsImpressionDistinction,
                                style = MaterialTheme.typography.bodySmall,
                                color = HospitalIndigo
                            )
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReportSectionBlock("1. Definition", r.definition)
                            ReportSectionBlock("2. Epidemiology", r.epidemiology)
                            ReportSectionBlock("3. Pathophysiology", r.pathophysiology)
                            ReportSectionBlock("4. Risk Factors", r.riskFactors)
                            ReportSectionBlock("5. Clinical Presentation", r.clinicalPresentation)
                            ReportSectionBlock("6. Diagnostic Criteria", r.diagnosticCriteria)
                            ReportSectionBlock("7. Disease Classification", r.diseaseClassification)
                            ReportSectionBlock("8. Severity Assessment", r.severityAssessment)
                            ReportSectionBlock("9. Differential Diagnoses", r.differentialDiagnoses)
                            ReportSectionBlock("10. Relevant Investigations", r.relevantInvestigations)
                            ReportSectionBlock("11. Clinical Assessment Considerations", r.clinicalAssessmentConsiderations)
                            ReportSectionBlock("12. Potential Complications", r.potentialComplications)
                            ReportSectionBlock("13. Evidence-Based Management Principles", r.evidenceBasedManagementPrinciples)
                            ReportSectionBlock("14. Monitoring Considerations", r.monitoringConsiderations)
                            ReportSectionBlock("15. Prognostic Indicators", r.prognosticIndicators)
                            ReportSectionBlock("16. Latest Clinical Guidelines", r.latestGuidelinesSummary)
                            if (r.traceableReferences.isNotEmpty()) {
                                Text("17. Latest Research Publications & References", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                r.traceableReferences.forEach { ref ->
                                    Text("• $ref", style = MaterialTheme.typography.labelSmall.copy(fontFamily = ClinicalMonoFamily))
                                }
                            }
                        }
                    }
                }
                items(diagnosisStudies.take(6), key = { "dx_study_${it.articleKey}" }) { article ->
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuidelinesAndProtocolsTab(guidelines: List<GuidelineRecord>) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var selectedSociety by remember { mutableStateOf("ALL") }
    var selectedScope by remember { mutableStateOf("ALL") }

    val filtered = remember(guidelines, selectedSociety, selectedScope) {
        guidelines.filter { g ->
            val matchSoc = selectedSociety == "ALL" || g.organization.contains(selectedSociety, ignoreCase = true)
            val matchScope = selectedScope == "ALL" || g.protocolScope == selectedScope
            matchSoc && matchScope
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (strings.isArabic) "الإرشادات السريرية الدولية وبروتوكولات المستشفى" else "International Clinical Guidelines & Medical Protocol Library",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedSociety == "ALL",
                        onClick = { selectedSociety = "ALL" },
                        label = { Text(if (strings.isArabic) "الكل" else "All Societies") }
                    )
                }
                items(MedicalTaxonomy.guidelineSocieties) { soc ->
                    FilterChip(
                        selected = selectedSociety == soc,
                        onClick = { selectedSociety = soc },
                        label = { Text(soc) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val scopes = listOf(
                    "ALL" to (if (strings.isArabic) "كل المصادر" else "All Scopes"),
                    "INTERNATIONAL_GUIDELINE" to (if (strings.isArabic) "إرشادات دولية" else "International Guideline"),
                    "LOCAL_HOSPITAL_PROTOCOL" to (if (strings.isArabic) "بروتوكول مستشفى الأنهار" else "Local Hospital Protocol"),
                    "EDUCATIONAL_SUMMARY" to (if (strings.isArabic) "ملخص تعليمي" else "Educational Summary")
                )
                scopes.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedScope == key,
                        onClick = { selectedScope = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { g ->
            val (bg, fg, scopeLabel) = when (g.protocolScope) {
                "LOCAL_HOSPITAL_PROTOCOL" -> Triple(HospitalIndigoBg, HospitalIndigo, "Al-Anhar Local Hospital Protocol")
                "EDUCATIONAL_SUMMARY" -> Triple(PreprintAmberBg, PreprintAmber, "Educational Summary")
                else -> Triple(OpenAccessEmeraldBg, OpenAccessEmerald, "International Guideline")
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(scopeLabel, bg, fg, Icons.Filled.Verified)
                        StatusPill(g.organization, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        StatusPill(g.publicationYear.toString(), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(g.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${g.topic} • Updated: ${g.latestUpdateDate.ifBlank { g.publicationYear.toString() }} • Verified: ${g.lastVerifiedDate}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = ClinicalMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(g.recommendations, style = MaterialTheme.typography.bodySmall)
                    if (g.evidenceCertainty.isNotBlank()) {
                        Text(
                            text = "Evidence Certainty: ${g.evidenceCertainty}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (g.changesSummary.isNotBlank()) {
                        Text(
                            text = "Updates vs Earlier Version: ${g.changesSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (g.sourceUrl.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = { openExternalUrl(context, g.sourceUrl) }) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (strings.isArabic) "المصدر الرسمي" else "Official Source")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LatestEvidenceAndComparisonTab(
    latestStudiesState: UiState<List<ResearchArticle>>?,
    evidenceComparisonState: UiState<EvidenceComparisonResult>?,
    libraryItems: List<LibraryItem>,
    onFetchEvidence: (String, TimeFilter) -> Unit,
    onCompareEvidence: (String) -> Unit,
    onOpenArticle: (ResearchArticle) -> Unit,
    onToggleArticleBookmark: (ResearchArticle, LibraryItem?) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val bookmarkedMap = remember(libraryItems) { libraryItems.associateBy { it.articleKey } }
    var topicQuery by remember { mutableStateOf("ARDS mechanical ventilation sepsis") }
    var selectedWindow by remember { mutableStateOf(TimeFilter.LAST_3_MONTHS) }

    val windows = listOf(
        TimeFilter.LAST_30_DAYS,
        TimeFilter.LAST_3_MONTHS,
        TimeFilter.LAST_6_MONTHS,
        TimeFilter.LAST_12_MONTHS,
        TimeFilter.LAST_5_YEARS
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (strings.isArabic) "أحدث الأدلة السريرية ومقارنة التحديثات" else "Latest Clinical Evidence & Guideline Comparison",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedTextField(
                        value = topicQuery,
                        onValueChange = { topicQuery = it },
                        label = { Text(if (strings.isArabic) "الموضوع السريري" else "Clinical Topic (e.g., ARDS, Sepsis, NIV, Weaning)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        windows.forEach { tf ->
                            FilterChip(
                                selected = selectedWindow == tf,
                                onClick = { selectedWindow = tf },
                                label = { Text(if (strings.isArabic) tf.labelAr else tf.labelEn) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onFetchEvidence(topicQuery, selectedWindow) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (strings.isArabic) "جلب الأدلة" else "Fetch Studies")
                        }
                        OutlinedButton(
                            onClick = { onCompareEvidence(topicQuery) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (strings.isArabic) "مقارنة الإرشادات" else "Compare Versions")
                        }
                    }
                }
            }
        }

        // Evidence Comparison Card
        when (evidenceComparisonState) {
            null -> Unit
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(evidenceComparisonState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                }
            }
            is UiState.Success -> {
                val c = evidenceComparisonState.data
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = HospitalIndigoBg)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${c.topic}: ${c.earlierGuidelineLabel} vs ${c.newerGuidelineLabel}",
                                style = MaterialTheme.typography.titleSmall,
                                color = HospitalIndigo,
                                fontWeight = FontWeight.Bold
                            )
                            Text(c.verificationNote, style = MaterialTheme.typography.labelSmall, color = HospitalIndigo)
                            if (c.areasOfAgreement.isNotEmpty()) {
                                Text("Areas of Agreement:", style = MaterialTheme.typography.labelLarge, color = HospitalIndigo)
                                c.areasOfAgreement.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = HospitalIndigo) }
                            }
                            if (c.areasOfDisagreement.isNotEmpty()) {
                                Text("Evolving / Divergent Evidence:", style = MaterialTheme.typography.labelLarge, color = HospitalIndigo)
                                c.areasOfDisagreement.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = HospitalIndigo) }
                            }
                            if (c.updatedRecommendations.isNotEmpty()) {
                                Text("Updated Practice Recommendations:", style = MaterialTheme.typography.labelLarge, color = HospitalIndigo)
                                c.updatedRecommendations.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = HospitalIndigo) }
                            }
                        }
                    }
                }
            }
        }

        when (latestStudiesState) {
            null -> Unit
            is UiState.Loading -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(latestStudiesState.message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp))
                }
            }
            is UiState.Success -> {
                items(latestStudiesState.data, key = { "ev_${it.articleKey}" }) { article ->
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
fun DifferentialAndChecklistTab(
    caseAnalysisState: UiState<ClinicalCaseAnalysisResult>?,
    onGoToCaseAnalyzer: () -> Unit,
    onExportReportPdf: () -> Unit
) {
    val strings = LocalAppStrings.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (caseAnalysisState) {
            is UiState.Success -> {
                val res = caseAnalysisState.data
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isArabic) "مستكشف التشخيص التفريقي" else "Differential Diagnosis Explorer",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(onClick = onExportReportPdf) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                    }
                }
                items(res.differentialDiagnoses) { diff ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(diff.conditionName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(diff.definition, style = MaterialTheme.typography.bodySmall)
                            if (diff.supportingFeatures.isNotEmpty()) {
                                Text("Supporting Features: ${diff.supportingFeatures.joinToString("; ")}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (diff.featuresArguingAgainst.isNotEmpty()) {
                                Text("Arguing Against: ${diff.featuresArguingAgainst.joinToString("; ")}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (diff.diagnosticCriteria.isNotBlank()) {
                                Text("Criteria: ${diff.diagnosticCriteria}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (diff.evaluationInvestigations.isNotEmpty()) {
                                Text("Investigations: ${diff.evaluationInvestigations.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
                if (res.assessmentChecklist.isNotEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (strings.isArabic) "قائمة التقييم السريري المرجعية" else "Clinical Assessment Checklist",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                res.assessmentChecklist.forEach { step ->
                                    Text("☐ $step", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (res.relevantInvestigations.isNotEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (strings.isArabic) "الفحوصات ذات الصلة للمناقشة" else "Relevant Diagnostic Considerations",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                res.relevantInvestigations.forEach { inv ->
                                    Text("• $inv", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                item {
                    Card {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (strings.isArabic) "قم بتشغيل محلل الحالات أولاً لعرض التشخيص التفريقي وقائمة التقييم السريري المخصصة للحالة."
                                else "Run the Clinical Case Analyzer first to generate tailored differential diagnoses, missing-data checks, and assessment checklists.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(onClick = onGoToCaseAnalyzer) {
                                Text(if (strings.isArabic) "الانتقال إلى محلل الحالات" else "Go to Case Analyzer")
                            }
                        }
                    }
                }
            }
        }
    }
}
