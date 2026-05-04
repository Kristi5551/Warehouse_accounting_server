package com.example.warehouse_accounting_server.util

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportDateBoundsTest {

    @Test
    fun `single calendar day is fully included via exclusive next midnight`() {
        val d = LocalDate.of(2026, 5, 1)
        val b = ReportDateBounds.from(d, d)
        assertEquals(LocalDateTime.of(2026, 5, 1, 0, 0, 0), b.fromInclusive)
        assertEquals(LocalDateTime.of(2026, 5, 2, 0, 0, 0), b.toExclusive)
    }

    @Test
    fun `open-ended from only`() {
        val b = ReportDateBounds.from(LocalDate.of(2026, 1, 10), null)
        assertEquals(LocalDateTime.of(2026, 1, 10, 0, 0, 0), b.fromInclusive)
        assertNull(b.toExclusive)
    }

    @Test
    fun `open-ended to only`() {
        val b = ReportDateBounds.from(null, LocalDate.of(2026, 3, 20))
        assertNull(b.fromInclusive)
        assertEquals(LocalDateTime.of(2026, 3, 21, 0, 0, 0), b.toExclusive)
    }
}
