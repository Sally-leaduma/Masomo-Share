package com.example.masomoshare254.ui.resources

import com.example.masomoshare254.model.Resource
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceFilterTest {

    private val data = listOf(
        Resource("1", "KCSE Maths Past Paper 2022", "Form 4 mathematics", "Mathematics", "Secondary", "Algebra", "Past Paper"),
        Resource("2", "Biology Revision Notes", "Cell biology summary", "Biology", "Secondary", "Cells", "Notes"),
        Resource("3", "Intro to Databases", "SQL basics", "Computer Science", "University", "SQL", "PDF")
    )

    @Test fun emptyQueryAndNoFilters_returnsEverything() {
        assertEquals(3, ResourceFilter.apply(data, "", null, null, null).size)
    }

    @Test fun search_isCaseInsensitiveAndMatchesTitle() {
        val result = ResourceFilter.apply(data, "BIOLOGY", null, null, null)
        assertEquals(listOf("2"), result.map { it.resourceId })
    }

    @Test fun search_matchesTopic() {
        val result = ResourceFilter.apply(data, "sql", null, null, null)
        assertEquals(listOf("3"), result.map { it.resourceId })
    }

    @Test fun search_requiresAllWords() {
        assertEquals(1, ResourceFilter.apply(data, "maths past", null, null, null).size)
        assertEquals(0, ResourceFilter.apply(data, "maths notes", null, null, null).size)
    }

    @Test fun filters_combineWithSearch() {
        val result = ResourceFilter.apply(data, "", null, "Secondary", null)
        assertEquals(2, result.size)
        val narrowed = ResourceFilter.apply(data, "", "Biology", "Secondary", "Notes")
        assertEquals(listOf("2"), narrowed.map { it.resourceId })
    }

    @Test fun noMatch_returnsEmptyList() {
        assertEquals(0, ResourceFilter.apply(data, "chemistry", null, null, null).size)
    }

    @Test fun distinctValues_removesBlanksAndDuplicates() {
        val values = ResourceFilter.distinctValues(listOf("Notes", "notes", " ", "PDF", ""))
        assertEquals(listOf("Notes", "PDF"), values)
    }

    @Test fun whitespaceOnlyQuery_returnsEverything() {
        assertEquals(3, ResourceFilter.apply(data, "   \t  ", null, null, null).size)
    }

    @Test fun extraSpacesTabsAndNewlines_areIgnored() {
        val result = ResourceFilter.apply(data, "  maths \t  past\n", null, null, null)
        assertEquals(listOf("1"), result.map { it.resourceId })
    }

    @Test fun nonBreakingSpace_isTreatedAsSpace() {
        val result = ResourceFilter.apply(data, "maths\u00A0past", null, null, null)
        assertEquals(listOf("1"), result.map { it.resourceId })
    }

    @Test fun filters_ignoreSurroundingSpacesInStoredValues() {
        val padded = listOf(data[1].copy(subject = " Biology "))
        assertEquals(1, ResourceFilter.apply(padded, "", "Biology", null, null).size)
    }
}
