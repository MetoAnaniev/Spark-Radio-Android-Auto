package com.sparklab.radio

import com.sparklab.radio.data.remote.DefaultStations
import com.sparklab.radio.data.repository.distinctStationEntries
import com.sparklab.radio.domain.model.Station
import com.sparklab.radio.domain.usecase.ValidateStreamUrl
import com.sparklab.radio.ui.viewmodel.StationFormFactory
import com.sparklab.radio.ui.viewmodel.StationFormState
import org.junit.Assert.*
import org.junit.Test

class CatalogAndEditingTest {
    private val manual = Station("user_1", "My Radio", "https://example.com/live", country = "Bulgaria", source = Station.Source.USER)

    @Test
    fun `editing retains the existing id instead of adding another station`() {
        val form = StationFormState.from(manual).copy(name = "Renamed", streamUrl = "https://example.com/new")
        val edited = StationFormFactory.validation(form).toStation()
        assertEquals(manual.id, edited.id)
        assertEquals("Renamed", edited.name)
        assertEquals("https://example.com/new", edited.streamUrl)
        assertTrue(edited.isUserEditable)
    }

    @Test
    fun `only manually added stations are editable`() {
        assertTrue(manual.isUserEditable)
        assertFalse(manual.copy(source = Station.Source.REMOTE).isUserEditable)
        assertFalse(manual.copy(source = Station.Source.STATIC).isUserEditable)
    }

    @Test
    fun `manual entries with matching names stay visible`() {
        val second = manual.copy(id = "user_2", streamUrl = "https://example.com/second")
        val remote = manual.copy(id = "remote", source = Station.Source.REMOTE)
        assertEquals(listOf(manual, second), distinctStationEntries(listOf(remote, manual, second)))
    }

    @Test
    fun `directory variants are collapsed case insensitively`() {
        val remote = manual.copy(id = "remote", source = Station.Source.REMOTE)
        val variant = remote.copy(id = "variant", name = " MY RADIO ", country = "bulgaria")
        assertEquals(listOf(remote), distinctStationEntries(listOf(remote, variant)))
    }

    @Test
    fun `built in favorite stays visible when same directory station loads`() {
        val fallback = manual.copy(id = "static", source = Station.Source.STATIC)
        val remote = fallback.copy(id = "remote", source = Station.Source.REMOTE)
        assertEquals(listOf(fallback), distinctStationEntries(listOf(remote, fallback), setOf(fallback.id)))
    }

    @Test
    fun `expanded built in catalog has unique valid stream entries`() {
        val stations = DefaultStations.all
        assertTrue(stations.size >= 10)
        assertEquals(stations.size, stations.map { it.id }.distinct().size)
        stations.forEach { assertNull(it.name, ValidateStreamUrl(it.streamUrl)) }
    }

    @Test
    fun `invalid edit cannot be saved as a valid form`() {
        val form = StationFormFactory.validation(StationFormState.from(manual).copy(streamUrl = "ftp://example.com/live"))
        assertFalse(form.isValid)
        assertNotNull(form.urlError)
    }
}
