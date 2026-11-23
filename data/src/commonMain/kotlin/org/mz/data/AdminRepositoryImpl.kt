package org.mz.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mz.data.domain.AdminRepository
import org.mz.data.supabase.DataRefresh
import org.mz.data.supabase.MzSupabase
import org.mz.data.supabase.ProductRow
import org.mz.data.supabase.SupabaseConfig
import org.mz.data.supabase.imageContentType
import org.mz.data.supabase.pollingFlow
import org.mz.data.supabase.readableMessage
import org.mz.data.supabase.toProduct
import org.mz.data.supabase.toRow
import org.mz.killrs.shared.Constants.ADMIN_EMAIL
import org.mz.killrs.shared.domain.Product
import org.mz.killrs.shared.domain.UploadFile
import org.mz.killrs.shared.domain.inventoryValidationError
import org.mz.killrs.shared.util.RequestState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Admin screens. The database re-checks admin rights (public.is_admin()) on every write. */
class AdminRepositoryImpl : AdminRepository {
    private val db get() = MzSupabase.client
    private val images get() = db.storage.from(SupabaseConfig.PRODUCT_IMAGES_BUCKET)
    private val publicImagePrefix =
        "${SupabaseConfig.URL}/storage/v1/object/public/${SupabaseConfig.PRODUCT_IMAGES_BUCKET}/"

    override fun getCurrentUserId() = MzSupabase.currentUserId()

    override fun isCurrentUserAdmin(): Boolean = MzSupabase.currentEmail().equals(ADMIN_EMAIL, ignoreCase = true)

    override suspend fun createNewProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) =
        adminWrite(onSuccess, onError, "Error while creating a new product") {
            product.inventoryValidationError()?.let { error(it) }
            db.from("products").insert(product.toRow())
        }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun uploadImageToStorage(file: UploadFile): String? {
        check(getCurrentUserId() != null && isCurrentUserAdmin()) { "Administrator access is required." }
        return try {
            withTimeout(timeMillis = 30_000L) {
                val bytes = file.read()
                val (type, ext) = imageContentType(bytes)
                val path = "${Uuid.random().toHexString()}.$ext"
                images.upload(path, bytes) { contentType = ContentType.parse(type) }
                images.publicUrl(path)
            }
        } catch (e: CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                throw IllegalStateException("The image upload timed out. Check your connection and try a smaller image.", e)
            }
            throw e
        } catch (e: Exception) {
            throw IllegalStateException("Image upload failed: ${e.readableMessage("unknown storage error")}", e)
        }
    }

    override suspend fun deleteImageFromStorage(downloadUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            if (!isCurrentUserAdmin()) return onError("Administrator access is required.")
            // Only images stored in this project's bucket can be removed; old Firebase URLs are left alone.
            if (!downloadUrl.startsWith(publicImagePrefix)) return onSuccess()
            val path = downloadUrl.removePrefix(publicImagePrefix).substringBefore('?')
            images.delete(path)
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError("Error while deleting a thumbnail: ${e.readableMessage("storage error")}")
        }
    }

    override fun readLastTenProducts(): Flow<RequestState<List<Product>>> = pollingFlow(errorPrefix = "Error while reading products") {
        if (getCurrentUserId() == null || !isCurrentUserAdmin()) return@pollingFlow RequestState.Error("User is not available.")
        val rows = db.from("products").select {
            order("created_at", Order.DESCENDING)
            limit(500)
        }.decodeList<ProductRow>()
        RequestState.Success(rows.map { it.toProduct().let { p -> p.copy(title = p.title.uppercase()) } })
    }

    override suspend fun readProductById(id: String): RequestState<Product> = try {
        if (getCurrentUserId() == null || !isCurrentUserAdmin()) RequestState.Error("User is not available.")
        else db.from("products").select { filter { eq("id", id) } }.decodeSingleOrNull<ProductRow>()
            ?.let { row -> RequestState.Success(row.toProduct().let { it.copy(title = it.title.uppercase()) }) }
            ?: RequestState.Error("Selected product not found.")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        RequestState.Error("Error while reading a selected product: ${e.readableMessage("network error")}")
    }

    override suspend fun updateProductThumbnail(productId: String, downloadUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        adminWrite(onSuccess, onError, "Error while updating a thumbnail image") {
            val updated = db.from("products").update(buildJsonObject { put("thumbnail", downloadUrl) }) {
                filter { eq("id", productId) }
                select()
            }.decodeList<ProductRow>()
            check(updated.isNotEmpty()) { "Selected Product not found." }
        }

    override suspend fun updateProduct(product: Product, onSuccess: () -> Unit, onError: (String) -> Unit) =
        adminWrite(onSuccess, onError, "Error while updating the product") {
            product.inventoryValidationError()?.let { error(it) }
            val updated = db.from("products").update(product.toRow()) {
                filter { eq("id", product.id) }
                select()
            }.decodeList<ProductRow>()
            check(updated.isNotEmpty()) { "Selected Product not found." }
        }

    override suspend fun deleteProduct(productId: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
        adminWrite(onSuccess, onError, "Error while deleting the product") {
            val deleted = db.from("products").delete {
                filter { eq("id", productId) }
                select()
            }.decodeList<ProductRow>()
            check(deleted.isNotEmpty()) { "Selected Product not found." }
        }

    override fun searchProductsByTitle(searchQuery: String): Flow<RequestState<List<Product>>> =
        pollingFlow(errorPrefix = "Error while searching products") {
            if (getCurrentUserId() == null || !isCurrentUserAdmin()) return@pollingFlow RequestState.Error("User is not available.")
            val query = searchQuery.trim().lowercase().replace("%", "").replace("_", "")
            val rows = db.from("products").select {
                if (query.isNotEmpty()) filter { ilike("title", "%$query%") }
                order("created_at", Order.DESCENDING)
                limit(500)
            }.decodeList<ProductRow>()
            RequestState.Success(rows.map { it.toProduct().let { p -> p.copy(title = p.title.uppercase()) } })
        }

    private suspend fun adminWrite(onSuccess: () -> Unit, onError: (String) -> Unit, errorPrefix: String, block: suspend () -> Unit) {
        try {
            if (getCurrentUserId() == null || !isCurrentUserAdmin()) return onError("Administrator access is required.")
            block()
            DataRefresh.poke()
            onSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            onError(e.message ?: errorPrefix)
        } catch (e: Exception) {
            onError("$errorPrefix: ${e.readableMessage("network error")}")
        }
    }
}
