package org.mz.killrs

import kotlinx.serialization.json.Json
import org.mz.killrs.shared.domain.*
import kotlin.test.*

class ProductInventoryTest {
    private fun draft() = Product(id = "inventory-test", title = "Collection A", createdAt = 1L,
        description = "", price = 0.0, thumbnail = "", category = "Sativa")
    private fun published() = draft().copy(description = "Sample description", price = 45.0,
        thumbnail = "https://example.com/photo.png", amountOfSeeds = "6", stockQuantity = 10, isPublished = true)

    @Test fun minimalDraftCanBeSavedAndStaysHidden() {
        assertNull(draft().inventoryValidationError())
        assertFalse(draft().isCatalogVisible)
        assertNotNull(draft().copy(title = "  ").inventoryValidationError())
    }
    @Test fun publishingNeedsCompleteProductInformation() {
        assertNull(published().inventoryValidationError())
        listOf(published().copy(description = ""), published().copy(thumbnail = ""),
            published().copy(price = 0.0), published().copy(amountOfSeeds = "0"),
            published().copy(category = "unknown"), published().copy(thumbnail = "http://example.com/a.png"))
            .forEach { assertNotNull(it.inventoryValidationError()) }
    }
    @Test fun invalidStockAndNonfinitePricesAreRejected() {
        assertNotNull(draft().copy(stockQuantity = -1).inventoryValidationError())
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0)
            .forEach { assertNotNull(draft().copy(price = it).inventoryValidationError()) }
        assertNull(published().copy(stockQuantity = 0).inventoryValidationError())
    }
    @Test fun oldRecordsHaveSafeInventoryDefaults() {
        val legacy = """{"id":"old","title":"Legacy","createdAt":1,"description":"Example","price":45.0,"thumbnail":"https://example.com/a.png","category":"Sativa"}"""
        val product = Json.decodeFromString(Product.serializer(), legacy)
        assertFalse(product.isPublished)
        assertEquals(0, product.stockQuantity)
    }
    @Test fun savedProductRetainsStockAndPublishingState() {
        val json = Json { encodeDefaults = true }
        val product = published()
        val restored = json.decodeFromString(Product.serializer(), json.encodeToString(Product.serializer(), product))
        assertEquals(product, restored)
        val reset = restored.copy(isPublished = false, stockQuantity = 0, isNew = false)
        val encoded = json.encodeToString(Product.serializer(), reset)
        assertTrue(encoded.contains("\"isPublished\":false"))
        assertTrue(encoded.contains("\"stockQuantity\":0"))
        assertEquals(reset, json.decodeFromString(Product.serializer(), encoded))
        assertFalse(reset.isCatalogVisible)
    }
    @Test fun packStockIsIndependentOfSeedsPerPack() {
        val p = published().copy(stockQuantity = 4, amountOfSeeds = "12")
        assertEquals(4, p.stockQuantity)
        assertEquals("12", p.amountOfSeeds)
    }
}