package org.mz.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mz.data.domain.CustomerRepository
import org.mz.data.supabase.CartItemRow
import org.mz.data.supabase.CustomerRow
import org.mz.data.supabase.DataRefresh
import org.mz.data.supabase.MzSupabase
import org.mz.data.supabase.SupabaseConfig
import org.mz.data.supabase.imageContentType
import org.mz.data.supabase.pollingFlow
import org.mz.data.supabase.readableMessage
import org.mz.data.supabase.toCartItem
import org.mz.data.supabase.toCustomer
import org.mz.killrs.shared.domain.CartItem
import org.mz.killrs.shared.domain.Customer
import org.mz.killrs.shared.domain.UploadFile
import org.mz.killrs.shared.util.RequestState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CustomerRepositoryImpl : CustomerRepository {
    private val db get() = MzSupabase.client

    override fun getCurrentUserId(): String? = MzSupabase.currentUserId()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun updateProfilePhoto(file: UploadFile?): RequestState<String?> {
        return try {
            val userId = getCurrentUserId() ?: error("Please sign in to change your photo.")
            val url = if (file == null) null else withTimeout(30_000) {
                val bytes = file.read()
                val (type, ext) = imageContentType(bytes)
                val bucket = db.storage.from(SupabaseConfig.PROFILE_IMAGES_BUCKET)
                val path = "$userId/${Uuid.random().toHexString()}.$ext"
                bucket.upload(path, bytes) { contentType = ContentType.parse(type) }
                bucket.publicUrl(path)
            }
            check(getCurrentUserId() == userId) { "Your account changed. Please try again." }
            db.from("customers").update(buildJsonObject {
                if (url == null) put("profile_image_url", JsonNull) else put("profile_image_url", url)
            }) { filter { eq("id", userId) } }
            DataRefresh.poke()
            RequestState.Success(url)
        } catch (error: TimeoutCancellationException) {
            RequestState.Error("Photo upload timed out. Please try again.")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            RequestState.Error("Unable to save profile photo: ${error.readableMessage("upload failed")}")
        }
    }

    /** The database creates the customer row when the account is created; this only confirms it exists. */
    override suspend fun createCustomer(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val userId = getCurrentUserId() ?: return onError("User is not available.")
            val row = db.from("customers").select { filter { eq("id", userId) } }.decodeSingleOrNull<CustomerRow>()
            if (row != null) onSuccess() else onError("Your customer profile is not ready yet. Please sign in again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError("Error while loading your profile: ${e.readableMessage("network error")}")
        }
    }

    override fun readCustomerFlow(): Flow<RequestState<Customer>> = pollingFlow(errorPrefix = "Error while reading your profile") {
        val userId = getCurrentUserId() ?: return@pollingFlow RequestState.Error("User is not available.")
        val row = db.from("customers").select { filter { eq("id", userId) } }.decodeSingleOrNull<CustomerRow>()
            ?: return@pollingFlow RequestState.Error("Your customer profile does not exist.")
        val cart = db.from("cart_items").select {
            filter { eq("customer_id", userId) }
            order("created_at", Order.ASCENDING)
        }.decodeList<CartItemRow>().map { it.toCartItem() }
        RequestState.Success(row.toCustomer(cart, db.auth.currentUserOrNull()?.email))
    }

    override suspend fun updateCustomer(customer: Customer, onSuccess: () -> Unit, onError: (String) -> Unit) =
        write(onSuccess, onError, "Error while updating your profile") { userId ->
            db.from("customers").update(buildJsonObject {
                put("first_name", customer.firstName.trim())
                put("last_name", customer.lastName.trim())
                put("city", customer.city)
                put("state", customer.state)
                put("postal_code", customer.zip)
                put("address", customer.address)
                put("phone_number", customer.phoneNumber?.number)
            }) { filter { eq("id", userId) } }
        }

    override suspend fun addItemToCart(cartItem: CartItem, onSuccess: () -> Unit, onError: (String) -> Unit) =
        write(onSuccess, onError, "Could not add to cart") {
            changeCart(productId = cartItem.productId, lineId = null, quantity = cartItem.quantity)
        }

    override suspend fun updateCartItemQuantity(id: String, quantity: Int, onSuccess: () -> Unit, onError: (String) -> Unit) =
        write(onSuccess, onError, "Could not update the cart") {
            changeCart(productId = null, lineId = id, quantity = quantity)
        }

    override suspend fun deleteCartItem(id: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        write(onSuccess, onError, "Could not remove the item") { userId ->
            db.from("cart_items").delete { filter { eq("id", id); eq("customer_id", userId) } }
        }

    override suspend fun deleteAllCartItems(onSuccess: () -> Unit, onError: (String) -> Unit) =
        write(onSuccess, onError, "Could not empty the cart") { userId ->
            db.from("cart_items").delete { filter { eq("customer_id", userId) } }
        }

    override suspend fun signOut(): RequestState<Unit> = try {
        db.auth.stopAutoRefreshForCurrentSession()
        db.auth.signOut()
        RequestState.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Supabase may fail before clearing local state, e.g. when offline.
        try {
            db.auth.clearSession()
            RequestState.Success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            RequestState.Error("Could not sign out on this device. Please try again.")
        }
    }

    private suspend fun changeCart(productId: String?, lineId: String?, quantity: Int) {
        db.postgrest.rpc("change_cart", buildJsonObject {
            put("p_product_id", productId)
            put("p_line_id", lineId)
            put("p_quantity", quantity)
        })
    }

    private suspend fun write(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        errorPrefix: String,
        block: suspend (userId: String) -> Unit,
    ) {
        try {
            val userId = getCurrentUserId() ?: return onError("User is not available.")
            block(userId)
            DataRefresh.poke()
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError("$errorPrefix: ${e.readableMessage("network error")}")
        }
    }
}
