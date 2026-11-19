package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.domain.model.ClinicalCaseAnalysisResult
import com.example.domain.model.ClinicalCaseInput
import com.example.domain.model.ResearchArticle
import com.example.domain.model.SafetyScreenResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClinicalReportPdfExporter(private val context: Context) {

    suspend fun exportDeidentifiedReportPdf(
        caseInput: ClinicalCaseInput,
        safety: SafetyScreenResult,
        analysis: ClinicalCaseAnalysisResult,
        studies: List<ResearchArticle>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val reportsDir = File(context.filesDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val outFile = File(reportsDir, "AlAnhar_Clinical_Report_$timestamp.pdf")

            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 40f
            val contentWidth = pageWidth - margin * 2

            val titlePaint = Paint().apply {
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.rgb(11, 60, 93)
            }
            val headerPaint = Paint().apply {
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.rgb(13, 148, 136)
            }
            val bodyPaint = Paint().apply {
                textSize = 9.5f
                color = android.graphics.Color.rgb(30, 41, 59)
            }
            val alertPaint = Paint().apply {
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.rgb(185, 28, 28)
            }

            var pageNum = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas
            var y = 48f

            fun ensureSpace(needed: Float) {
                if (y + needed > pageHeight - 40f) {
                    document.finishPage(page)
                    pageNum++
                    page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = 48f
                }
            }

            fun writeWrapped(text: String, paint: Paint, lineHeight: Float = 13f) {
                val paragraphs = text.ifBlank { "Not reported / None" }.split("\n")
                for (para in paragraphs) {
                    val words = para.split(" ")
                    val line = StringBuilder()
                    for (word in words) {
                        val candidate = if (line.isEmpty()) word else "${line} $word"
                        if (paint.measureText(candidate) > contentWidth) {
                            ensureSpace(lineHeight)
                            canvas.drawText(line.toString(), margin, y, paint)
                            y += lineHeight
                            line.clear().append(word)
                        } else {
                            line.clear().append(candidate)
                        }
                    }
                    if (line.isNotEmpty()) {
                        ensureSpace(lineHeight)
                        canvas.drawText(line.toString(), margin, y, paint)
                        y += lineHeight
                    }
                }
            }

            fun writeSection(title: String, body: String, isAlert: Boolean = false) {
                ensureSpace(32f)
                y += 6f
                canvas.drawText(title, margin, y, headerPaint)
                y += 14f
                writeWrapped(body, if (isAlert) alertPaint else bodyPaint)
                y += 4f
            }

            canvas.drawText("AL-ANHAR CLINICAL INTELLIGENCE — EDUCATIONAL CASE REPORT", margin, y, titlePaint)
            y += 16f
            writeWrapped("Al-Anhar Specialized Hospital | De-identified Knowledge-Support Report | Generated: $timestamp", bodyPaint)
            writeWrapped("DISCLAIMER: Educational clinical knowledge-support only. Not a diagnostic or treatment order. No patient-identifying information included.", alertPaint)
            y += 8f

            writeSection("1. Case Identification (De-identified)", "Topic Identified: ${analysis.identifiedTopic}\nReport Timestamp: $timestamp")
            writeSection("2. Preliminary Diagnosis / Impression", caseInput.preliminaryDiagnosis.ifBlank { "Not entered" })
            writeSection("3. Clinical Summary", analysis.aiEducationalInterpretation)
            writeSection("4. Available Patient Information", analysis.availableInformationSummary.ifBlank { caseInput.buildDeidentifiedSummaryText() })
            writeSection(
                "5. Missing Clinical Information",
                analysis.missingInformation.joinToString("\n") { "[${it.priority.name}] ${it.fieldName}: ${it.whyItMatters}" }
            )
            writeSection("6. Relevant Diagnostic Criteria", analysis.establishedMedicalEvidence)
            writeSection(
                "7. Differential Diagnosis (For Professional Evaluation Only)",
                analysis.differentialDiagnoses.joinToString("\n\n") { d ->
                    "${d.conditionName}: ${d.definition}\nSupporting: ${d.supportingFeatures.joinToString(", ")}\nCriteria: ${d.diagnosticCriteria}"
                }
            )
            writeSection("8. Relevant Investigations", analysis.relevantInvestigations.joinToString("\n• ", prefix = "• "))
            writeSection("9. Clinical Safety Considerations", "${safety.escalationMessage}\n" + safety.flags.joinToString("\n") { "- ${it.parameter}: ${it.observedValue} (${it.clinicalConcern})" }, isAlert = safety.flags.isNotEmpty())
            writeSection("10. Relevant Guidelines", analysis.publishedGuidelinesSummary)
            writeSection("11. Latest Scientific Research", studies.take(6).joinToString("\n") { "- ${it.title} (${it.journal}, ${it.publicationDate}) DOI:${it.doi}" })
            writeSection("12. Evidence Comparison & Research Context", analysis.researchFindingsSummary)
            writeSection("13. Traceable References", (analysis.traceableReferences + studies.mapNotNull { it.doi.takeIf { d -> d.isNotBlank() }?.let { d -> "https://doi.org/$d" } }).distinct().joinToString("\n"))

            document.finishPage(page)
            outFile.outputStream().use { document.writeTo(it) }
            document.close()
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sharePdf(file: File): Result<Unit> {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share De-identified Clinical Report PDF"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
