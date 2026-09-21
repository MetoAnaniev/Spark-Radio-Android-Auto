package com.sparklab.radio.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Fixed set of genres used to organize stations and to build the "Explore" grid.
 *
 * [label] is shown to the user, [key] is the stable id used for filtering and for
 * the Android Auto browse tree. Add new genres here and they automatically appear
 * in Explore (as long as a matching Material icon is provided).
 */
enum class Genre(val key: String, val label: String) {
    POP("pop", "Pop"),
    ROCK("rock", "Rock"),
    JAZZ("jazz", "Jazz"),
    CLASSICAL("classical", "Classical"),
    NEWS_TALK("news_talk", "News / Talk"),
    ELECTRONIC("electronic", "Electronic"),
    CHILL("chill", "Chill"),
    BULGARIAN("bulgarian", "Bulgarian"),
    GREEK("greek", "Greek"),
    KIDS("kids", "Kids"),
    OTHER("other", "Other");

    /** Icon used in the Explore grid & elsewhere. */
    val icon: ImageVector
        get() = when (this) {
            POP -> Icons.Filled.LibraryMusic
            ROCK -> Icons.AutoMirrored.Filled.QueueMusic
            JAZZ -> Icons.Filled.Piano
            CLASSICAL -> Icons.Filled.Podcasts
            NEWS_TALK -> Icons.Filled.Newspaper
            ELECTRONIC -> Icons.Filled.GraphicEq
            CHILL -> Icons.Filled.SelfImprovement
            BULGARIAN -> Icons.Filled.Radio
            GREEK -> Icons.Filled.Radio
            KIDS -> Icons.Filled.ChildCare
            OTHER -> Icons.Filled.Radio
        }

    companion object {
        /** Resolve a genre from a stored/lower-cased key, defaulting to [OTHER]. */
        fun fromKey(key: String?): Genre =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER
    }
}
