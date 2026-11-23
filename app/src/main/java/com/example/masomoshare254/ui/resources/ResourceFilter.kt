package com.example.masomoshare254.ui.resources

import com.example.masomoshare254.model.Resource

/** Pure Kotlin search + filter logic (no Android classes, so it can be unit tested). */
object ResourceFilter {

    fun apply(
        resources: List<Resource>,
        query: String,
        subject: String?,
        level: String?,
        type: String?
    ): List<Resource> {
        val words = query.trim().lowercase().split(" ").filter { it.isNotBlank() }

        return resources.filter { r ->
            val matchesFilters =
                (subject == null || r.subject.equals(subject, ignoreCase = true)) &&
                (level == null || r.educationLevel.equals(level, ignoreCase = true)) &&
                (type == null || r.resourceType.equals(type, ignoreCase = true))

            // every typed word must appear in title, description, subject or topic
            val searchable = "${r.title} ${r.description} ${r.subject} ${r.topic}".lowercase()
            matchesFilters && words.all { searchable.contains(it) }
        }
    }

    /** Distinct, sorted, non-blank values for a filter dropdown. */
    fun distinctValues(values: List<String>): List<String> =
        values.map { it.trim() }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
}
