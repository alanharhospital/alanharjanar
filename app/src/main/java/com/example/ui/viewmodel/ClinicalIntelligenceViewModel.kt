package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.remote.MultiDatabaseSearchService
import com.example.data.repository.ClinicalReportPdfExporter
import com.example.data.repository.ClinicalSafetyScreener
import com.example.data.repository.FirestoreJournalRepository
import com.example.data.repository.GeminiScientificAssistant
import com.example.domain.model.ClinicalCaseAnalysisResult
import com.example.domain.model.ClinicalCaseInput
import com.example.domain.model.ClinicalCaseSession
import com.example.domain.model.DiagnosisKnowledgeReport
import com.example.domain.model.EvidenceComparisonResult
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SafetyScreenResult
import com.example.domain.model.SavedClinicalTopic
import com.example.domain.model.SearchFilters
import com.example.domain.model.SortOption
import com.example.domain.model.TimeFilter
import com.example.domain.model.UiState
import com.example.domain.taxonomy.MedicalTaxonomy
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClinicalIntelligenceViewModel(
    private val firestoreRepo: FirestoreJournalRepository,
    private val searchService: MultiDatabaseSearchService,
    private val geminiAssistant: GeminiScientificAssistant,
    private val pdfExporter: ClinicalReportPdfExporter,
    private val currentUserId: String
) : ViewModel() {

    private val _caseInput = MutableStateFlow(ClinicalCaseInput())
    val caseInput: StateFlow<ClinicalCaseInput> = _caseInput.asStateFlow()

    private val _safetyScreen = MutableStateFlow(ClinicalSafetyScreener.screen(ClinicalCaseInput()))
    val safetyScreen: StateFlow<SafetyScreenResult> = _safetyScreen.asStateFlow()

    private val _caseAnalysisState = MutableStateFlow<UiState<ClinicalCaseAnalysisResult>?>(null)
    val caseAnalysisState: StateFlow<UiState<ClinicalCaseAnalysisResult>?> = _caseAnalysisState.asStateFlow()

    private val _caseSupportingStudies = MutableStateFlow<List<ResearchArticle>>(emptyList())
    val caseSupportingStudies: StateFlow<List<ResearchArticle>> = _caseSupportingStudies.asStateFlow()

    private val _diagnosisReportState = MutableStateFlow<UiState<DiagnosisKnowledgeReport>?>(null)
    val diagnosisReportState: StateFlow<UiState<DiagnosisKnowledgeReport>?> = _diagnosisReportState.asStateFlow()

    private val _diagnosisStudies = MutableStateFlow<List<ResearchArticle>>(emptyList())
    val diagnosisStudies: StateFlow<List<ResearchArticle>> = _diagnosisStudies.asStateFlow()

    private val _evidenceComparisonState = MutableStateFlow<UiState<EvidenceComparisonResult>?>(null)
    val evidenceComparisonState: StateFlow<UiState<EvidenceComparisonResult>?> = _evidenceComparisonState.asStateFlow()

    private val _latestClinicalStudiesState = MutableStateFlow<UiState<List<ResearchArticle>>?>(null)
    val latestClinicalStudiesState: StateFlow<UiState<List<ResearchArticle>>?> = _latestClinicalStudiesState.asStateFlow()

    private val _reportExportMessage = MutableStateFlow<String?>(null)
    val reportExportMessage: StateFlow<String?> = _reportExportMessage.asStateFlow()

    val savedCasesState: StateFlow<UiState<List<ClinicalCaseSession>>> =
        firestoreRepo.observeClinicalCases(currentUserId)
            .map<List<ClinicalCaseSession>, UiState<List<ClinicalCaseSession>>> { UiState.Success(it) }
            .catch { err -> emit(unwrapError(err)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val savedTopicsState: StateFlow<UiState<List<SavedClinicalTopic>>> =
        firestoreRepo.observeSavedClinicalTopics(currentUserId)
            .map<List<SavedClinicalTopic>, UiState<List<SavedClinicalTopic>>> { UiState.Success(it) }
            .catch { err -> emit(unwrapError(err)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val guidelinesState: StateFlow<List<GuidelineRecord>> =
        firestoreRepo.observeGuidelineRecords()
            .catch { emit(MedicalTaxonomy.referenceGuidelinesAndProtocols) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MedicalTaxonomy.referenceGuidelinesAndProtocols)

    fun updateCaseInput(transform: (ClinicalCaseInput) -> ClinicalCaseInput) {
        _caseInput.update { old ->
            val next = transform(old)
            _safetyScreen.value = ClinicalSafetyScreener.screen(next)
            next
        }
    }

    fun clearCaseInput() {
        _caseInput.value = ClinicalCaseInput()
        _safetyScreen.value = ClinicalSafetyScreener.screen(ClinicalCaseInput())
        _caseAnalysisState.value = null
        _caseSupportingStudies.value = emptyList()
    }

    fun analyzeClinicalCase(isArabic: Boolean, saveSession: Boolean = true) {
        val input = _caseInput.value
        val summary = input.buildDeidentifiedSummaryText()
        if (summary.isBlank()) {
            _caseAnalysisState.value = UiState.Error("Please enter at least a chief complaint, preliminary diagnosis, or clinical observation.")
            return
        }
        viewModelScope.launch {
            _caseAnalysisState.value = UiState.Loading
            val querySeed = listOf(input.preliminaryDiagnosis, input.chiefComplaint)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "acute respiratory failure critical care" }

            val studies = searchService.searchAcrossDatabases(
                SearchFilters(
                    query = querySeed,
                    timeFilter = TimeFilter.LAST_5_YEARS,
                    sortOption = SortOption.RELEVANCE,
                    enabledDatabases = setOf("PubMed", "Europe PMC")
                )
            ).getOrDefault(emptyList())
            _caseSupportingStudies.value = studies

            val result = geminiAssistant.analyzeClinicalCase(input, studies, isArabic)
            result.fold(
                onSuccess = { analysis ->
                    _caseAnalysisState.value = UiState.Success(analysis)
                    if (saveSession) {
                        val session = ClinicalCaseSession(
                            caseTitle = analysis.identifiedTopic.ifBlank { querySeed.take(120) },
                            preliminaryDiagnosis = input.preliminaryDiagnosis.ifBlank { "Preliminary Assessment" },
                            specialtyTrack = if (querySeed.contains("sepsis", true) || querySeed.contains("shock", true)) "CRITICAL_CARE" else "RESPIRATORY_CARE",
                            demographicsSummary = "${input.ageYears} ${input.demographicsNote}".trim(),
                            vitalsSummary = listOf(input.heartRateBpm, input.respiratoryRateBpm, input.bloodPressureMmHg, input.spo2Percent).filter { it.isNotBlank() }.joinToString(" / "),
                            respiratoryAbgSummary = listOf(input.abgPh, input.paco2MmHg, input.pao2MmHg, input.fio2Fraction, input.peepCmH2o).filter { it.isNotBlank() }.joinToString(" / "),
                            labsSummary = listOf(input.lactateMmolL, input.cbc, input.renalFunction, input.inflammatoryMarkers).filter { it.isNotBlank() }.joinToString("; "),
                            imagingSummary = listOf(input.chestXray, input.ctFindings, input.ultrasoundFindings).filter { it.isNotBlank() }.joinToString("; "),
                            safetyAlertLevel = _safetyScreen.value.overallLevel.name,
                            aiReportSummary = analysis.aiEducationalInterpretation,
                            missingChecklistSummary = analysis.missingInformation.joinToString("\n") { "[${it.priority.name}] ${it.fieldName}" }
                        )
                        firestoreRepo.saveClinicalCase(currentUserId, session)
                    }
                },
                onFailure = { err ->
                    _caseAnalysisState.value = UiState.Error(err.localizedMessage ?: "Case analysis failed")
                }
            )
        }
    }

    fun deleteSavedCase(caseId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteClinicalCase(currentUserId, caseId)
        }
    }

    fun exploreDiagnosis(query: String, isArabic: Boolean, bookmarkTopic: Boolean = false) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _diagnosisReportState.value = UiState.Loading
            val track = if (query.contains("sepsis", true) || query.contains("shock", true) || query.contains("kidney", true)) {
                "CRITICAL_CARE"
            } else {
                "RESPIRATORY_CARE"
            }
            firestoreRepo.recordClinicalTopic(currentUserId, query.trim(), track, bookmarkTopic)

            val studies = searchService.searchAcrossDatabases(
                SearchFilters(
                    query = query.trim(),
                    timeFilter = TimeFilter.LAST_5_YEARS,
                    sortOption = SortOption.NEWEST,
                    enabledDatabases = setOf("PubMed", "Europe PMC", "OpenAlex")
                )
            ).getOrDefault(emptyList())
            _diagnosisStudies.value = studies

            val matchedGuidelines = guidelinesState.value.filter {
                it.topic.contains(query.trim(), true) || it.title.contains(query.trim(), true)
            }
            val res = geminiAssistant.exploreDiagnosis(query.trim(), studies, matchedGuidelines, isArabic)
            _diagnosisReportState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Diagnosis exploration failed") }
            )
        }
    }

    fun toggleTopicBookmark(topicName: String, track: String, bookmarked: Boolean) {
        viewModelScope.launch {
            firestoreRepo.recordClinicalTopic(currentUserId, topicName, track, bookmarked)
        }
    }

    fun fetchLatestClinicalEvidence(topicQuery: String, timeFilter: TimeFilter) {
        viewModelScope.launch {
            _latestClinicalStudiesState.value = UiState.Loading
            val result = searchService.searchAcrossDatabases(
                SearchFilters(
                    query = topicQuery.ifBlank { "ARDS mechanical ventilation sepsis" },
                    timeFilter = timeFilter,
                    sortOption = SortOption.NEWEST,
                    enabledDatabases = setOf("PubMed", "Europe PMC", "OpenAlex", "Crossref", "DOAJ")
                )
            )
            _latestClinicalStudiesState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Evidence fetch failed") }
            )
        }
    }

    fun compareEvidenceForTopic(topic: String, isArabic: Boolean) {
        viewModelScope.launch {
            _evidenceComparisonState.value = UiState.Loading
            val matched = guidelinesState.value.filter {
                it.topic.contains(topic, true) || it.title.contains(topic, true)
            }
            val studies = searchService.searchAcrossDatabases(
                SearchFilters(
                    query = "$topic randomized controlled trial guideline",
                    timeFilter = TimeFilter.LAST_5_YEARS,
                    enabledDatabases = setOf("PubMed", "Europe PMC")
                )
            ).getOrDefault(emptyList())

            val result = geminiAssistant.compareClinicalEvidence(topic, matched, studies, isArabic)
            _evidenceComparisonState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Evidence comparison failed") }
            )
        }
    }

    fun exportAndShareClinicalReportPdf() {
        val analysis = (_caseAnalysisState.value as? UiState.Success)?.data
        if (analysis == null) {
            _reportExportMessage.value = "Run Clinical Case Analyzer first to generate a structured report."
            return
        }
        viewModelScope.launch {
            _reportExportMessage.value = "Generating de-identified PDF report..."
            pdfExporter.exportDeidentifiedReportPdf(
                caseInput = _caseInput.value,
                safety = _safetyScreen.value,
                analysis = analysis,
                studies = _caseSupportingStudies.value
            ).fold(
                onSuccess = { file ->
                    pdfExporter.sharePdf(file)
                    _reportExportMessage.value = "Exported: ${file.name}"
                },
                onFailure = { err ->
                    _reportExportMessage.value = "PDF export failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun clearReportExportMessage() {
        _reportExportMessage.value = null
    }

    private fun <T> unwrapError(error: Throwable): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return UiState.Error(firestoreError?.message ?: error.message ?: "Firestore error")
    }
}
