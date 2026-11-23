package org.mz.data.domain


import org.mz.killrs.shared.domain.UploadFile
import kotlinx.coroutines.flow.Flow
import org.mz.killrs.shared.domain.Product
import org.mz.killrs.shared.util.RequestState



interface AdminRepository {
    fun getCurrentUserId(): String?
    fun isCurrentUserAdmin(): Boolean = false
    suspend fun createNewProduct(
        product: Product,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun uploadImageToStorage(file: UploadFile): String?
    suspend fun deleteImageFromStorage(
        downloadUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    fun readLastTenProducts(): Flow<RequestState<List<Product>>>
    suspend fun readProductById(id: String): RequestState<Product>
    suspend fun updateProductThumbnail(
        productId: String,
        downloadUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun updateProduct(
        product: Product,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    suspend fun deleteProduct(
        productId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    )

    fun searchProductsByTitle(
        searchQuery: String,
    ): Flow<RequestState<List<Product>>>
}