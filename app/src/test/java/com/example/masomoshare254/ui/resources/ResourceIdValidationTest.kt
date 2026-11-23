package com.example.masomoshare254.ui.resources

import com.example.masomoshare254.data.ResourceCatalogueRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceIdValidationTest {
    @Test fun normalId_isValid() = assertTrue(ResourceCatalogueRepository.isValidId("abc123"))
    @Test fun emptyOrBlank_isInvalid() {
        assertFalse(ResourceCatalogueRepository.isValidId(""))
        assertFalse(ResourceCatalogueRepository.isValidId("   "))
        assertFalse(ResourceCatalogueRepository.isValidId(null))
    }
    @Test fun idWithSlash_isInvalid() = assertFalse(ResourceCatalogueRepository.isValidId("a/b"))
}
