package com.example.data.repository

import android.util.Log
import com.example.domain.model.ClinicalCaseAnalysisResult
import com.example.domain.model.ClinicalCaseInput
import com.example.domain.model.DiagnosisKnowledgeReport
import com.example.domain.model.DifferentialDiagnosisItem
import com.example.domain.model.EvidenceComparisonResult
import com.example.domain.model.GuidelineRecord
import com.example.domain.model.MissingInfoItem
import com.example.domain.model.MissingPriority
import com.example.domain.model.PaperAiAnalysis
import com.example.domain.model.ResearchArticle
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "GeminiSciAssistant"
private const val MODEL_NAME = "gemini-3.8-flash"

class GeminiScientificAssistant {

    // 1. Structured 10-Point Research Paper Analysis Schema
    private val paperSchema = Schema.obj(
        properties = mapOf(
            "researchObjective" to Schema.string(description = "Primary research objective stated in the paper"),
            "background" to Schema.string(description = "Scientific and clinical background reported"),
            "studyDesign" to Schema.string(description = "Study design (e.g., RCT, Cohort, Meta-analysis)"),
            "methodology" to Schema.string(description = "Methods and interventions studied"),
            "studyPopulation" to Schema.string(description = "Population, sample size, or setting if reported; state Not reported in abstract if absent"),
            "mainFindings" to Schema.string(description = "Key results and measurements actually reported"),
            "statisticalAnalysis" to Schema.string(description = "Statistical methods or metrics reported, or state Not detailed in abstract"),
            "clinicalImplications" to Schema.string(description = "Educational interpretation of clinical relevance without issuing treatment orders"),
            "studyLimitations" to Schema.string(description = "Limitations noted or inherent to abstract-only review"),
            "conclusions" to Schema.string(description = "Conclusions reported by the authors"),
            "respiratoryParametersExtracted" to Schema.array(
                items = Schema.string(),
                description = "Respiratory/ICU parameters found (e.g. PaO2/FiO2, PEEP, Tidal Volume, Ventilator settings, Respiratory Mechanics, ICU LOS, Mortality)"
            ),
            "publishedVsAiDistinction" to Schema.string(description = "Clear statement distinguishing source-reported findings from AI educational interpretation")
        )
    )

    private val paperModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = paperSchema
            }
        )
    }

    // 2. Structured Clinical Case Analyzer + Missing Information + Differential Schema
    private val caseSchema = Schema.obj(
        properties = mapOf(
            "identifiedTopic" to Schema.string(description = "Primary respiratory or critical care topic identified"),
            "availableInformationSummary" to Schema.string(description = "Structured summary strictly of the information provided by the clinician; never invent missing measurements"),
            "establishedMedicalEvidence" to Schema.string(description = "A. Established medical textbook/consensus evidence relevant to this presentation"),
            "publishedGuidelinesSummary" to Schema.string(description = "B. Summary of relevant published international guidelines (ATS/ERS/SCCM/ESICM/GOLD/GINA)"),
            "researchFindingsSummary" to Schema.string(description = "C. Findings from the provided indexed research references"),
            "aiEducationalInterpretation" to Schema.string(description = "D. AI-generated educational synthesis only; NO prescriptions, drug dosages, ventilator orders, or autonomous diagnosis"),
            "missingInformation" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "fieldName" to Schema.string(),
                        "priority" to Schema.enumeration(listOf("ESSENTIAL", "IMPORTANT", "OPTIONAL")),
                        "whyItMatters" to Schema.string()
                    )
                ),
                description = "E. Structured missing clinical information detector"
            ),
            "assessmentChecklist" to Schema.array(
                items = Schema.string(),
                description = "Actionable clinical assessment checklist items for the healthcare team"
            ),
            "differentialDiagnoses" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "conditionName" to Schema.string(),
                        "definition" to Schema.string(),
                        "supportingFeatures" to Schema.array(items = Schema.string()),
                        "featuresArguingAgainst" to Schema.array(items = Schema.string()),
                        "diagnosticCriteria" to Schema.string(),
                        "evaluationInvestigations" to Schema.array(items = Schema.string()),
                        "supportingReferences" to Schema.array(items = Schema.string())
                    )
                ),
                description = "Possibilities for professional evaluation, explicitly NOT confirmed diagnoses"
            ),
            "relevantInvestigations" to Schema.array(items = Schema.string()),
            "traceableReferences" to Schema.array(items = Schema.string())
        )
    )

    private val caseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = caseSchema
            }
        )
    }

    // 3. 17-Point Diagnosis Explorer Schema
    private val diagnosisSchema = Schema.obj(
        properties = mapOf(
            "topicTitle" to Schema.string(),
            "criteriaVsImpressionDistinction" to Schema.string(description = "Clear distinction between established diagnostic criteria and preliminary clinical impressions"),
            "definition" to Schema.string(),
            "epidemiology" to Schema.string(),
            "pathophysiology" to Schema.string(),
            "riskFactors" to Schema.string(),
            "clinicalPresentation" to Schema.string(),
            "diagnosticCriteria" to Schema.string(),
            "diseaseClassification" to Schema.string(),
            "severityAssessment" to Schema.string(),
            "differentialDiagnoses" to Schema.string(),
            "relevantInvestigations" to Schema.string(),
            "clinicalAssessmentConsiderations" to Schema.string(),
            "potentialComplications" to Schema.string(),
            "evidenceBasedManagementPrinciples" to Schema.string(description = "High-level evidence-based principles only; no individualized prescriptions or orders"),
            "monitoringConsiderations" to Schema.string(),
            "prognosticIndicators" to Schema.string(),
            "latestGuidelinesSummary" to Schema.string(),
            "traceableReferences" to Schema.array(items = Schema.string())
        )
    )

    private val diagnosisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosisSchema
            }
        )
    }

    // 4. Evidence Comparison Schema
    private val comparisonSchema = Schema.obj(
        properties = mapOf(
            "topic" to Schema.string(),
            "earlierGuidelineLabel" to Schema.string(),
            "newerGuidelineLabel" to Schema.string(),
            "verificationNote" to Schema.string(),
            "areasOfAgreement" to Schema.array(items = Schema.string()),
            "areasOfDisagreement" to Schema.array(items = Schema.string()),
            "updatedRecommendations" to Schema.array(items = Schema.string()),
            "supportingEvidenceAndTrials" to Schema.array(items = Schema.string())
        )
    )

    private val comparisonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = comparisonSchema
            }
        )
    }

    suspend fun analyzeResearchPaper(article: ResearchArticle, isArabic: Boolean): Result<PaperAiAnalysis> {
        val langNote = if (isArabic) "Respond in clear medical Arabic with standard English clinical terms/acronyms in parentheses." else "Respond in precise academic medical English."
        val prompt = """
            You are the AI Scientific Research Assistant for Al-Anhar Scientific Journal (Respiratory & Critical Care).
            STRICT RULES:
            - Extract ONLY information actually reported in the provided title, metadata, and abstract.
            - If a detail (e.g., statistical p-value, sample size, ventilator parameter) is not in the provided text, explicitly state "Not reported in provided abstract/metadata".
            - Clearly distinguish published findings from AI educational interpretation.
            - NEVER generate medical treatment recommendations from an isolated research paper.
            - $langNote

            ARTICLE METADATA:
            Title: ${article.title}
            Authors: ${article.authors}
            Journal: ${article.journal}
            Publication Date: ${article.publicationDate}
            Publication Type: ${article.publicationType}
            DOI: ${article.doi.ifBlank { "N/A" }}
            PMID: ${article.pmid.ifBlank { "N/A" }}
            Source: ${article.contributingSources.joinToString(", ")}
            Abstract:
            ${article.abstractText}
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                paperModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
            Result.success(
                PaperAiAnalysis(
                    researchObjective = json.optString("researchObjective"),
                    background = json.optString("background"),
                    studyDesign = json.optString("studyDesign"),
                    methodology = json.optString("methodology"),
                    studyPopulation = json.optString("studyPopulation"),
                    mainFindings = json.optString("mainFindings"),
                    statisticalAnalysis = json.optString("statisticalAnalysis"),
                    clinicalImplications = json.optString("clinicalImplications"),
                    studyLimitations = json.optString("studyLimitations"),
                    conclusions = json.optString("conclusions"),
                    respiratoryParametersExtracted = jsonArrayToList(json.optJSONArray("respiratoryParametersExtracted")),
                    publishedVsAiDistinction = json.optString("publishedVsAiDistinction")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Paper analysis timed out", e)
            Result.failure(IllegalStateException("AI analysis timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Paper analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeClinicalCase(
        caseInput: ClinicalCaseInput,
        evidenceRefs: List<ResearchArticle>,
        isArabic: Boolean
    ): Result<ClinicalCaseAnalysisResult> {
        val langNote = if (isArabic) "Write output in professional Arabic medical terminology with English clinical acronyms." else "Write output in professional clinical English."
        val refContext = evidenceRefs.take(6).joinToString("\n") {
            "- ${it.title} (${it.journal}, ${it.publicationDate}) [DOI: ${it.doi.ifBlank { "N/A" }}, PMID: ${it.pmid.ifBlank { "N/A" }}]"
        }.ifBlank { "No external studies attached yet; cite only verified standard guidelines by exact society and year." }

        val prompt = """
            You are AL-ANHAR CLINICAL INTELLIGENCE, an evidence-based clinical knowledge-support assistant.
            MANDATORY CLINICAL SAFETY & ACCURACY RULES:
            1. Never autonomously diagnose a patient. Never prescribe drugs, dosages, or ventilator orders.
            2. Never invent or assume missing measurements, vitals, ABGs, or lab values.
            3. If available data is insufficient, explicitly mark the impression as preliminary/unconfirmed and list all missing items classified as ESSENTIAL, IMPORTANT, or OPTIONAL with why each matters.
            4. Clearly separate Established Medical Evidence, Published Guidelines, Research Findings, AI Educational Interpretation, and Missing Information.
            5. Only cite traceable references from the verified list below or well-established major society guidelines (never fabricate DOIs/PMIDs).
            6. $langNote

            DE-IDENTIFIED CASE DATA ENTERED BY CLINICIAN:
            ${caseInput.buildDeidentifiedSummaryText()}

            VERIFIED INDEXED STUDIES RETRIEVED FOR THIS CASE:
            $refContext
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                caseModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
            val missingArr = json.optJSONArray("missingInformation")
            val missingItems = mutableListOf<MissingInfoItem>()
            if (missingArr != null) {
                for (i in 0 until missingArr.length()) {
                    val o = missingArr.optJSONObject(i) ?: continue
                    val pri = enumValues<MissingPriority>()
                        .firstOrNull { it.name == o.optString("priority") } ?: MissingPriority.IMPORTANT
                    missingItems.add(
                        MissingInfoItem(
                            fieldName = o.optString("fieldName"),
                            priority = pri,
                            whyItMatters = o.optString("whyItMatters")
                        )
                    )
                }
            }

            val diffArr = json.optJSONArray("differentialDiagnoses")
            val diffs = mutableListOf<DifferentialDiagnosisItem>()
            if (diffArr != null) {
                for (i in 0 until diffArr.length()) {
                    val o = diffArr.optJSONObject(i) ?: continue
                    diffs.add(
                        DifferentialDiagnosisItem(
                            conditionName = o.optString("conditionName"),
                            definition = o.optString("definition"),
                            supportingFeatures = jsonArrayToList(o.optJSONArray("supportingFeatures")),
                            featuresArguingAgainst = jsonArrayToList(o.optJSONArray("featuresArguingAgainst")),
                            diagnosticCriteria = o.optString("diagnosticCriteria"),
                            evaluationInvestigations = jsonArrayToList(o.optJSONArray("evaluationInvestigations")),
                            supportingReferences = jsonArrayToList(o.optJSONArray("supportingReferences"))
                        )
                    )
                }
            }

            Result.success(
                ClinicalCaseAnalysisResult(
                    identifiedTopic = json.optString("identifiedTopic"),
                    availableInformationSummary = json.optString("availableInformationSummary"),
                    establishedMedicalEvidence = json.optString("establishedMedicalEvidence"),
                    publishedGuidelinesSummary = json.optString("publishedGuidelinesSummary"),
                    researchFindingsSummary = json.optString("researchFindingsSummary"),
                    aiEducationalInterpretation = json.optString("aiEducationalInterpretation"),
                    missingInformation = missingItems,
                    assessmentChecklist = jsonArrayToList(json.optJSONArray("assessmentChecklist")),
                    differentialDiagnoses = diffs,
                    relevantInvestigations = jsonArrayToList(json.optJSONArray("relevantInvestigations")),
                    traceableReferences = jsonArrayToList(json.optJSONArray("traceableReferences"))
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Clinical case analysis timed out", e)
            Result.failure(IllegalStateException("Clinical case analysis timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Clinical case analysis failed", e)
            Result.failure(e)
        }
    }

    suspend fun exploreDiagnosis(
        query: String,
        evidenceRefs: List<ResearchArticle>,
        guidelines: List<GuidelineRecord>,
        isArabic: Boolean
    ): Result<DiagnosisKnowledgeReport> {
        val langNote = if (isArabic) "Respond in clear Arabic clinical language with standard English terms in parentheses." else "Respond in structured academic medical English."
        val refContext = evidenceRefs.take(5).joinToString("\n") {
            "- ${it.title} (${it.journal}, ${it.publicationDate}) [DOI: ${it.doi}, PMID: ${it.pmid}]"
        }
        val guideContext = guidelines.take(4).joinToString("\n") {
            "- ${it.organization} (${it.publicationYear}): ${it.title} — ${it.recommendations}"
        }

        val prompt = """
            Generate a structured 17-point educational clinical knowledge report for the respiratory or critical care topic/diagnosis: "$query".
            RULES:
            - Clearly distinguish established diagnostic criteria from preliminary clinical impressions.
            - Provide general evidence-based management principles ONLY — NO individualized drug dosages, prescriptions, or patient-specific ventilator orders.
            - Never fabricate references or DOIs.
            - $langNote

            VERIFIED GUIDELINES CONTEXT:
            $guideContext

            VERIFIED RECENT STUDIES:
            $refContext
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                diagnosisModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
            Result.success(
                DiagnosisKnowledgeReport(
                    topicTitle = json.optString("topicTitle"),
                    criteriaVsImpressionDistinction = json.optString("criteriaVsImpressionDistinction"),
                    definition = json.optString("definition"),
                    epidemiology = json.optString("epidemiology"),
                    pathophysiology = json.optString("pathophysiology"),
                    riskFactors = json.optString("riskFactors"),
                    clinicalPresentation = json.optString("clinicalPresentation"),
                    diagnosticCriteria = json.optString("diagnosticCriteria"),
                    diseaseClassification = json.optString("diseaseClassification"),
                    severityAssessment = json.optString("severityAssessment"),
                    differentialDiagnoses = json.optString("differentialDiagnoses"),
                    relevantInvestigations = json.optString("relevantInvestigations"),
                    clinicalAssessmentConsiderations = json.optString("clinicalAssessmentConsiderations"),
                    potentialComplications = json.optString("potentialComplications"),
                    evidenceBasedManagementPrinciples = json.optString("evidenceBasedManagementPrinciples"),
                    monitoringConsiderations = json.optString("monitoringConsiderations"),
                    prognosticIndicators = json.optString("prognosticIndicators"),
                    latestGuidelinesSummary = json.optString("latestGuidelinesSummary"),
                    traceableReferences = jsonArrayToList(json.optJSONArray("traceableReferences"))
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Diagnosis exploration timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Diagnosis exploration failed", e)
            Result.failure(e)
        }
    }

    suspend fun compareClinicalEvidence(
        topic: String,
        guidelines: List<GuidelineRecord>,
        studies: List<ResearchArticle>,
        isArabic: Boolean
    ): Result<EvidenceComparisonResult> {
        val langNote = if (isArabic) "Respond in Arabic medical terminology." else "Respond in precise medical English."
        val guideStr = guidelines.joinToString("\n") {
            "- [${it.organization}, ${it.publicationYear}, updated ${it.latestUpdateDate}, verified ${it.lastVerifiedDate}] ${it.title}. Prev: ${it.previousVersionSummary}. Changes: ${it.changesSummary}. Recs: ${it.recommendations}"
        }
        val studyStr = studies.take(6).joinToString("\n") {
            "- ${it.title} (${it.journal}, ${it.publicationDate}, ${it.publicationType}, DOI: ${it.doi})"
        }
        val prompt = """
            Compare published clinical guidelines and research evidence for: "$topic".
            RULES:
            - Never invent changes between guidelines.
            - If no verified newer guideline update exists in the provided context, explicitly state that no newer verified update was identified.
            - $langNote

            VERIFIED GUIDELINE VERSIONS:
            $guideStr

            INDEXED STUDIES:
            $studyStr
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                comparisonModel.generateContent(prompt)
            }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty AI response"))
            Result.success(
                EvidenceComparisonResult(
                    topic = json.optString("topic"),
                    earlierGuidelineLabel = json.optString("earlierGuidelineLabel"),
                    newerGuidelineLabel = json.optString("newerGuidelineLabel"),
                    verificationNote = json.optString("verificationNote"),
                    areasOfAgreement = jsonArrayToList(json.optJSONArray("areasOfAgreement")),
                    areasOfDisagreement = jsonArrayToList(json.optJSONArray("areasOfDisagreement")),
                    updatedRecommendations = jsonArrayToList(json.optJSONArray("updatedRecommendations")),
                    supportingEvidenceAndTrials = jsonArrayToList(json.optJSONArray("supportingEvidenceAndTrials"))
                )
            )
        } catch (e: TimeoutCancellationException) {
            Result.failure(IllegalStateException("Evidence comparison timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Evidence comparison failed", e)
            Result.failure(e)
        }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    }
}
