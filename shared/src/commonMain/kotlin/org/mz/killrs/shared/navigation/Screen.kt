package org.mz.killrs.shared.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

@Serializable
sealed class Screen : NavKey {
    /** 18+ gate. Always the first destination of a fresh session. */
    @Serializable
    data object AgeGate : Screen()

    @Serializable
    data object Auth : Screen()

    @Serializable
    data object HomeGraph : Screen()

    @Serializable
    data object ProductsOverview : Screen()

    @Serializable
    data object Cart : Screen()

    @Serializable
    data object Profile : Screen()

    @Serializable
    data object Categories : Screen()

    @Serializable
    data object Orders : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object Admin : Screen()

    // 🔹 Routes with arguments
    @Serializable
    data class CategorySearch(
        val category: String? = null
    ) : Screen()

    @Serializable
    data class ManageProduct(
        val id: String? = null,
    ) : Screen()

    @Serializable
    data class Details(
        val id: String
    ) : Screen()

    @Serializable
    data class Checkout(
        val totalAmount: String
    ) : Screen()

    @Serializable
    data class PaymentCompleted(
        val isSuccess: Boolean? = null,
        val error: String? = null,
        val token: String? = null,
    ) : Screen()
}

/**
 * Navigation 3 saves back stacks with kotlinx.serialization. Registering every [Screen]
 * subtype polymorphically lets `rememberNavBackStack` survive configuration changes and
 * process death on every target (including the non-JVM targets planned for iOS/web).
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
val ScreenSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclassesOfSealed<Screen>()
        }
    }
}
