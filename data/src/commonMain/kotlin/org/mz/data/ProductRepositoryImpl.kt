package org.mz.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import org.mz.data.domain.ProductRepository
import org.mz.killrs.shared.domain.Product
import org.mz.killrs.shared.domain.ProductCategory
import org.mz.killrs.shared.domain.isCatalogVisible
import org.mz.killrs.shared.util.RequestState

class ProductRepositoryImpl : ProductRepository {
    override fun getCurrentUserId(): String? = Firebase.auth.currentUser?.uid

    override fun readAllProducts(): Flow<RequestState<List<Product>>> = flow<RequestState<List<Product>>> {
        check(getCurrentUserId() != null) { "Sign in to browse products." }
        Firebase.firestore.collection("product").where { "isPublished" equalTo true }.snapshots.collect { snapshot ->
            emit(RequestState.Success(snapshot.documents.map { it.toProduct() }
                .filter { it.isCatalogVisible }.sortedByDescending { it.createdAt }
                .map { it.copy(title = it.title.uppercase()) }))
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(RequestState.Error("Unable to load products: ${error.message}"))
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

    override fun readProductByIdFlow(id: String): Flow<RequestState<Product>> = flow<RequestState<Product>> {
        check(getCurrentUserId() != null) { "Sign in to view products." }
        Firebase.firestore.collection("product").document(id).snapshots.collect { document ->
            if (!document.exists) emit(RequestState.Error("This product is no longer available."))
            else {
                val product = document.toProduct()
                if (!product.isCatalogVisible) emit(RequestState.Error("This product is not currently published."))
                else emit(RequestState.Success(product.copy(title = product.title.uppercase())))
            }
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(RequestState.Error("Unable to load this product: ${error.message}"))
    }
}