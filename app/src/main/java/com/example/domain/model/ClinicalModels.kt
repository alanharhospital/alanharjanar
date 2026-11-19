package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SafetyAlertLevel {
    NORMAL,
    CAUTION,
    URGENT
}

data class SafetyFlag(
    val parameter: String,
    val observedValue: String,
    val clinicalConcern: String,
    val level: SafetyAlertLevel
)

data class SafetyScreenResult(
    val overallLevel: SafetyAlertLevel,
    val flags: List<SafetyFlag>,
    val escalationMessage: String
)

data class ClinicalCaseInput(
    // A. Basic Patient Info (Zero PII - no name, MRN, or phone)
    val ageYears: String = "",
    val demographicsNote: String = "",
    val chiefComplaint: String = "",
    val preliminaryDiagnosis: String = "",
    val medicalHistory: String = "",
    val comorbidities: String = "",
    // B. Clinical Assessment
    val heartRateBpm: String = "",
    val respiratoryRateBpm: String = "",
    val bloodPressureMmHg: String = "",
    val temperatureCelsius: String = "",
    val spo2Percent: String = "",
    val consciousnessLevel: String = "",
    val physicalExamFindings: String = "",
    // C. Respiratory & ABG Assessment
    val abgPh: String = "",
    val paco2MmHg: String = "",
    val pao2MmHg: String = "",
    val hco3MeqL: String = "",
    val sao2Percent: String = "",
    val lactateMmolL: String = "",
    val fio2Fraction: String = "",
    val peepCmH2o: String = "",
    val tidalVolumeMl: String = "",
    val ventilatorMode: String = "",
    val airwayStatus: String = "",
    val oxygenDeliveryMethod: String = "",
    // D. Laboratory Investigations
    val cbc: String = "",
    val electrolytes: String = "",
    val renalFunction: String = "",
    val liverFunction: String = "",
    val inflammatoryMarkers: String = "",
    val microbiology: String = "",
    val otherLabs: String = "",
    // E. Imaging & Additional Reports
    val chestXray: String = "",
    val ctFindings: String = "",
    val ultrasoundFindings: String = "",
    val echoFindings: String = "",
    val otherReports: String = ""
) {
    fun buildDeidentifiedSummaryText(): String = buildString {
        if (ageYears.isNotBlank() || demographicsNote.isNotBlank()) {
            appendLine("Demographics (De-identified): Age=${ageYears.ifBlank { "Not provided" }}, Notes=${demographicsNote.ifBlank { "None" }}")
        }
        if (chiefComplaint.isNotBlank()) appendLine("Chief Complaint: $chiefComplaint")
        if (preliminaryDiagnosis.isNotBlank()) appendLine("Preliminary Clinical Impression: $preliminaryDiagnosis")
        if (medicalHistory.isNotBlank()) appendLine("Medical History: $medicalHistory")
        if (comorbidities.isNotBlank()) appendLine("Comorbidities: $comorbidities")
        val vitals = listOfNotNull(
            heartRateBpm.takeIf { it.isNotBlank() }?.let { "HR $it bpm" },
            respiratoryRateBpm.takeIf { it.isNotBlank() }?.let { "RR $it /min" },
            bloodPressureMmHg.takeIf { it.isNotBlank() }?.let { "BP $it mmHg" },
            temperatureCelsius.takeIf { it.isNotBlank() }?.let { "Temp $it °C" },
            spo2Percent.takeIf { it.isNotBlank() }?.let { "SpO2 $it%" },
            consciousnessLevel.takeIf { it.isNotBlank() }?.let { "Consciousness: $it" }
        )
        if (vitals.isNotEmpty()) appendLine("Vitals & Consciousness: ${vitals.joinToString(", ")}")
        if (physicalExamFindings.isNotBlank()) appendLine("Physical Exam: $physicalExamFindings")

        val resp = listOfNotNull(
            abgPh.takeIf { it.isNotBlank() }?.let { "pH $it" },
            paco2MmHg.takeIf { it.isNotBlank() }?.let { "PaCO2 $it mmHg" },
            pao2MmHg.takeIf { it.isNotBlank() }?.let { "PaO2 $it mmHg" },
            hco3MeqL.takeIf { it.isNotBlank() }?.let { "HCO3 $it mEq/L" },
            sao2Percent.takeIf { it.isNotBlank() }?.let { "SaO2 $it%" },
            lactateMmolL.takeIf { it.isNotBlank() }?.let { "Lactate $it mmol/L" },
            fio2Fraction.takeIf { it.isNotBlank() }?.let { "FiO2 $it" },
            peepCmH2o.takeIf { it.isNotBlank() }?.let { "PEEP $it cmH2O" },
            tidalVolumeMl.takeIf { it.isNotBlank() }?.let { "Vt $it mL" },
            ventilatorMode.takeIf { it.isNotBlank() }?.let { "Vent Mode: $it" },
            airwayStatus.takeIf { it.isNotBlank() }?.let { "Airway: $it" },
            oxygenDeliveryMethod.takeIf { it.isNotBlank() }?.let { "O2 Delivery: $it" }
        )
        if (resp.isNotEmpty()) appendLine("Respiratory & ABG: ${resp.joinToString(", ")}")

        val labs = listOfNotNull(
            cbc.takeIf { it.isNotBlank() }?.let { "CBC: $it" },
            electrolytes.takeIf { it.isNotBlank() }?.let { "Electrolytes: $it" },
            renalFunction.takeIf { it.isNotBlank() }?.let { "Renal: $it" },
            liverFunction.takeIf { it.isNotBlank() }?.let { "Liver: $it" },
            inflammatoryMarkers.takeIf { it.isNotBlank() }?.let { "Inflammatory: $it" },
            microbiology.takeIf { it.isNotBlank() }?.let { "Microbiology: $it" },
            otherLabs.takeIf { it.isNotBlank() }?.let { "Other Labs: $it" }
        )
        if (labs.isNotEmpty()) appendLine("Labs: ${labs.joinToString("; ")}")

        val img = listOfNotNull(
            chestXray.takeIf { it.isNotBlank() }?.let { "CXR: $it" },
            ctFindings.takeIf { it.isNotBlank() }?.let { "CT: $it" },
            ultrasoundFindings.takeIf { it.isNotBlank() }?.let { "US: $it" },
            echoFindings.takeIf { it.isNotBlank() }?.let { "Echo: $it" },
            otherReports.takeIf { it.isNotBlank() }?.let { "Other: $it" }
        )
        if (img.isNotEmpty()) appendLine("Imaging: ${img.joinToString("; ")}")
    }.trim()
}

enum class MissingPriority {
    ESSENTIAL,
    IMPORTANT,
    OPTIONAL
}

data class MissingInfoItem(
    val fieldName: String,
    val priority: MissingPriority,
    val whyItMatters: String
)

data class DifferentialDiagnosisItem(
    val conditionName: String,
    val definition: String,
    val supportingFeatures: List<String>,
    val featuresArguingAgainst: List<String>,
    val diagnosticCriteria: String,
    val evaluationInvestigations: List<String>,
    val supportingReferences: List<String>
)

data class ClinicalCaseAnalysisResult(
    val identifiedTopic: String,
    val availableInformationSummary: String,
    val establishedMedicalEvidence: String,
    val publishedGuidelinesSummary: String,
    val researchFindingsSummary: String,
    val aiEducationalInterpretation: String,
    val missingInformation: List<MissingInfoItem>,
    val assessmentChecklist: List<String>,
    val differentialDiagnoses: List<DifferentialDiagnosisItem>,
    val relevantInvestigations: List<String>,
    val traceableReferences: List<String>
)

data class DiagnosisKnowledgeReport(
    val topicTitle: String,
    val criteriaVsImpressionDistinction: String,
    val definition: String,
    val epidemiology: String,
    val pathophysiology: String,
    val riskFactors: String,
    val clinicalPresentation: String,
    val diagnosticCriteria: String,
    val diseaseClassification: String,
    val severityAssessment: String,
    val differentialDiagnoses: String,
    val relevantInvestigations: String,
    val clinicalAssessmentConsiderations: String,
    val potentialComplications: String,
    val evidenceBasedManagementPrinciples: String,
    val monitoringConsiderations: String,
    val prognosticIndicators: String,
    val latestGuidelinesSummary: String,
    val traceableReferences: List<String>
)

data class PaperAiAnalysis(
    val researchObjective: String,
    val background: String,
    val studyDesign: String,
    val methodology: String,
    val studyPopulation: String,
    val mainFindings: String,
    val statisticalAnalysis: String,
    val clinicalImplications: String,
    val studyLimitations: String,
    val conclusions: String,
    val respiratoryParametersExtracted: List<String>,
    val publishedVsAiDistinction: String
)

data class EvidenceComparisonResult(
    val topic: String,
    val earlierGuidelineLabel: String,
    val newerGuidelineLabel: String,
    val verificationNote: String,
    val areasOfAgreement: List<String>,
    val areasOfDisagreement: List<String>,
    val updatedRecommendations: List<String>,
    val supportingEvidenceAndTrials: List<String>
)

@Keep
@IgnoreExtraProperties
data class ClinicalCaseSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val caseTitle: String = "",
    val preliminaryDiagnosis: String = "",
    val specialtyTrack: String = "RESPIRATORY_CARE",
    val demographicsSummary: String = "",
    val vitalsSummary: String = "",
    val respiratoryAbgSummary: String = "",
    val labsSummary: String = "",
    val imagingSummary: String = "",
    val safetyAlertLevel: String = "NORMAL",
    val aiReportSummary: String = "",
    val missingChecklistSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedClinicalTopic(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topicName: String = "",
    val specialtyTrack: String = "RESPIRATORY_CARE",
    val bookmarked: Boolean = false,
    val lastViewedMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
