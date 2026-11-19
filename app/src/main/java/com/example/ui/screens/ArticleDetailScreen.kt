package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.LibraryItem
import com.example.domain.model.PaperAiAnalysis
import com.example.domain.model.ResearchArticle
import com.example.domain.model.UiState
import com.example.domain.model.UserCollection
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
import com.example.ui.theme.UrgentCrimson
import com.example.ui.theme.UrgentCrimsonBg

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArticleDetailScreen(
    article: ResearchArticle,
    libraryItem: LibraryItem?,
    collections: List<UserCollection>,
    paperAiState: UiState<PaperAiAnalysis>?,
    pdfDownloadStatus: String?,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateReadingStatus: (String) -> Unit,
    onSaveNotes: (String) -> Unit,
    onToggleCollection: (String) -> Unit,
    onDownloadPdf: () -> Unit,
    onOpenLocalPdf: (String) -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onFeatureArticle: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var noteDraft by remember(libraryItem?.personalNotes) {
        mutableStateOf(libraryItem?.personalNotes.orEmpty())
    }
    var showCollectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (strings.isArabic) "تفاصيل البحث العلمي" else "Publication Details", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareArticleText(context, article) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quality-control & OA pills
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (article.retracted) {
                        StatusPill(strings.retractedWarning, UrgentCrimsonBg, UrgentCrimson, Icons.Filled.Warning)
                    }
                    if (article.preprint) {
                        StatusPill(strings.preprintWarning, PreprintAmberBg, PreprintAmber, Icons.Filled.Warning)
                    }
                    if (article.openAccess) {
                        StatusPill(strings.openAccess, OpenAccessEmeraldBg, OpenAccessEmerald, Icons.Filled.CheckCircle)
                    }
                    if (article.pdfUrl.isNotBlank()) {
                        StatusPill(strings.legalPdfAvailable, HospitalIndigoBg, HospitalIndigo, Icons.Filled.PictureAsPdf)
                    }
                    article.contributingSources.forEach { src ->
                        StatusPill(src, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Title & Metadata
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(article.title, style = MaterialTheme.typography.headlineSmall)
                    Text(article.authors, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${article.journal} • ${article.publicationDate} • ${article.publicationType}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (article.doi.isNotBlank()) {
                        Text("DOI: ${article.doi}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = ClinicalMonoFamily))
                    }
                    if (article.pmid.isNotBlank()) {
                        Text("PMID: ${article.pmid}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = ClinicalMonoFamily))
                    }
                    Text(
                        text = strings.editorialDisclaimer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Action Buttons
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (article.publisherUrl.isNotBlank()) {
                        Button(
                            onClick = { openExternalUrl(context, article.publisherUrl) },
                            modifier = Modifier.testTag("detail_read_full_text")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "فتح المصدر الكامل" else "Read Full Text")
                        }
                    }

                    // NEVER create fake PDF download buttons: only render enabled download when legal PDF URL exists!
                    val localPdf = libraryItem?.localPdfPath.orEmpty()
                    if (localPdf.isNotBlank()) {
                        Button(
                            onClick = { onOpenLocalPdf(localPdf) },
                            modifier = Modifier.testTag("detail_open_local_pdf")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "فتح PDF المحفوظ" else "Open Downloaded PDF")
                        }
                    } else if (article.pdfUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = onDownloadPdf,
                            modifier = Modifier.testTag("detail_download_pdf")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "تنزيل PDF قانوني" else "Download Legal PDF")
                        }
                    }

                    OutlinedButton(onClick = onToggleSave, modifier = Modifier.testTag("detail_save_button")) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (libraryItem?.saved == true) (if (strings.isArabic) "محفوظ" else "Saved") else (if (strings.isArabic) "حفظ" else "Save Research"))
                    }

                    OutlinedButton(onClick = onToggleBookmark, modifier = Modifier.testTag("detail_bookmark_button")) {
                        Icon(
                            if (libraryItem?.bookmarked == true) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isArabic) "إشارة مرجعية" else "Bookmark")
                    }

                    OutlinedButton(onClick = onToggleFavorite) {
                        Icon(
                            if (libraryItem?.favorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isArabic) "مفضلة" else "Favorite")
                    }

                    OutlinedButton(onClick = { showCollectionDialog = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (strings.isArabic) "إضافة لمجلد" else "Add to Collection")
                    }

                    if (isAdmin) {
                        OutlinedButton(onClick = onFeatureArticle) {
                            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (strings.isArabic) "تمييز في المجلة" else "Feature in Journal")
                        }
                    }
                }

                if (pdfDownloadStatus != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(pdfDownloadStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // Abstract Card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (strings.isArabic) "الملخص العلمي" else "Abstract", style = MaterialTheme.typography.titleMedium)
                        Text(article.abstractText, style = MaterialTheme.typography.bodyMedium)
                        if (article.keywords.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                article.keywords.forEach { kw ->
                                    StatusPill(kw, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Gemini AI Scientific Research Assistant (10-point extraction + Respiratory/Critical Care concepts)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (strings.isArabic) "المساعد العلمي الذكي (Gemini)" else "AI Scientific Research Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (strings.isArabic)
                                "يستخلص فقط المعلومات الواردة في النص الأصلي ويفصل بين النتائج المنشورة والتفسير التعليمي دون إصدار توصيات علاجية."
                            else
                                "Extracts only facts reported in the paper metadata/abstract, identifies respiratory & ICU parameters, and separates published findings from AI educational interpretation.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = onAnalyzeWithAi,
                            enabled = paperAiState !is UiState.Loading,
                            modifier = Modifier.fillMaxWidth().testTag("detail_analyze_ai_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (strings.isArabic) "تحليل البحث بالذكاء الاصطناعي (10 نقاط)" else "Generate 10-Point Scientific Summary")
                        }

                        when (paperAiState) {
                            is UiState.Loading -> {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Text(strings.loading, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            is UiState.Error -> {
                                Text(paperAiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            is UiState.Success -> {
                                val ai = paperAiState.data
                                AiSection("1. Research Objective", ai.researchObjective)
                                AiSection("2. Background", ai.background)
                                AiSection("3. Study Design", ai.studyDesign)
                                AiSection("4. Methodology", ai.methodology)
                                AiSection("5. Study Population", ai.studyPopulation)
                                AiSection("6. Main Findings", ai.mainFindings)
                                AiSection("7. Statistical Analysis", ai.statisticalAnalysis)
                                AiSection("8. Clinical Implications (Educational)", ai.clinicalImplications)
                                AiSection("9. Study Limitations", ai.studyLimitations)
                                AiSection("10. Conclusions", ai.conclusions)
                                if (ai.respiratoryParametersExtracted.isNotEmpty()) {
                                    AiSection(
                                        "Respiratory & Critical Care Concepts",
                                        ai.respiratoryParametersExtracted.joinToString("\n• ", prefix = "• ")
                                    )
                                }
                                AiSection("Published Findings vs AI Interpretation", ai.publishedVsAiDistinction)
                            }
                            null -> {}
                        }
                    }
                }
            }

            // Reading Status & Personal Notes
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (strings.isArabic) "حالة القراءة والملاحظات الخاصة" else "Reading Management & Private Notes", style = MaterialTheme.typography.titleMedium)
                        val curStatus = libraryItem?.readingStatus ?: "UNREAD"
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("UNREAD" to "Unread", "READING" to "Reading", "READ" to "Read").forEach { (code, label) ->
                                FilterChip(
                                    selected = curStatus == code,
                                    onClick = { onUpdateReadingStatus(code) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = noteDraft,
                            onValueChange = { noteDraft = it },
                            label = { Text(if (strings.isArabic) "ملاحظاتك الشخصية (خاصة بك)" else "Private Study Notes") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("detail_notes_input")
                        )
                        Button(
                            onClick = { onSaveNotes(noteDraft) },
                            modifier = Modifier.align(Alignment.End).testTag("detail_save_notes_button")
                        ) {
                            Text(if (strings.isArabic) "حفظ الملاحظات" else "Save Notes")
                        }
                    }
                }
            }
        }
    }

    if (showCollectionDialog) {
        val currentCols = libraryItem?.collectionIds.orEmpty()
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text(if (strings.isArabic) "المجموعات الشخصية" else "Add to Personal Collection") },
            text = {
                Column {
                    collections.forEach { col ->
                        val checked = col.id in currentCols
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = checked, onCheckedChange = { onToggleCollection(col.id) })
                            Spacer(Modifier.width(8.dp))
                            Text(col.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCollectionDialog = false }) {
                    Text(if (strings.isArabic) "تم" else "Done")
                }
            }
        )
    }
}

@Composable
private fun AiSection(label: String, body: String) {
    if (body.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}
