package com.sparklab.radio

import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.usecase.stationSwitchQueue
import org.junit.Assert.assertEquals
import org.junit.Test

class StationSwitchQueueTest {
    private val a = Station("a", "Alpha", "https://example.com/a")
    private val b = Station("b", "Beta", "https://example.com/b")
    private val c = Station("c", "Custom", "https://example.com/c")

    @Test
    fun `cold resume retains saved station order and selected index`() {
        val queue = stationSwitchQueue(a, listOf(b, a, c), emptyList())
        assertEquals(listOf(b, a, c), queue)
        assertEquals(1, queue.indexOfFirst { it.id == a.id })
    }

    @Test
    fun `first resume after upgrade builds a switchable queue`() {
        assertEquals(listOf(a, b), stationSwitchQueue(b, emptyList(), listOf(a, b)))
    }

    @Test
    fun `custom station missing from catalog is retained`() {
        assertEquals(listOf(a, b, c), stationSwitchQueue(c, emptyList(), listOf(a, b)))
    }

    @Test
    fun `selected stream snapshot replaces stale saved url`() {
        val updated = a.copy(streamUrl = "https://example.com/new")
        assertEquals(listOf(updated, b), stationSwitchQueue(updated, listOf(a, b), emptyList()))
    }

    @Test
    fun `queue does not duplicate selected station`() {
        assertEquals(listOf(a, b), stationSwitchQueue(a, listOf(a, a, b), listOf(c)))
    }

    @Test
    fun `saved favorites queue is not expanded to full catalog`() {
        assertEquals(listOf(b, a), stationSwitchQueue(a, listOf(b, a), listOf(a, b, c)))
    }

    @Test
    fun `invalid saved entries are ignored`() {
        assertEquals(listOf(a), stationSwitchQueue(a, listOf(b.copy(streamUrl = ""), a), emptyList()))
    }
}
