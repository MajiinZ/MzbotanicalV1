package org.mz.data.domain

import org.mz.killrs.shared.domain.UploadFile
import kotlinx.coroutines.flow.Flow
import org.mz.killrs.shared.domain.CartItem
import org.mz.killrs.shared.domain.Customer
import org.mz.killrs.shared.util.RequestState

interface CustomerRepository {
    suspend fun updateProfilePhoto(file: UploadFile?): RequestState<String?>
    fun getCurrentUserId(): String?
    /** Confirms the signed-in user's customer row (created by the database on sign-up). */
    suspend fun createCustomer(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )
    fun readCustomerFlow(): Flow<RequestState<Customer>>
    suspend fun updateCustomer(
        customer: Customer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun addItemToCart(
        cartItem: CartItem,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
        )
    suspend fun updateCartItemQuantity(
        id: String,
        quantity: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun deleteCartItem(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun deleteAllCartItems(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun signOut(): RequestState<Unit>

}
