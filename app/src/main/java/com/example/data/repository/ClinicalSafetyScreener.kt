package com.example.data.repository

import com.example.domain.model.ClinicalCaseInput
import com.example.domain.model.SafetyAlertLevel
import com.example.domain.model.SafetyFlag
import com.example.domain.model.SafetyScreenResult

object ClinicalSafetyScreener {

    fun screen(input: ClinicalCaseInput): SafetyScreenResult {
        val flags = mutableListOf<SafetyFlag>()

        // 1. Oxygenation impairment
        input.spo2Percent.toDoubleOrNull()?.let { spo2 ->
            when {
                spo2 < 88.0 -> flags.add(
                    SafetyFlag("SpO2", "$spo2%", "Marked hypoxemia (SpO2 < 88%)", SafetyAlertLevel.URGENT)
                )
                spo2 < 92.0 -> flags.add(
                    SafetyFlag("SpO2", "$spo2%", "Borderline oxygenation impairment (SpO2 < 92%)", SafetyAlertLevel.CAUTION)
                )
            }
        }
        input.pao2MmHg.toDoubleOrNull()?.let { pao2 ->
            if (pao2 < 55.0) {
                flags.add(SafetyFlag("PaO2", "$pao2 mmHg", "Severe arterial hypoxemia (PaO2 < 55 mmHg)", SafetyAlertLevel.URGENT))
            } else if (pao2 < 65.0) {
                flags.add(SafetyFlag("PaO2", "$pao2 mmHg", "Hypoxemia (PaO2 < 65 mmHg)", SafetyAlertLevel.CAUTION))
            }
        }

        // 2. Acid-base critical thresholds
        input.abgPh.toDoubleOrNull()?.let { ph ->
            when {
                ph < 7.20 -> flags.add(SafetyFlag("Arterial pH", "$ph", "Severe acidemia (pH < 7.20)", SafetyAlertLevel.URGENT))
                ph > 7.55 -> flags.add(SafetyFlag("Arterial pH", "$ph", "Severe alkalemia (pH > 7.55)", SafetyAlertLevel.URGENT))
                ph < 7.30 || ph > 7.50 -> flags.add(SafetyFlag("Arterial pH", "$ph", "Significant acid-base disturbance", SafetyAlertLevel.CAUTION))
            }
        }

        // 3. Respiratory rate / distress
        input.respiratoryRateBpm.toDoubleOrNull()?.let { rr ->
            when {
                rr >= 32.0 || rr <= 8.0 -> flags.add(
                    SafetyFlag("Respiratory Rate", "$rr /min", "Severe respiratory distress or hypoventilation", SafetyAlertLevel.URGENT)
                )
                rr >= 24.0 -> flags.add(
                    SafetyFlag("Respiratory Rate", "$rr /min", "Tachypnea (RR >= 24 /min)", SafetyAlertLevel.CAUTION)
                )
            }
        }

        // 4. Hemodynamics & Lactate
        input.heartRateBpm.toDoubleOrNull()?.let { hr ->
            if (hr >= 130.0 || hr <= 40.0) {
                flags.add(SafetyFlag("Heart Rate", "$hr bpm", "Marked tachycardia or bradycardia", SafetyAlertLevel.URGENT))
            }
        }
        val systolic = input.bloodPressureMmHg.split("/").firstOrNull()?.trim()?.toDoubleOrNull()
        if (systolic != null && systolic < 90.0) {
            flags.add(SafetyFlag("Systolic BP", "${systolic.toInt()} mmHg", "Hemodynamic instability / hypotension (SBP < 90 mmHg)", SafetyAlertLevel.URGENT))
        }
        input.lactateMmolL.toDoubleOrNull()?.let { lac ->
            when {
                lac >= 4.0 -> flags.add(SafetyFlag("Lactate", "$lac mmol/L", "Severe hyperlactatemia / tissue hypoperfusion (>= 4.0 mmol/L)", SafetyAlertLevel.URGENT))
                lac >= 2.0 -> flags.add(SafetyFlag("Lactate", "$lac mmol/L", "Elevated serum lactate (>= 2.0 mmol/L)", SafetyAlertLevel.CAUTION))
            }
        }

        // 5. Consciousness check
        val neuro = input.consciousnessLevel.lowercase()
        if (listOf("unresponsive", "coma", "obtunded", "stupor", "gcs <", "pain", "altered").any { it in neuro }) {
            flags.add(SafetyFlag("Consciousness", input.consciousnessLevel, "Altered level of consciousness", SafetyAlertLevel.URGENT))
        }

        val overall = when {
            flags.any { it.level == SafetyAlertLevel.URGENT } -> SafetyAlertLevel.URGENT
            flags.any { it.level == SafetyAlertLevel.CAUTION } -> SafetyAlertLevel.CAUTION
            else -> SafetyAlertLevel.NORMAL
        }

        val escalationMsg = when (overall) {
            SafetyAlertLevel.URGENT -> "URGENT CLINICAL ASSESSMENT ADVISED: Initiate immediate bedside evaluation by the responsible clinical team according to Al-Anhar Specialized Hospital emergency procedures. Do not delay urgent care while reviewing reference summaries."
            SafetyAlertLevel.CAUTION -> "CLINICAL CAUTION: Abnormal physiological or respiratory values detected. Correlate immediately at bedside."
            SafetyAlertLevel.NORMAL -> "No immediate critical red-flag threshold detected among entered numeric fields. Always correlate clinically — missing fields are not screened."
        }

        return SafetyScreenResult(overall, flags, escalationMsg)
    }
}
