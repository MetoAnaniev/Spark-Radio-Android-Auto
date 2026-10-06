package com.sparklab.radio.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sparklab.radio.domain.model.Genre
import com.sparklab.radio.ui.viewmodel.RadioViewModel
import com.sparklab.radio.ui.viewmodel.StationFormFactory
import com.sparklab.radio.ui.viewmodel.StationFormState

/**
 * Add / Edit a custom station.
 * Validates the stream URL (http/https + host) and persists to Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStationScreen(vm: RadioViewModel, stationId: String?, onBack: () -> Unit) {
    val userStations by vm.userStations.collectAsState()
    val editing = stationId?.let { id -> userStations.firstOrNull { it.id == id } }

    var form by remember { mutableStateOf(StationFormState()) }
    var genreExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Prefill when editing an existing user station.
    LaunchedEffect(editing?.id) {
        if (editing != null) form = StationFormState.from(editing)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                if (editing == null) "Add station" else "Edit station",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = form.name,
            onValueChange = { form = form.copy(name = it, nameError = null) },
            label = { Text("Station name") },
            isError = form.nameError != null,
            supportingText = form.nameError?.let { { Text(it) } },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.streamUrl,
            onValueChange = { form = form.copy(streamUrl = it, urlError = null) },
            label = { Text("Stream URL") },
            placeholder = { Text("https://stream.example.com/live") },
            isError = form.urlError != null,
            supportingText = { Text(form.urlError ?: "http(s), .m3u / .m3u8 / .pls supported") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.logoUrl,
            onValueChange = { form = form.copy(logoUrl = it) },
            label = { Text("Logo URL (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        // Genre dropdown
        ExposedDropdownMenuBox(
            expanded = genreExpanded,
            onExpandedChange = { genreExpanded = it },
        ) {
            OutlinedTextField(
                value = form.genre.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Genre") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = genreExpanded, onDismissRequest = { genreExpanded = false }) {
                Genre.entries.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g.label) },
                        onClick = { form = form.copy(genre = g); genreExpanded = false },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.country,
            onValueChange = { form = form.copy(country = it) },
            label = { Text("Language / Country") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = form.description,
            onValueChange = { form = form.copy(description = it) },
            label = { Text("Short description") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val validated = StationFormFactory.validation(form)
                    form = validated
                    if (validated.isValid) {
                        vm.saveStation(validated.toStation()) { onBack() }
                    }
                },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            ) { Text("Save") }

            if (editing != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showDeleteConfirmation && editing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete station?") },
            text = { Text("${editing.name} will be permanently removed from My Stations.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        vm.deleteStation(editing.id)
                        onBack()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
