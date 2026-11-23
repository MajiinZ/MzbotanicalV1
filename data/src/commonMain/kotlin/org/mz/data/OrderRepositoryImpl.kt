package org.mz.data

import org.mz.data.domain.CustomerRepository
import org.mz.data.domain.OrderRepository
import org.mz.data.supabase.MzSupabase
import org.mz.killrs.shared.domain.Order

/**
 * Checkout is disabled for now. Clients are not allowed to insert orders in Supabase
 * (row-level security); orders must be created by a trusted server once payments exist.
 */
class OrderRepositoryImpl(
    @Suppress("unused") private val customerRepository: CustomerRepository,
) : OrderRepository {
    override fun getCurrentUserId() = MzSupabase.currentUserId()

    override suspend fun createTheOrder(
        order: Order,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!org.mz.killrs.shared.Constants.CHECKOUT_ENABLED) {
            onError("Checkout is currently disabled."); return
        }
        onError("Online checkout is not available in this version of the app.")
    }
}
