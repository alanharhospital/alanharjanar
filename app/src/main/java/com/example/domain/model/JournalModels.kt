package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ReadingStatus {
    UNREAD,
    READING,
    READ
}

enum class SpecialtyTrack {
    RESPIRATORY_CARE,
    CRITICAL_CARE
}

enum class TimeFilter(val labelEn: String, val labelAr: String, val days: Int?) {
    ALL("All Time", "كل الأوقات", null),
    LAST_24_HOURS("Last 24 Hours", "آخر 24 ساعة", 1),
    LAST_7_DAYS("Last 7 Days", "آخر 7 أيام", 7),
    LAST_30_DAYS("Last 30 Days", "آخر 30 يوماً", 30),
    LAST_3_MONTHS("Last 3 Months", "آخر 3 أشهر", 90),
    LAST_6_MONTHS("Last 6 Months", "آخر 6 أشهر", 180),
    LAST_12_MONTHS("Last 12 Months", "آخر 12 شهراً", 365),
    LAST_5_YEARS("Last 5 Years", "آخر 5 سنوات", 1825)
}

enum class StudyTypeFilter(val labelEn: String, val labelAr: String, val pubmedFilter: String?) {
    ALL("All Types", "جميع الأنواع", null),
    CLINICAL_TRIAL("Clinical Trials", "التجارب السريرية", "Clinical Trial[pt]"),
    RANDOMIZED_CONTROLLED_TRIAL("Randomized Controlled Trials", "التجارب المعشاة المضبوطة", "Randomized Controlled Trial[pt]"),
    SYSTEMATIC_REVIEW("Systematic Reviews", "المراجعات المنهجية", "Systematic Review[pt]"),
    META_ANALYSIS("Meta-Analyses", "التحليلات التلوية", "Meta-Analysis[pt]"),
    GUIDELINE("Clinical Guidelines", "الإرشادات السريرية", "Practice Guideline[pt]"),
    OBSERVATIONAL("Observational Studies", "الدراسات الرصدية", "Observational Study[pt]"),
    REVIEW("Review Articles", "مقالات المراجعة", "Review[pt]")
}

enum class SortOption(val labelEn: String, val labelAr: String) {
    NEWEST("Newest First", "الأحدث أولاً"),
    MOST_CITED("Most Cited", "الأكثر استشهاداً"),
    RELEVANCE("Relevance", "الأكثر صلة"),
    PUBLICATION_DATE("Publication Date", "تاريخ النشر")
}

data class SearchFilters(
    val query: String = "",
    val authorFilter: String = "",
    val journalFilter: String = "",
    val yearFilter: String = "",
    val specialtyTrack: SpecialtyTrack? = null,
    val timeFilter: TimeFilter = TimeFilter.ALL,
    val studyTypeFilter: StudyTypeFilter = StudyTypeFilter.ALL,
    val openAccessOnly: Boolean = false,
    val freeFullTextOnly: Boolean = false,
    val freePdfOnly: Boolean = false,
    val sortOption: SortOption = SortOption.NEWEST,
    val enabledDatabases: Set<String> = setOf(
        "PubMed", "Europe PMC", "OpenAlex", "Semantic Scholar", "Crossref", "DOAJ"
    )
)

data class ResearchArticle(
    val articleKey: String,
    val title: String,
    val authors: String,
    val journal: String,
    val publicationDate: String,
    val abstractText: String,
    val doi: String = "",
    val pmid: String = "",
    val pmcid: String = "",
    val publicationType: String = "Journal Article",
    val keywords: List<String> = emptyList(),
    val sourceDatabase: String = "PubMed",
    val contributingSources: List<String> = listOf("PubMed"),
    val citationCount: Int = 0,
    val publisherUrl: String = "",
    val openAccess: Boolean = false,
    val fullTextAvailable: Boolean = false,
    val pdfUrl: String = "",
    val preprint: Boolean = false,
    val retracted: Boolean = false,
    val retrievedAt: String = "",
    val specialtyTrack: String = ""
)

@Keep
@IgnoreExtraProperties
data class HospitalConfig(
    @DocumentId val id: String = "branding",
    val hospitalNameEn: String = "Al-Anhar Specialized Hospital",
    val hospitalNameAr: String = "مستشفى الأنهار التخصصي",
    val tagline: String = "ADVANCING RESPIRATORY & CRITICAL CARE THROUGH SCIENTIFIC EVIDENCE.",
    val logoDataUri: String = "",
    val updateFrequencyHours: Long = 12L,
    val unpaywallEmail: String = "research@alanharhospital.org",
    val enabledSources: List<String> = listOf("PubMed", "Europe PMC", "OpenAlex", "Semantic Scholar", "Crossref", "DOAJ", "Unpaywall"),
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScientificCategory(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val parentTrack: String = "RESPIRATORY_CARE",
    val description: String = "",
    val searchQueryTerms: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EditorialPost(
    @DocumentId val id: String = "",
    val title: String = "",
    val titleAr: String = "",
    val summary: String = "",
    val body: String = "",
    val specialty: String = "",
    val contentType: String = "EDITORIAL_ANNOUNCEMENT",
    val authorName: String = "",
    val hospitalProtocol: Boolean = false,
    val externalUrl: String = "",
    val doi: String = "",
    val publicationDate: String = "",
    val verifiedDate: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonitoredArticle(
    @DocumentId val id: String = "",
    val title: String = "",
    val authors: String = "",
    val journal: String = "",
    val publicationDate: String = "",
    val abstractText: String = "",
    val doi: String = "",
    val pmid: String = "",
    val publicationType: String = "",
    val keywords: List<String> = emptyList(),
    val sourceDatabase: String = "",
    val citationCount: Long = 0L,
    val publisherUrl: String = "",
    val openAccess: Boolean = false,
    val fullTextAvailable: Boolean = false,
    val pdfUrl: String = "",
    val preprint: Boolean = false,
    val retracted: Boolean = false,
    val featured: Boolean = false,
    val specialtyTrack: String = "RESPIRATORY_CARE",
    val categoryName: String = "",
    val retrievedAt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toResearchArticle(): ResearchArticle = ResearchArticle(
        articleKey = id.ifBlank { doi.ifBlank { pmid.ifBlank { title.hashCode().toString() } } },
        title = title,
        authors = authors,
        journal = journal,
        publicationDate = publicationDate,
        abstractText = abstractText,
        doi = doi,
        pmid = pmid,
        publicationType = publicationType,
        keywords = keywords,
        sourceDatabase = sourceDatabase,
        contributingSources = listOf(sourceDatabase),
        citationCount = citationCount.toInt(),
        publisherUrl = publisherUrl,
        openAccess = openAccess,
        fullTextAvailable = fullTextAvailable,
        pdfUrl = pdfUrl,
        preprint = preprint,
        retracted = retracted,
        retrievedAt = retrievedAt,
        specialtyTrack = specialtyTrack
    )
}

@Keep
@IgnoreExtraProperties
data class GuidelineRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val organization: String = "",
    val specialtyTrack: String = "RESPIRATORY_CARE",
    val topic: String = "",
    val publicationYear: Long = 2024L,
    val latestUpdateDate: String = "",
    val lastVerifiedDate: String = "",
    val recommendations: String = "",
    val evidenceCertainty: String = "",
    val previousVersionSummary: String = "",
    val changesSummary: String = "",
    val sourceUrl: String = "",
    val fullTextAvailable: Boolean = true,
    val protocolScope: String = "INTERNATIONAL_GUIDELINE",
    val doi: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val specialty: String = "Respiratory Care",
    val language: String = "en",
    val themeMode: String = "system",
    val subscribedTopics: List<String> = emptyList(),
    val pushEnabled: Boolean = true,
    val emailDigestEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LibraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val articleKey: String = "",
    val title: String = "",
    val authors: String = "",
    val journal: String = "",
    val publicationDate: String = "",
    val abstractText: String = "",
    val doi: String = "",
    val pmid: String = "",
    val publicationType: String = "",
    val sourceDatabase: String = "",
    val publisherUrl: String = "",
    val pdfUrl: String = "",
    val openAccess: Boolean = false,
    val fullTextAvailable: Boolean = false,
    val preprint: Boolean = false,
    val retracted: Boolean = false,
    val saved: Boolean = false,
    val bookmarked: Boolean = false,
    val favorite: Boolean = false,
    val readingStatus: String = "UNREAD",
    val personalNotes: String = "",
    val collectionIds: List<String> = emptyList(),
    val localPdfPath: String = "",
    val lastOpenedMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toResearchArticle(): ResearchArticle = ResearchArticle(
        articleKey = articleKey,
        title = title,
        authors = authors,
        journal = journal,
        publicationDate = publicationDate,
        abstractText = abstractText,
        doi = doi,
        pmid = pmid,
        publicationType = publicationType,
        sourceDatabase = sourceDatabase,
        contributingSources = listOf(sourceDatabase).filter { it.isNotBlank() },
        publisherUrl = publisherUrl,
        openAccess = openAccess,
        fullTextAvailable = fullTextAvailable,
        pdfUrl = pdfUrl,
        preprint = preprint,
        retracted = retracted
    )
}

@Keep
@IgnoreExtraProperties
data class UserCollection(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val colorHex: String = "#0B3C5D",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val notificationType: String = "NEW_PUBLICATION",
    val articleKey: String = "",
    val doi: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "admin",
    @ServerTimestamp val createdAt: Timestamp? = null
)
