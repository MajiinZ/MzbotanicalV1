package org.mz.killrs.shared.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackStackExtTest {
    private fun stack(vararg keys: NavKey): MutableList<NavKey> = mutableListOf(*keys)

    @Test
    fun navigateUpNeverEmptiesTheStack() {
        val s = stack(Screen.AgeGate)
        s.navigateUp()
        assertEquals(listOf<NavKey>(Screen.AgeGate), s)
    }

    @Test
    fun replaceAllLeavesOnlyTheNewKey() {
        val s = stack(Screen.AgeGate)
        s.replaceAll(Screen.Auth)
        assertEquals(listOf<NavKey>(Screen.Auth), s)
        s.add(Screen.Profile)
        s.replaceAll(Screen.HomeGraph)
        assertEquals(listOf<NavKey>(Screen.HomeGraph), s)
    }

    @Test
    fun navigateSingleTopDoesNotDuplicate() {
        val s = stack(Screen.ProductsOverview)
        s.navigateSingleTop(Screen.Cart)
        s.navigateSingleTop(Screen.Cart)
        assertEquals(listOf<NavKey>(Screen.ProductsOverview, Screen.Cart), s)
    }

    @Test
    fun switchSectionSavesAndRestoresSectionHistory() {
        val saved = mutableMapOf<NavKey, List<NavKey>>()
        val root = Screen.ProductsOverview
        val s = stack(root)

        s.switchSection(Screen.Categories, root, saved)
        s.add(Screen.CategorySearch("Hybrid"))
        assertEquals(listOf<NavKey>(root, Screen.Categories, Screen.CategorySearch("Hybrid")), s)

        s.switchSection(Screen.Cart, root, saved)
        assertEquals(listOf<NavKey>(root, Screen.Cart), s)

        s.switchSection(Screen.Categories, root, saved)
        assertEquals(listOf<NavKey>(root, Screen.Categories, Screen.CategorySearch("Hybrid")), s)

        s.switchSection(root, root, saved)
        assertEquals(listOf<NavKey>(root), s)
    }

    @Test
    fun reselectingCurrentSectionIsANoOp() {
        val saved = mutableMapOf<NavKey, List<NavKey>>()
        val s = stack(Screen.ProductsOverview, Screen.Categories, Screen.CategorySearch("Indica"))
        s.switchSection(Screen.Categories, Screen.ProductsOverview, saved)
        assertEquals(3, s.size)
        assertTrue(saved.isEmpty())
    }
}
