package org.mz.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mz.data.domain.ProductRepository
import org.mz.data.supabase.MzSupabase
import org.mz.data.supabase.ProductRow
import org.mz.data.supabase.pollingFlow
import org.mz.data.supabase.toProduct
import org.mz.killrs.shared.domain.Product
import org.mz.killrs.shared.domain.ProductCategory
import org.mz.killrs.shared.domain.isCatalogVisible
import org.mz.killrs.shared.util.RequestState

class ProductRepositoryImpl : ProductRepository {
    private val db get() = MzSupabase.client

    override fun getCurrentUserId(): String? = MzSupabase.currentUserId()

    override fun readAllProducts(): Flow<RequestState<List<Product>>> = pollingFlow(errorPrefix = "Unable to load products") {
        if (getCurrentUserId() == null) return@pollingFlow RequestState.Error("Sign in to browse products.")
        val rows = db.from("products").select {
            filter { eq("is_published", true) }
            order("created_at", Order.DESCENDING)
        }.decodeList<ProductRow>()
        RequestState.Success(rows.map { it.toProduct() }.filter { it.isCatalogVisible }
            .map { it.copy(title = it.title.uppercase()) })
    }

    private fun filtered(predicate: (Product) -> Boolean): Flow<RequestState<List<Product>>> = readAllProducts().map { state ->
        when (state) {
            is RequestState.Success -> RequestState.Success(state.data.filter(predicate))
            else -> state
        }
    }
    override fun readNewAndDiscountedProducts() = filtered { it.isDiscounted }
    override fun readNewProducts() = filtered { it.isNew }
    override fun readProductsByCategoryFlow(category: ProductCategory) = filtered { it.category == category.name }
    override fun readProductsByIdsFlow(ids: List<String>) = filtered { it.id in ids }

    override fun readProductByIdFlow(id: String): Flow<RequestState<Product>> = pollingFlow(errorPrefix = "Unable to load this product") {
        if (getCurrentUserId() == null) return@pollingFlow RequestState.Error("Sign in to view products.")
        val row = db.from("products").select { filter { eq("id", id) } }.decodeSingleOrNull<ProductRow>()
        when {
            row == null -> RequestState.Error("This product is no longer available.")
            !row.isPublished -> RequestState.Error("This product is not currently published.")
            else -> RequestState.Success(row.toProduct().let { it.copy(title = it.title.uppercase()) })
        }
    }
}
