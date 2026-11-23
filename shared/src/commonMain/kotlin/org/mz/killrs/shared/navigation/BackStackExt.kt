package org.mz.killrs.shared.navigation

import androidx.navigation3.runtime.NavKey

/*
 * Small helpers that reproduce the Navigation 2 operations this app relied on
 * (navigateUp, popUpTo { inclusive = true }, launchSingleTop) on top of a plain
 * Navigation 3 back stack, which is just a MutableList<NavKey>.
 */

/** Equivalent of `navController.navigateUp()`: pop unless we are at the root. */
fun MutableList<NavKey>.navigateUp() {
    if (size > 1) removeAt(lastIndex)
}

/** Push [key] unless it is already on top (Navigation 2 `launchSingleTop`). */
fun MutableList<NavKey>.navigateSingleTop(key: NavKey) {
    if (lastOrNull() != key) add(key)
}

/**
 * Make [key] the only entry, e.g. after the age gate or on sign-out, so the user can't
 * go back to the previous flow. The new key is added before the old entries are removed
 * so the back stack is never empty (NavDisplay requires at least one entry).
 */
fun MutableList<NavKey>.replaceAll(key: NavKey) {
    add(key)
    if (size > 1) subList(0, size - 1).clear()
}

/**
 * Switch the top-level section of a stack shaped `[root, section, ...sub-screens]`,
 * reproducing Navigation 2's `popUpTo(root) { saveState = true }; restoreState = true;
 * launchSingleTop = true` used by the storefront bottom bar and drawer.
 *
 * The current section's sub-stack is stored in [savedSections] and restored when the
 * user returns to it. Index 0 ([root]) is never removed, so the stack is never empty.
 */
fun MutableList<NavKey>.switchSection(
    destination: NavKey,
    root: NavKey,
    savedSections: MutableMap<NavKey, List<NavKey>>,
) {
    val currentSection = getOrNull(1) ?: root
    if (destination == currentSection) return
    if (size > 1) savedSections[currentSection] = subList(1, size).toList()
    val restored = if (destination == root) emptyList()
    else savedSections.remove(destination) ?: listOf(destination)
    subList(1, size).clear()
    addAll(restored)
}
