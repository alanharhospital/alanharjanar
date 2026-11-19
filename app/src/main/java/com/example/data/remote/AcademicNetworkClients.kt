package com.example.data.remote

import android.util.Log
import android.util.Xml
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SearchFilters
import com.example.domain.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "AcademicClients"

class AcademicNetworkClients {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "AlAnharScientificJournal/1.0 (Al-Anhar Specialized Hospital; mailto:research@alanharhospital.org)")
                .build()
            chain.proceed(req)
        }
        .build()

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun executeGet(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} for $url")
                return null
            }
            return response.body?.string()
        }
    }

    private fun buildEffectiveQuery(filters: SearchFilters): String {
        val parts = mutableListOf<String>()
        if (filters.query.isNotBlank()) parts.add(filters.query.trim())
        if (filters.authorFilter.isNotBlank()) parts.add(filters.authorFilter.trim())
        if (filters.journalFilter.isNotBlank()) parts.add(filters.journalFilter.trim())
        if (filters.yearFilter.isNotBlank()) parts.add(filters.yearFilter.trim())
        if (parts.isEmpty()) {
            return when (filters.specialtyTrack) {
                com.example.domain.model.SpecialtyTrack.CRITICAL_CARE -> "critical care intensive care sepsis"
                else -> "respiratory care mechanical ventilation ARDS"
            }
        }
        return parts.joinToString(" ")
    }

    suspend fun searchPubMed(filters: SearchFilters, page: Int = 0, pageSize: Int = 15): List<ResearchArticle> =
        withContext(Dispatchers.IO) {
            val baseQuery = buildEffectiveQuery(filters)
            val queryParts = mutableListOf("($baseQuery)")
            if (filters.authorFilter.isNotBlank()) queryParts.add("${filters.authorFilter.trim()}[Author]")
            if (filters.journalFilter.isNotBlank()) queryParts.add("${filters.journalFilter.trim()}[Journal]")
            if (filters.yearFilter.isNotBlank()) queryParts.add("${filters.yearFilter.trim()}[dp]")
            filters.studyTypeFilter.pubmedFilter?.let { queryParts.add(it) }
            if (filters.freeFullTextOnly) queryParts.add("free full text[sb]")

            val fullTerm = queryParts.joinToString(" AND ")
            val sortParam = when (filters.sortOption) {
                SortOption.RELEVANCE -> "relevance"
                else -> "pub+date"
            }
            val relDateParam = filters.timeFilter.days?.let { "&reldate=$it&datetype=pdat" } ?: ""
            val retStart = page * pageSize

            val esearchUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi" +
                "?db=pubmed&term=${encode(fullTerm)}&retmode=json&retmax=$pageSize&retstart=$retStart&sort=$sortParam$relDateParam"

            val searchBody = executeGet(esearchUrl) ?: return@withContext emptyList()
            val idList = JSONObject(searchBody)
                .optJSONObject("esearchresult")
                ?.optJSONArray("idlist") ?: return@withContext emptyList()

            val pmids = (0 until idList.length()).mapNotNull { idList.optString(it).takeIf { id -> id.isNotBlank() } }
            if (pmids.isEmpty()) return@withContext emptyList()

            val efetchUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi" +
                "?db=pubmed&id=${pmids.joinToString(",")}&retmode=xml"
            val xmlBody = executeGet(efetchUrl) ?: return@withContext emptyList()
            parsePubMedXml(xmlBody)
        }

    private fun parsePubMedXml(xml: String): List<ResearchArticle> {
        val results = mutableListOf<ResearchArticle>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var inArticle = false
        var inAuthor = false
        var inPubDate = false
        var inJournal = false
        var currentTag = ""

        var pmid = ""
        var doi = ""
        var pmcid = ""
        var title = ""
        var journal = ""
        var pubYear = ""
        var pubMonth = ""
        var pubDay = ""
        val abstractParts = mutableListOf<String>()
        val authors = mutableListOf<String>()
        var lastName = ""
        var initials = ""
        val keywords = mutableListOf<String>()
        val pubTypes = mutableListOf<String>()
        var articleIdType = ""
        var abstractLabel = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    when (currentTag) {
                        "PubmedArticle" -> {
                            inArticle = true
                            pmid = ""; doi = ""; pmcid = ""; title = ""; journal = ""
                            pubYear = ""; pubMonth = ""; pubDay = ""
                            abstractParts.clear(); authors.clear(); keywords.clear(); pubTypes.clear()
                        }
                        "Journal" -> if (inArticle) inJournal = true
                        "PubDate" -> if (inArticle && inJournal) inPubDate = true
                        "Author" -> if (inArticle) { inAuthor = true; lastName = ""; initials = "" }
                        "ArticleId" -> articleIdType = parser.getAttributeValue(null, "IdType") ?: ""
                        "AbstractText" -> abstractLabel = parser.getAttributeValue(null, "Label") ?: ""
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (inArticle && text.isNotEmpty()) {
                        when (currentTag) {
                            "PMID" -> if (pmid.isEmpty()) pmid = text
                            "ArticleTitle" -> title += text
                            "Title" -> if (inJournal && journal.isEmpty()) journal = text
                            "ISOAbbreviation" -> if (inJournal && journal.isEmpty()) journal = text
                            "Year" -> if (inPubDate && pubYear.isEmpty()) pubYear = text
                            "Month" -> if (inPubDate && pubMonth.isEmpty()) pubMonth = text
                            "Day" -> if (inPubDate && pubDay.isEmpty()) pubDay = text
                            "AbstractText" -> {
                                val prefix = if (abstractLabel.isNotBlank()) "$abstractLabel: " else ""
                                abstractParts.add("$prefix$text")
                            }
                            "LastName" -> if (inAuthor) lastName = text
                            "Initials" -> if (inAuthor) initials = text
                            "Keyword" -> keywords.add(text)
                            "PublicationType" -> pubTypes.add(text)
                            "ArticleId" -> when (articleIdType) {
                                "doi" -> if (doi.isEmpty()) doi = text
                                "pmc" -> if (pmcid.isEmpty()) pmcid = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "Author" -> {
                            if (inAuthor && lastName.isNotBlank()) {
                                authors.add(if (initials.isNotBlank()) "$lastName $initials" else lastName)
                            }
                            inAuthor = false
                        }
                        "PubDate" -> inPubDate = false
                        "Journal" -> inJournal = false
                        "AbstractText" -> abstractLabel = ""
                        "ArticleId" -> articleIdType = ""
                        "PubmedArticle" -> {
                            inArticle = false
                            if (title.isNotBlank()) {
                                val dateFormatted = listOfNotNull(
                                    pubYear.takeIf { it.isNotBlank() },
                                    pubMonth.takeIf { it.isNotBlank() },
                                    pubDay.takeIf { it.isNotBlank() }
                                ).joinToString("-")
                                val isRetracted = pubTypes.any { it.contains("Retracted", ignoreCase = true) }
                                val isPreprint = pubTypes.any { it.contains("Preprint", ignoreCase = true) }
                                val hasPmc = pmcid.isNotBlank()
                                val pdfLink = if (hasPmc) "https://europepmc.org/backend/ptpmcrender.fcgi?accid=$pmcid&blobtype=pdf" else ""

                                results.add(
                                    ResearchArticle(
                                        articleKey = if (doi.isNotBlank()) "doi_${doi.lowercase()}" else "pmid_$pmid",
                                        title = title.trim().removeSuffix("."),
                                        authors = authors.take(8).joinToString(", ").ifBlank { "Authors indexed in PubMed" },
                                        journal = journal.ifBlank { "PubMed Indexed Journal" },
                                        publicationDate = dateFormatted.ifBlank { "Recent" },
                                        abstractText = abstractParts.joinToString("\n\n").ifBlank { "Abstract not provided by publisher." },
                                        doi = doi,
                                        pmid = pmid,
                                        pmcid = pmcid,
                                        publicationType = pubTypes.firstOrNull() ?: "Journal Article",
                                        keywords = keywords.distinct().take(10),
                                        sourceDatabase = "PubMed",
                                        contributingSources = listOf("PubMed"),
                                        publisherUrl = if (pmid.isNotBlank()) "https://pubmed.ncbi.nlm.nih.gov/$pmid/" else "https://doi.org/$doi",
                                        openAccess = hasPmc,
                                        fullTextAvailable = hasPmc,
                                        pdfUrl = pdfLink,
                                        preprint = isPreprint,
                                        retracted = isRetracted,
                                        retrievedAt = todayIso()
                                    )
                                )
                            }
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return results
    }

    suspend fun searchEuropePmc(filters: SearchFilters, page: Int = 0, pageSize: Int = 15): List<ResearchArticle> =
        withContext(Dispatchers.IO) {
            val baseQuery = buildEffectiveQuery(filters)
            val queryParts = mutableListOf(baseQuery)
            if (filters.openAccessOnly) queryParts.add("OPEN_ACCESS:y")
            if (filters.freePdfOnly) queryParts.add("HAS_PDF:y")
            if (filters.freeFullTextOnly) queryParts.add("HAS_FT:y")
            filters.timeFilter.days?.let { days ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
                val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                queryParts.add("FIRST_PDATE:[$fromDate TO ${todayIso()}]")
            }
            val sortParam = when (filters.sortOption) {
                SortOption.MOST_CITED -> "&sort=CITED desc"
                SortOption.RELEVANCE -> ""
                else -> "&sort=P_PDATE_D desc"
            }
            val url = "https://www.ebi.ac.uk/europepmc/webservices/rest/search" +
                "?query=${encode(queryParts.joinToString(" AND "))}&format=json&resultType=core&pageSize=$pageSize&page=${page + 1}$sortParam"

            val body = executeGet(url) ?: return@withContext emptyList()
            val results = JSONObject(body)
                .optJSONObject("resultList")
                ?.optJSONArray("result") ?: return@withContext emptyList()

            val list = mutableListOf<ResearchArticle>()
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val title = obj.optString("title").replace(Regex("<[^>]+>"), "").trim()
                if (title.isBlank()) continue
                val doi = obj.optString("doi")
                val pmid = obj.optString("pmid")
                val pmcid = obj.optString("pmcid")
                val isOa = obj.optString("isOpenAccess") == "Y"
                val hasPdf = obj.optString("hasPDF") == "Y"
                val pdfUrl = if (hasPdf && pmcid.isNotBlank()) {
                    "https://europepmc.org/backend/ptpmcrender.fcgi?accid=$pmcid&blobtype=pdf"
                } else ""
                val source = obj.optString("source")
                val isPreprint = source.equals("PPR", ignoreCase = true)
                val pubTypes = obj.optJSONObject("pubTypeList")?.optJSONArray("pubType")
                val pubType = if (pubTypes != null && pubTypes.length() > 0) pubTypes.optString(0) else "Journal Article"
                val isRetracted = pubType.contains("Retracted", ignoreCase = true)

                list.add(
                    ResearchArticle(
                        articleKey = when {
                            doi.isNotBlank() -> "doi_${doi.lowercase()}"
                            pmid.isNotBlank() -> "pmid_$pmid"
                            else -> "epmc_${obj.optString("id")}"
                        },
                        title = title,
                        authors = obj.optString("authorString").ifBlank { "Authors listed in Europe PMC" },
                        journal = obj.optString("journalTitle").ifBlank { if (isPreprint) "Preprint Server" else "Europe PMC" },
                        publicationDate = obj.optString("firstPublicationDate").ifBlank { obj.optString("pubYear") },
                        abstractText = obj.optString("abstractText").replace(Regex("<[^>]+>"), "").ifBlank { "Abstract available via Europe PMC link." },
                        doi = doi,
                        pmid = pmid,
                        pmcid = pmcid,
                        publicationType = if (isPreprint) "Preprint" else pubType,
                        sourceDatabase = "Europe PMC",
                        contributingSources = listOf("Europe PMC"),
                        citationCount = obj.optInt("citedByCount", 0),
                        publisherUrl = when {
                            pmcid.isNotBlank() -> "https://europepmc.org/article/PMC/$pmcid"
                            pmid.isNotBlank() -> "https://europepmc.org/article/MED/$pmid"
                            doi.isNotBlank() -> "https://doi.org/$doi"
                            else -> "https://europepmc.org/"
                        },
                        openAccess = isOa,
                        fullTextAvailable = isOa || obj.optString("hasTextMinedTerms") == "Y" && pmcid.isNotBlank(),
                        pdfUrl = pdfUrl,
                        preprint = isPreprint,
                        retracted = isRetracted,
                        retrievedAt = todayIso()
                    )
                )
            }
            list
        }

    suspend fun searchOpenAlex(filters: SearchFilters, page: Int = 0, pageSize: Int = 15): List<ResearchArticle> =
        withContext(Dispatchers.IO) {
            val query = buildEffectiveQuery(filters)
            val filterParts = mutableListOf<String>()
            if (filters.openAccessOnly) filterParts.add("is_oa:true")
            filters.timeFilter.days?.let { days ->
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
                val fromDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                filterParts.add("from_publication_date:$fromDate")
            }
            val filterQuery = if (filterParts.isNotEmpty()) "&filter=${encode(filterParts.joinToString(","))}" else ""
            val sortParam = when (filters.sortOption) {
                SortOption.MOST_CITED -> "&sort=cited_by_count:desc"
                SortOption.RELEVANCE -> ""
                else -> "&sort=publication_date:desc"
            }
            val url = "https://api.openalex.org/works?search=${encode(query)}&per-page=$pageSize&page=${page + 1}$filterQuery$sortParam"

            val body = executeGet(url) ?: return@withContext emptyList()
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<ResearchArticle>()
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                if (title.isBlank()) continue
                val rawDoi = obj.optString("doi").removePrefix("https://doi.org/").removePrefix("http://doi.org/")
                val oaObj = obj.optJSONObject("open_access")
                val isOa = oaObj?.optBoolean("is_oa", false) == true
                val primaryLoc = obj.optJSONObject("primary_location")
                val pdfUrl = primaryLoc?.optString("pdf_url").orEmpty().takeIf { it != "null" }.orEmpty()
                val sourceName = primaryLoc?.optJSONObject("source")?.optString("display_name").orEmpty()
                val landingUrl = primaryLoc?.optString("landing_page_url").orEmpty().ifBlank { oaObj?.optString("oa_url").orEmpty() }

                val authorships = obj.optJSONArray("authorships")
                val authorNames = mutableListOf<String>()
                if (authorships != null) {
                    for (a in 0 until minOf(authorships.length(), 8)) {
                        val name = authorships.optJSONObject(a)?.optJSONObject("author")?.optString("display_name").orEmpty()
                        if (name.isNotBlank()) authorNames.add(name)
                    }
                }
                val conceptsArr = obj.optJSONArray("concepts")
                val concepts = mutableListOf<String>()
                if (conceptsArr != null) {
                    for (c in 0 until minOf(conceptsArr.length(), 6)) {
                        val cName = conceptsArr.optJSONObject(c)?.optString("display_name").orEmpty()
                        if (cName.isNotBlank()) concepts.add(cName)
                    }
                }
                val typeStr = obj.optString("type").ifBlank { "article" }
                val isPreprint = typeStr.equals("preprint", ignoreCase = true)
                val isRetracted = obj.optBoolean("is_retracted", false)

                val abstractText = reconstructOpenAlexAbstract(obj.optJSONObject("abstract_inverted_index"))
                    .ifBlank { "Full abstract indexed at publisher / OpenAlex." }

                list.add(
                    ResearchArticle(
                        articleKey = if (rawDoi.isNotBlank()) "doi_${rawDoi.lowercase()}" else "oa_${obj.optString("id").substringAfterLast('/')}",
                        title = title,
                        authors = authorNames.joinToString(", ").ifBlank { "OpenAlex Indexed Authors" },
                        journal = sourceName.ifBlank { if (isPreprint) "Preprint Repository" else "OpenAlex Indexed Source" },
                        publicationDate = obj.optString("publication_date").ifBlank { obj.optString("publication_year") },
                        abstractText = abstractText,
                        doi = rawDoi,
                        publicationType = typeStr.replaceFirstChar { it.uppercase() },
                        keywords = concepts,
                        sourceDatabase = "OpenAlex",
                        contributingSources = listOf("OpenAlex"),
                        citationCount = obj.optInt("cited_by_count", 0),
                        publisherUrl = landingUrl.ifBlank { if (rawDoi.isNotBlank()) "https://doi.org/$rawDoi" else obj.optString("id") },
                        openAccess = isOa,
                        fullTextAvailable = isOa && (pdfUrl.isNotBlank() || landingUrl.isNotBlank()),
                        pdfUrl = pdfUrl,
                        preprint = isPreprint,
                        retracted = isRetracted,
                        retrievedAt = todayIso()
                    )
                )
            }
            list
        }

    private fun reconstructOpenAlexAbstract(invertedIndex: JSONObject?): String {
        if (invertedIndex == null) return ""
        val wordPositions = mutableListOf<Pair<Int, String>>()
        val keys = invertedIndex.keys()
        while (keys.hasNext()) {
            val word = keys.next()
            val positions = invertedIndex.optJSONArray(word) ?: continue
            for (i in 0 until positions.length()) {
                wordPositions.add(positions.optInt(i) to word)
            }
        }
        return wordPositions.sortedBy { it.first }.joinToString(" ") { it.second }
    }

    suspend fun searchSemanticScholar(filters: SearchFilters, page: Int = 0, pageSize: Int = 12): List<ResearchArticle> =
        withContext(Dispatchers.IO) {
            val query = buildEffectiveQuery(filters)
            val fields = "paperId,externalIds,title,abstract,venue,year,publicationDate,citationCount,isOpenAccess,openAccessPdf,authors,publicationTypes,url"
            val offset = page * pageSize
            val url = "https://api.semanticscholar.org/graph/v1/paper/search?query=${encode(query)}&offset=$offset&limit=$pageSize&fields=$fields"
            val body = executeGet(url) ?: return@withContext emptyList()
            val data = JSONObject(body).optJSONArray("data") ?: return@withContext emptyList()
            val list = mutableListOf<ResearchArticle>()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val title = obj.optString("title").trim()
                if (title.isBlank()) continue
                val extIds = obj.optJSONObject("externalIds")
                val doi = extIds?.optString("DOI").orEmpty()
                val pmid = extIds?.optString("PubMed").orEmpty()
                val pdfUrl = obj.optJSONObject("openAccessPdf")?.optString("url").orEmpty()
                val authorsArr = obj.optJSONArray("authors")
                val authorNames = (0 until minOf(authorsArr?.length() ?: 0, 8)).mapNotNull {
                    authorsArr?.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() }
                }
                val pubTypesArr = obj.optJSONArray("publicationTypes")
                val pubType = if (pubTypesArr != null && pubTypesArr.length() > 0) pubTypesArr.optString(0) else "Journal Article"
                val isOa = obj.optBoolean("isOpenAccess", false) || pdfUrl.isNotBlank()

                list.add(
                    ResearchArticle(
                        articleKey = when {
                            doi.isNotBlank() -> "doi_${doi.lowercase()}"
                            pmid.isNotBlank() -> "pmid_$pmid"
                            else -> "s2_${obj.optString("paperId")}"
                        },
                        title = title,
                        authors = authorNames.joinToString(", ").ifBlank { "Semantic Scholar Authors" },
                        journal = obj.optString("venue").ifBlank { "Semantic Scholar Indexed" },
                        publicationDate = obj.optString("publicationDate").ifBlank { obj.optString("year") },
                        abstractText = obj.optString("abstract").takeIf { it != "null" && it.isNotBlank() }
                            ?: "Abstract available at Semantic Scholar.",
                        doi = doi,
                        pmid = pmid,
                        publicationType = pubType,
                        sourceDatabase = "Semantic Scholar",
                        contributingSources = listOf("Semantic Scholar"),
                        citationCount = obj.optInt("citationCount", 0),
                        publisherUrl = obj.optString("url"),
                        openAccess = isOa,
                        fullTextAvailable = pdfUrl.isNotBlank(),
                        pdfUrl = pdfUrl,
                        preprint = obj.optString("venue").contains("medRxiv", true) || obj.optString("venue").contains("bioRxiv", true),
                        retrievedAt = todayIso()
                    )
                )
            }
            list
        }

    suspend fun searchCrossref(filters: SearchFilters, page: Int = 0, pageSize: Int = 12): List<ResearchArticle> =
        withContext(Dispatchers.IO) {
            val query = buildEffectiveQuery(filters)
            val sortParam = when (filters.sortOption) {
                SortOption.MOST_CITED -> "is-referenced-by-count"
                SortOption.RELEVANCE -> "score"
                else -> "published"
            }
            val url = "https://api.crossref.org/works?query=${encode(query)}&rows=$pageSize&offset=${page * pageSize}&sort=$sortParam&order=desc"
            val body = executeGet(url) ?: return@withContext emptyList()
            val items = JSONObject(body).optJSONObject("message")?.optJSONArray("items") ?: return@withContext emptyList()
            val list = mutableListOf<ResearchArticle>()
            for (i in 0 until items.length()) {
                val obj = items.optJSONObject(i) ?: continue
                val title = obj.optJSONArray("title")?.optString(0)?.trim().orEmpty()
                if (title.isBlank()) continue
                val doi = obj.optString("DOI")
                val journal = obj.optJSONArray("container-title")?.optString(0).orEmpty()
                val authorsArr = obj.optJSONArray("author")
                val authorNames = (0 until minOf(authorsArr?.length() ?: 0, 8)).mapNotNull { idx ->
                    val a = authorsArr?.optJSONObject(idx) ?: return@mapNotNull null
                    "${a.optString("family")} ${a.optString("given").take(1)}".trim().takeIf { it.isNotBlank() }
                }
                val dateParts = obj.optJSONObject("published")?.optJSONArray("date-parts")?.optJSONArray(0)
                val pubDate = if (dateParts != null) {
                    (0 until dateParts.length()).map { dateParts.optInt(it).toString().padStart(2, '0') }.joinToString("-")
                } else ""
                var pdfLink = ""
                val linksArr = obj.optJSONArray("link")
                if (linksArr != null) {
                    for (l in 0 until linksArr.length()) {
                        val linkObj = linksArr.optJSONObject(l) ?: continue
                        if (linkObj.optString("content-type").contains("pdf", ignoreCase = true)) {
                            pdfLink = linkObj.optString("URL")
                            break
                        }
                    }
                }
                val typeStr = obj.optString("type").ifBlank { "journal-article" }
                val updateTo = obj.optJSONArray("update-to")
                var isRetracted = false
                if (updateTo != null) {
                    for (u in 0 until updateTo.length()) {
                        if (updateTo.optJSONObject(u)?.optString("type")?.contains("retraction", true) == true) {
                            isRetracted = true
                        }
                    }
                }
                val rawAbstract = obj.optString("abstract").replace(Regex("<[^>]+>"), "").trim()

                list.add(
                    ResearchArticle(
                        articleKey = if (doi.isNotBlank()) "doi_${doi.lowercase()}" else "cr_${title.hashCode()}",
                        title = title,
                        authors = authorNames.joinToString(", ").ifBlank { "Crossref Indexed Authors" },
                        journal = journal.ifBlank { "Crossref Publisher" },
                        publicationDate = pubDate.ifBlank { "Recent" },
                        abstractText = rawAbstract.ifBlank { "Metadata verified via Crossref DOI registry." },
                        doi = doi,
                        publicationType = typeStr,
                        sourceDatabase = "Crossref",
                        contributingSources = listOf("Crossref"),
                        citationCount = obj.optInt("is-referenced-by-count", 0),
                        publisherUrl = obj.optString("URL").ifBlank { "https://doi.org/$doi" },
                        openAccess = pdfLink.isNotBlank(),
                        fullTextAvailable = pdfLink.isNotBlank(),
                        pdfUrl = pdfLink,
                        preprint = typeStr.contains("posted-content", true),
                        retracted = isRetracted,
                        retrievedAt = todayIso()
                    )
                )
            }
            list
        }

    suspend fun searchDoaj(filters: SearchFilters, page: Int = 0, pageSize: Int = 10): List<ResearchArticle> =
        withContext(Dispatchers.IO) {
            val query = buildEffectiveQuery(filters)
            val url = "https://doaj.org/api/search/articles/${encode(query)}?page=${page + 1}&pageSize=$pageSize"
            val body = executeGet(url) ?: return@withContext emptyList()
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            val list = mutableListOf<ResearchArticle>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val bib = item.optJSONObject("bibjson") ?: continue
                val title = bib.optString("title").trim()
                if (title.isBlank()) continue
                var doi = ""
                val ids = bib.optJSONArray("identifier")
                if (ids != null) {
                    for (k in 0 until ids.length()) {
                        val idObj = ids.optJSONObject(k) ?: continue
                        if (idObj.optString("type").equals("doi", true)) {
                            doi = idObj.optString("id")
                            break
                        }
                    }
                }
                var fullTextUrl = ""
                var pdfUrl = ""
                val links = bib.optJSONArray("link")
                if (links != null) {
                    for (k in 0 until links.length()) {
                        val lObj = links.optJSONObject(k) ?: continue
                        val u = lObj.optString("url")
                        if (u.endsWith(".pdf", ignoreCase = true) || lObj.optString("content_type").contains("pdf", true)) {
                            pdfUrl = u
                        }
                        if (fullTextUrl.isEmpty()) fullTextUrl = u
                    }
                }
                val authorsArr = bib.optJSONArray("author")
                val authors = (0 until minOf(authorsArr?.length() ?: 0, 8)).mapNotNull {
                    authorsArr?.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() }
                }
                val journal = bib.optJSONObject("journal")?.optString("title").orEmpty()
                val year = bib.optString("year")
                val month = bib.optString("month")

                list.add(
                    ResearchArticle(
                        articleKey = if (doi.isNotBlank()) "doi_${doi.lowercase()}" else "doaj_${item.optString("id")}",
                        title = title,
                        authors = authors.joinToString(", ").ifBlank { "DOAJ Open Access Authors" },
                        journal = journal.ifBlank { "DOAJ Open Access Journal" },
                        publicationDate = listOf(year, month).filter { it.isNotBlank() }.joinToString("-").ifBlank { "Open Access" },
                        abstractText = bib.optString("abstract").ifBlank { "Open Access article indexed in DOAJ." },
                        doi = doi,
                        publicationType = "Open Access Article",
                        sourceDatabase = "DOAJ",
                        contributingSources = listOf("DOAJ"),
                        publisherUrl = fullTextUrl.ifBlank { if (doi.isNotBlank()) "https://doi.org/$doi" else "https://doaj.org/" },
                        openAccess = true,
                        fullTextAvailable = fullTextUrl.isNotBlank(),
                        pdfUrl = pdfUrl,
                        retrievedAt = todayIso()
                    )
                )
            }
            list
        }

    /**
     * Unpaywall DOI lookup subject to Unpaywall API terms and email requirement.
     * Never invents PDF links; only returns verified legal OA pdf_url if present.
     */
    suspend fun resolveUnpaywall(doi: String, email: String): UnpaywallResult? =
        withContext(Dispatchers.IO) {
            val cleanDoi = doi.trim().removePrefix("https://doi.org/").removePrefix("http://doi.org/")
            val cleanEmail = email.trim()
            if (cleanDoi.isBlank() || !cleanEmail.contains("@")) return@withContext null
            val url = "https://api.unpaywall.org/v2/${encode(cleanDoi)}?email=${encode(cleanEmail)}"
            val body = executeGet(url) ?: return@withContext null
            val obj = JSONObject(body)
            val isOa = obj.optBoolean("is_oa", false)
            val bestLoc = obj.optJSONObject("best_oa_location")
            val pdfUrl = bestLoc?.optString("url_for_pdf").orEmpty().takeIf { it != "null" }.orEmpty()
            val landingUrl = bestLoc?.optString("url_for_landing_page").orEmpty().takeIf { it != "null" }.orEmpty()
            UnpaywallResult(
                isOpenAccess = isOa,
                legalPdfUrl = pdfUrl,
                landingUrl = landingUrl
            )
        }
}

data class UnpaywallResult(
    val isOpenAccess: Boolean,
    val legalPdfUrl: String,
    val landingUrl: String
)
