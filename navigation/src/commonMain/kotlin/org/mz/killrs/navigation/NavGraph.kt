package org.mz.killrs.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.mz.admin.AdminPanelScreen
import org.mz.home.HomeGraphScreen
import org.mz.killrs.CheckoutScreen
import org.mz.killrs.DetailsScreen
import org.mz.killrs.PaymentCompleted
import org.mz.killrs.auth.AgeGateScreen
import org.mz.killrs.auth.AuthenticationScreen
import org.mz.killrs.category_search.CategorySearchScreen
import org.mz.killrs.manage_product.ManageProductScreen
import org.mz.killrs.profile.ProfileScreen
import org.mz.killrs.shared.domain.ProductCategory
import org.mz.killrs.shared.navigation.Screen
import org.mz.killrs.shared.navigation.ScreenSavedStateConfiguration
import org.mz.killrs.shared.navigation.navigateUp
import org.mz.killrs.shared.navigation.replaceAll

/**
 * Root navigation, built on Navigation 3.
 *
 * The back stack is a plain, saveable list of [Screen] keys that we own and mutate
 * directly; [NavDisplay] renders whatever is on top. Flow of a fresh session:
 *
 *   AgeGate -> Auth -> HomeGraph (storefront, with its own nested NavDisplay) -> ...
 *
 * Each transition between those three replaces the whole stack, so Back can never
 * return to (or skip past) the age gate or sign-in.
 */
@Composable
fun SetupNavGraph(
    startDestination: Screen = Screen.AgeGate
) {
    val backStack = rememberNavBackStack(ScreenSavedStateConfiguration, startDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.navigateUp() },
        entryDecorators = listOf(
            // Keeps rememberSaveable state per entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes koinViewModel() to each entry and clears it when the entry is popped.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            // 🔹 Age gate (18+). Replaces itself with sign-in once verified.
            entry<Screen.AgeGate> {
                AgeGateScreen(
                    onVerified = { backStack.replaceAll(Screen.Auth) }
                )
            }

            // 🔹 Authentication
            entry<Screen.Auth> {
                AuthenticationScreen(
                    navigateToHome = { backStack.replaceAll(Screen.HomeGraph) },
                )
            }

            // 🔹 Main app / storefront (with nested bottom-bar NavDisplay inside)
            entry<Screen.HomeGraph> {
                HomeGraphScreen(
                    // Signing out starts a fresh session, which begins at the age gate
                    // (same behavior as before, when the gate lived inside AuthScreen).
                    navigateToAuth = { backStack.replaceAll(Screen.AgeGate) },
                    navigateToProfile = { backStack.add(Screen.Profile) },
                    navigateToAdmin = { backStack.add(Screen.Admin) },
                    navigateToDetails = { productId -> backStack.add(Screen.Details(id = productId)) },
                    navigateToCategorySearch = { categoryName ->
                        backStack.add(Screen.CategorySearch(categoryName))
                    },
                    navigateToCart = { backStack.add(Screen.Cart) },
                    navigateToCheckout = { totalAmount -> backStack.add(Screen.Checkout(totalAmount)) }
                )
            }

            // 🔹 Profile
            entry<Screen.Profile> {
                ProfileScreen(
                    navigateBack = { backStack.navigateUp() }
                )
            }

            // 🔹 Admin
            entry<Screen.Admin> {
                AdminPanelScreen(
                    navigateBack = { backStack.navigateUp() },
                    navigateToManageProduct = { id -> backStack.add(Screen.ManageProduct(id = id)) }
                )
            }

            // 🔹 Manage product
            entry<Screen.ManageProduct> { key ->
                ManageProductScreen(
                    navigateBack = { backStack.navigateUp() },
                    productId = key.id
                )
            }

            entry<Screen.Checkout> { key ->
                CheckoutScreen(
                    totalAmount = key.totalAmount.toDoubleOrNull() ?: 0.0,
                    navigateBack = { backStack.navigateUp() },
                    navigateToPaymentCompleted = { isSuccess, error ->
                        backStack.add(Screen.PaymentCompleted(isSuccess = isSuccess, error = error))
                    }
                )
            }

            entry<Screen.Details> { key ->
                DetailsScreen(
                    productId = key.id,
                    navigateBack = { backStack.navigateUp() },
                    navigateToCart = { backStack.navigateUp() }
                )
            }

            entry<Screen.CategorySearch> { key ->
                val category = key.category?.let { ProductCategory.valueOf(it) }
                if (category != null) {
                    CategorySearchScreen(
                        category = category,
                        navigateToDetails = { id -> backStack.add(Screen.Details(id)) },
                        navigateBack = { backStack.navigateUp() }
                    )
                }
            }

            entry<Screen.PaymentCompleted> { key ->
                PaymentCompleted(
                    isSuccess = key.isSuccess,
                    error = key.error,
                    // Back to the storefront with the back stack cleared completely.
                    navigateBack = { backStack.replaceAll(Screen.HomeGraph) }
                )
            }
        }
    )
}
