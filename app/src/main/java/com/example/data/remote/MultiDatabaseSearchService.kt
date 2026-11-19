package com.example.data.remote

import android.util.Log
import com.example.data.local.CachedArticleDao
import com.example.data.local.CachedArticleEntity
import com.example.data.local.SyncLogDao
import com.example.data.local.SyncLogEntity
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SearchFilters
import com.example.domain.model.SortOption
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder

private const val TAG = "MultiDatabaseSearch"

class MultiDatabaseSearchService(
    private val clients: AcademicNetworkClients,
    private val cachedArticleDao: CachedArticleDao,
    private val syncLogDao: SyncLogDao
) {

    suspend fun searchAcrossDatabases(
        filters: SearchFilters,
        page: Int = 0
    ): Result<List<ResearchArticle>> {
        return try {
            val combined = coroutineScope {
                val pubmedJob = if ("PubMed" in filters.enabledDatabases) {
                    async { safeFetch("PubMed") { clients.searchPubMed(filters, page) } }
                } else null
                val epmcJob = if ("Europe PMC" in filters.enabledDatabases) {
                    async { safeFetch("Europe PMC") { clients.searchEuropePmc(filters, page) } }
                } else null
                val oaJob = if ("OpenAlex" in filters.enabledDatabases) {
                    async { safeFetch("OpenAlex") { clients.searchOpenAlex(filters, page) } }
                } else null
                val s2Job = if ("Semantic Scholar" in filters.enabledDatabases) {
                    async { safeFetch("Semantic Scholar") { clients.searchSemanticScholar(filters, page) } }
                } else null
                val crJob = if ("Crossref" in filters.enabledDatabases) {
                    async { safeFetch("Crossref") { clients.searchCrossref(filters, page) } }
                } else null
                val doajJob = if ("DOAJ" in filters.enabledDatabases) {
                    async { safeFetch("DOAJ") { clients.searchDoaj(filters, page) } }
                } else null

                listOfNotNull(
                    pubmedJob?.await(),
                    epmcJob?.await(),
                    oaJob?.await(),
                    s2Job?.await(),
                    crJob?.await(),
                    doajJob?.await()
                ).flatten()
            }

            var deduped = deduplicateAndMerge(combined)

            if (filters.openAccessOnly) deduped = deduped.filter { it.openAccess }
            if (filters.freeFullTextOnly) deduped = deduped.filter { it.fullTextAvailable || it.pdfUrl.isNotBlank() }
            if (filters.freePdfOnly) deduped = deduped.filter { it.pdfUrl.isNotBlank() }

            deduped = when (filters.sortOption) {
                SortOption.MOST_CITED -> deduped.sortedByDescending { it.citationCount }
                SortOption.NEWEST, SortOption.PUBLICATION_DATE -> deduped.sortedByDescending { it.publicationDate }
                SortOption.RELEVANCE -> deduped
            }

            if (deduped.isNotEmpty()) {
                cachedArticleDao.upsertAll(deduped.take(40).map { CachedArticleEntity.fromArticle(it) })
            }
            Result.success(deduped)
        } catch (e: Exception) {
            Log.e(TAG, "Multi-database search failed", e)
            Result.failure(e)
        }
    }

    private suspend fun safeFetch(sourceName: String, block: suspend () -> List<ResearchArticle>): List<ResearchArticle> {
        return try {
            val items = block()
            syncLogDao.insertLog(
                SyncLogEntity(
                    sourceName = sourceName,
                    status = "OK",
                    message = "Retrieved ${items.size} records",
                    discoveredCount = items.size,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            items
        } catch (e: Exception) {
            Log.w(TAG, "$sourceName fetch error: ${e.message}")
            syncLogDao.insertLog(
                SyncLogEntity(
                    sourceName = sourceName,
                    status = "ERROR",
                    message = e.localizedMessage ?: "Network/API error",
                    discoveredCount = 0,
                    timestampMillis = System.currentTimeMillis()
                )
            )
            emptyList()
        }
    }

    suspend fun enrichArticleWithUnpaywall(article: ResearchArticle, email: String): ResearchArticle {
        if (article.doi.isBlank()) return article
        val unpaywall = try {
            clients.resolveUnpaywall(article.doi, email)
        } catch (_: Exception) {
            null
        } ?: return article

        return article.copy(
            openAccess = article.openAccess || unpaywall.isOpenAccess,
            pdfUrl = article.pdfUrl.ifBlank { unpaywall.legalPdfUrl },
            fullTextAvailable = article.fullTextAvailable || unpaywall.legalPdfUrl.isNotBlank() || unpaywall.landingUrl.isNotBlank(),
            publisherUrl = article.publisherUrl.ifBlank { unpaywall.landingUrl },
            contributingSources = (article.contributingSources + "Unpaywall").distinct()
        )
    }

    fun deduplicateAndMerge(articles: List<ResearchArticle>): List<ResearchArticle> {
        val byKey = LinkedHashMap<String, ResearchArticle>()
        for (item in articles) {
            val normDoi = item.doi.trim().lowercase()
            val normPmid = item.pmid.trim()
            val normTitle = item.title.lowercase().replace(Regex("[^a-z0-9]"), "").take(80)

            val matchKey = when {
                normDoi.isNotBlank() && byKey.containsKey("doi:$normDoi") -> "doi:$normDoi"
                normPmid.isNotBlank() && byKey.containsKey("pmid:$normPmid") -> "pmid:$normPmid"
                normTitle.isNotBlank() && byKey.containsKey("title:$normTitle") -> "title:$normTitle"
                normDoi.isNotBlank() -> "doi:$normDoi"
                normPmid.isNotBlank() -> "pmid:$normPmid"
                else -> "title:$normTitle"
            }

            val existing = byKey[matchKey]
            if (existing == null) {
                byKey[matchKey] = item
            } else {
                byKey[matchKey] = existing.copy(
                    abstractText = if (item.abstractText.length > existing.abstractText.length) item.abstractText else existing.abstractText,
                    doi = existing.doi.ifBlank { item.doi },
                    pmid = existing.pmid.ifBlank { item.pmid },
                    pmcid = existing.pmcid.ifBlank { item.pmcid },
                    pdfUrl = existing.pdfUrl.ifBlank { item.pdfUrl },
                    publisherUrl = existing.publisherUrl.ifBlank { item.publisherUrl },
                    openAccess = existing.openAccess || item.openAccess,
                    fullTextAvailable = existing.fullTextAvailable || item.fullTextAvailable,
                    citationCount = maxOf(existing.citationCount, item.citationCount),
                    keywords = (existing.keywords + item.keywords).distinct().take(12),
                    preprint = existing.preprint || item.preprint,
                    retracted = existing.retracted || item.retracted,
                    contributingSources = (existing.contributingSources + item.contributingSources).distinct()
                )
            }
        }
        return byKey.values.toList()
    }

    fun buildGoogleScholarSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.ifBlank { "respiratory critical care" }, "UTF-8")
        return "https://scholar.google.com/scholar?q=$encoded"
    }

    /**
     * Parses user-authorized Google Scholar Alert email text/snippets without scraping.
     */
    fun parseGoogleScholarAlertEmail(rawEmailText: String): List<ResearchArticle> {
        val lines = rawEmailText.lines().map { it.trim() }
        val parsed = mutableListOf<ResearchArticle>()
        var currentTitle = ""
        var currentUrl = ""
        var currentAuthors = ""
        val snippetBuilder = StringBuilder()

        fun flush() {
            if (currentTitle.length >= 8) {
                parsed.add(
                    ResearchArticle(
                        articleKey = "gs_${currentTitle.hashCode()}",
                        title = currentTitle,
                        authors = currentAuthors.ifBlank { "Google Scholar Alert Citation" },
                        journal = "Imported via Google Scholar Alert",
                        publicationDate = "Alert Import",
                        abstractText = snippetBuilder.toString().trim().ifBlank { "Imported from user-authorized Google Scholar Alert email." },
                        sourceDatabase = "Google Scholar Alert",
                        contributingSources = listOf("Google Scholar Alert"),
                        publisherUrl = currentUrl.ifBlank { buildGoogleScholarSearchUrl(currentTitle) }
                    )
                )
            }
            currentTitle = ""; currentUrl = ""; currentAuthors = ""; snippetBuilder.clear()
        }

        val urlRegex = Regex("https?://\\S+")
        for (line in lines) {
            if (line.isBlank()) {
                if (currentTitle.isNotBlank() && snippetBuilder.isNotEmpty()) flush()
                continue
            }
            val urlMatch = urlRegex.find(line)
            if (urlMatch != null) {
                if (currentUrl.isBlank()) currentUrl = urlMatch.value.trimEnd('>', ')', '.', ',')
                continue
            }
            if (currentTitle.isBlank() && line.length in 15..260 && !line.startsWith("This message", true) && !line.startsWith("Cancel alert", true)) {
                currentTitle = line
            } else if (currentAuthors.isBlank() && (line.contains(" - ") || line.contains(","))) {
                currentAuthors = line
            } else {
                snippetBuilder.append(line).append(" ")
            }
        }
        flush()
        return parsed
    }
}
