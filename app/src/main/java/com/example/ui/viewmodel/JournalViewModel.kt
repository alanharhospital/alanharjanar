package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CachedArticleDao
import com.example.data.local.CachedArticleEntity
import com.example.data.local.PreferencesManager
import com.example.data.remote.MultiDatabaseSearchService
import com.example.data.remote.PdfDownloadService
import com.example.data.repository.FirestoreJournalRepository
import com.example.data.repository.GeminiScientificAssistant
import com.example.domain.model.EditorialPost
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.MonitoredArticle
import com.example.domain.model.PaperAiAnalysis
import com.example.domain.model.ResearchArticle
import com.example.domain.model.ScientificCategory
import com.example.domain.model.SearchFilters
import com.example.domain.model.SortOption
import com.example.domain.model.SpecialtyTrack
import com.example.domain.model.StudyTypeFilter
import com.example.domain.model.TimeFilter
import com.example.domain.model.UiState
import com.example.domain.taxonomy.MedicalTaxonomy
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "JournalVM"

data class HomeJournalFeed(
    val latestPublications: List<ResearchArticle> = emptyList(),
    val mostRelevant: List<ResearchArticle> = emptyList(),
    val newRespiratory: List<ResearchArticle> = emptyList(),
    val newCriticalCare: List<ResearchArticle> = emptyList(),
    val openAccessList: List<ResearchArticle> = emptyList(),
    val systematicReviews: List<ResearchArticle> = emptyList(),
    val clinicalGuidelinesPapers: List<ResearchArticle> = emptyList()
)

class JournalViewModel(
    private val firestoreRepo: FirestoreJournalRepository,
    private val searchService: MultiDatabaseSearchService,
    private val geminiAssistant: GeminiScientificAssistant,
    private val pdfService: PdfDownloadService,
    private val cachedArticleDao: CachedArticleDao,
    private val preferencesManager: PreferencesManager,
    private val currentUserId: String
) : ViewModel() {

    val categoriesState: StateFlow<UiState<List<ScientificCategory>>> = firestoreRepo.observeCategories()
        .map<List<ScientificCategory>, UiState<List<ScientificCategory>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, MedicalTaxonomy.allDefaultCategories)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val editorialPostsState: StateFlow<UiState<List<EditorialPost>>> = firestoreRepo.observeEditorialPosts()
        .map<List<EditorialPost>, UiState<List<EditorialPost>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, emptyList())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val monitoredArticlesState: StateFlow<UiState<List<MonitoredArticle>>> = firestoreRepo.observeMonitoredArticles()
        .map<List<MonitoredArticle>, UiState<List<MonitoredArticle>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, emptyList())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val guidelineRecordsState: StateFlow<UiState<List<GuidelineRecord>>> = firestoreRepo.observeGuidelineRecords()
        .map<List<GuidelineRecord>, UiState<List<GuidelineRecord>>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFirestoreError(error, MedicalTaxonomy.referenceGuidelinesAndProtocols)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val offlineCachedArticles: StateFlow<List<ResearchArticle>> = cachedArticleDao.observeCachedArticles()
        .map { list -> list.map { it.toResearchArticle() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _homeFeedState = MutableStateFlow<UiState<HomeJournalFeed>>(UiState.Loading)
    val homeFeedState: StateFlow<UiState<HomeJournalFeed>> = _homeFeedState.asStateFlow()

    private val _searchFilters = MutableStateFlow(SearchFilters(query = "mechanical ventilation ARDS"))
    val searchFilters: StateFlow<SearchFilters> = _searchFilters.asStateFlow()

    private val _searchResultsState = MutableStateFlow<UiState<List<ResearchArticle>>>(UiState.Loading)
    val searchResultsState: StateFlow<UiState<List<ResearchArticle>>> = _searchResultsState.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _selectedArticle = MutableStateFlow<ResearchArticle?>(null)
    val selectedArticle: StateFlow<ResearchArticle?> = _selectedArticle.asStateFlow()

    private val _paperAiState = MutableStateFlow<UiState<PaperAiAnalysis>?>(null)
    val paperAiState: StateFlow<UiState<PaperAiAnalysis>?> = _paperAiState.asStateFlow()

    private val _pdfDownloadStatus = MutableStateFlow<String?>(null)
    val pdfDownloadStatus: StateFlow<String?> = _pdfDownloadStatus.asStateFlow()

    private val _importedScholarMessage = MutableStateFlow<String?>(null)
    val importedScholarMessage: StateFlow<String?> = _importedScholarMessage.asStateFlow()

    init {
        refreshHomeFeeds()
        executeSearch(resetPage = true)
    }

    fun refreshHomeFeeds() {
        viewModelScope.launch {
            _homeFeedState.value = UiState.Loading
            try {
                val feed = coroutineScope {
                    val latestJob = async {
                        searchService.searchAcrossDatabases(
                            SearchFilters(
                                query = "mechanical ventilation OR ARDS OR sepsis OR intensive care",
                                timeFilter = TimeFilter.LAST_30_DAYS,
                                sortOption = SortOption.NEWEST,
                                enabledDatabases = setOf("PubMed", "Europe PMC", "OpenAlex")
                            )
                        ).getOrDefault(emptyList())
                    }
                    val respJob = async {
                        searchService.searchAcrossDatabases(
                            SearchFilters(
                                query = "mechanical ventilation HFNC NIV ARDS respiratory care",
                                specialtyTrack = SpecialtyTrack.RESPIRATORY_CARE,
                                enabledDatabases = setOf("PubMed", "Europe PMC")
                            )
                        ).getOrDefault(emptyList())
                    }
                    val critJob = async {
                        searchService.searchAcrossDatabases(
                            SearchFilters(
                                query = "sepsis septic shock ICU hemodynamic monitoring acute kidney injury",
                                specialtyTrack = SpecialtyTrack.CRITICAL_CARE,
                                enabledDatabases = setOf("PubMed", "Europe PMC")
                            )
                        ).getOrDefault(emptyList())
                    }
                    val reviewJob = async {
                        searchService.searchAcrossDatabases(
                            SearchFilters(
                                query = "mechanical ventilation sepsis ARDS",
                                studyTypeFilter = StudyTypeFilter.META_ANALYSIS,
                                enabledDatabases = setOf("PubMed")
                            )
                        ).getOrDefault(emptyList())
                    }
                    val guideJob = async {
                        searchService.searchAcrossDatabases(
                            SearchFilters(
                                query = "respiratory critical care sepsis ARDS",
                                studyTypeFilter = StudyTypeFilter.GUIDELINE,
                                enabledDatabases = setOf("PubMed")
                            )
                        ).getOrDefault(emptyList())
                    }

                    val latest = latestJob.await()
                    val resp = respJob.await()
                    val crit = critJob.await()
                    val reviews = reviewJob.await()
                    val guides = guideJob.await()

                    val allCombined = searchService.deduplicateAndMerge(latest + resp + crit)
                    HomeJournalFeed(
                        latestPublications = latest.take(12),
                        mostRelevant = allCombined.sortedByDescending { it.citationCount }.take(10),
                        newRespiratory = resp.take(10),
                        newCriticalCare = crit.take(10),
                        openAccessList = allCombined.filter { it.openAccess }.take(10),
                        systematicReviews = reviews.take(10),
                        clinicalGuidelinesPapers = guides.take(10)
                    )
                }
                _homeFeedState.value = UiState.Success(feed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load journal home feeds", e)
                val cached = offlineCachedArticles.value
                if (cached.isNotEmpty()) {
                    _homeFeedState.value = UiState.Success(
                        HomeJournalFeed(
                            latestPublications = cached,
                            openAccessList = cached.filter { it.openAccess }
                        )
                    )
                } else {
                    _homeFeedState.value = UiState.Error(e.localizedMessage ?: "Unable to connect to academic databases.")
                }
            }
        }
    }

    fun updateFilters(transform: (SearchFilters) -> SearchFilters) {
        _searchFilters.update(transform)
    }

    fun searchByCategory(category: ScientificCategory) {
        val track = if (category.parentTrack == "CRITICAL_CARE") SpecialtyTrack.CRITICAL_CARE else SpecialtyTrack.RESPIRATORY_CARE
        _searchFilters.value = SearchFilters(
            query = category.searchQueryTerms.ifBlank { category.nameEn },
            specialtyTrack = track,
            sortOption = SortOption.NEWEST
        )
        executeSearch(resetPage = true)
    }

    fun executeSearch(resetPage: Boolean = true) {
        viewModelScope.launch {
            if (resetPage) _currentPage.value = 0
            _searchResultsState.value = UiState.Loading
            val result = searchService.searchAcrossDatabases(_searchFilters.value, page = _currentPage.value)
            _searchResultsState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Search failed") }
            )
        }
    }

    fun nextPage() {
        _currentPage.update { it + 1 }
        executeSearch(resetPage = false)
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.update { it - 1 }
            executeSearch(resetPage = false)
        }
    }

    fun openArticleDetail(article: ResearchArticle) {
        _selectedArticle.value = article
        _paperAiState.value = null
        _pdfDownloadStatus.value = null
        viewModelScope.launch {
            // Track reading history in Firestore & cache locally
            firestoreRepo.saveOrUpdateLibraryItem(
                userId = currentUserId,
                article = article,
                markOpened = true
            )
            cachedArticleDao.upsertAll(listOf(CachedArticleEntity.fromArticle(article)))

            // Enrich with Unpaywall if DOI exists and legal PDF not yet resolved
            if (article.doi.isNotBlank() && article.pdfUrl.isBlank()) {
                val email = preferencesManager.preferencesFlow.first().unpaywallEmail
                val enriched = searchService.enrichArticleWithUnpaywall(article, email)
                if (_selectedArticle.value?.articleKey == article.articleKey) {
                    _selectedArticle.value = enriched
                }
            }
        }
    }

    fun analyzeSelectedArticleWithAi(isArabic: Boolean) {
        val article = _selectedArticle.value ?: return
        viewModelScope.launch {
            _paperAiState.value = UiState.Loading
            val result = geminiAssistant.analyzeResearchPaper(article, isArabic)
            _paperAiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "AI paper analysis failed") }
            )
        }
    }

    fun downloadLegalPdfForSelectedArticle() {
        val article = _selectedArticle.value ?: return
        if (article.pdfUrl.isBlank()) {
            _pdfDownloadStatus.value = "No verified legal PDF available for this publication."
            return
        }
        viewModelScope.launch {
            _pdfDownloadStatus.value = "Downloading legal PDF..."
            val result = pdfService.downloadLegalPdf(article.articleKey, article.pdfUrl)
            result.fold(
                onSuccess = { localPath ->
                    cachedArticleDao.upsertAll(listOf(CachedArticleEntity.fromArticle(article, localPath)))
                    firestoreRepo.saveOrUpdateLibraryItem(
                        userId = currentUserId,
                        article = article,
                        saved = true,
                        localPdfPath = localPath
                    )
                    _pdfDownloadStatus.value = "Downloaded to device PDF library."
                },
                onFailure = { err ->
                    _pdfDownloadStatus.value = "Download failed: ${err.localizedMessage}"
                }
            )
        }
    }

    fun openLocalPdf(path: String) {
        pdfService.openLocalPdf(path).onFailure { err ->
            _pdfDownloadStatus.value = err.localizedMessage
        }
    }

    fun getGoogleScholarUrl(query: String): String = searchService.buildGoogleScholarSearchUrl(query)

    fun importGoogleScholarEmailAlert(rawEmailText: String) {
        viewModelScope.launch {
            val parsed = searchService.parseGoogleScholarAlertEmail(rawEmailText)
            if (parsed.isEmpty()) {
                _importedScholarMessage.value = "No citations detected in the pasted alert email."
            } else {
                _searchResultsState.value = UiState.Success(parsed)
                _importedScholarMessage.value = "Imported ${parsed.size} citations from Google Scholar Alert."
            }
        }
    }

    fun clearScholarImportMessage() {
        _importedScholarMessage.value = null
    }

    private fun <T> unwrapFirestoreError(error: Throwable, fallback: T): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Firestore stream warning: ${firestoreError?.message ?: error.message}")
        return UiState.Success(fallback)
    }
}
