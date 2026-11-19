package com.example

import com.example.data.repository.ClinicalSafetyScreener
import com.example.domain.model.ClinicalCaseInput
import com.example.domain.model.SafetyAlertLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalAndSearchUnitTest {

    @Test
    fun clinicalSafetyScreener_detectsUrgentHypoxemiaAndAcidemia() {
        val input = ClinicalCaseInput(
            spo2Percent = "82",
            abgPh = "7.14",
            bloodPressureMmHg = "82/50",
            lactateMmolL = "4.8"
        )
        val result = ClinicalSafetyScreener.screen(input)
        assertEquals(SafetyAlertLevel.URGENT, result.overallLevel)
        assertTrue(result.flags.any { it.parameter == "SpO2" })
        assertTrue(result.flags.any { it.parameter == "Arterial pH" })
        assertTrue(result.flags.any { it.parameter == "Systolic BP" })
        assertTrue(result.flags.any { it.parameter == "Lactate" })
    }

    @Test
    fun clinicalSafetyScreener_detectsCautionForBorderlineTachypneaAndSpo2() {
        val input = ClinicalCaseInput(
            spo2Percent = "90",
            respiratoryRateBpm = "26",
            abgPh = "7.38"
        )
        val result = ClinicalSafetyScreener.screen(input)
        assertEquals(SafetyAlertLevel.CAUTION, result.overallLevel)
        assertEquals(2, result.flags.size)
    }

    @Test
    fun clinicalSafetyScreener_emptyInputReturnsNormalWithNoFlags() {
        val result = ClinicalSafetyScreener.screen(ClinicalCaseInput())
        assertEquals(SafetyAlertLevel.NORMAL, result.overallLevel)
        assertTrue(result.flags.isEmpty())
    }
}
