package org.mz.killrs.manage_product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.storage.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.mz.data.domain.AdminRepository
import org.mz.killrs.shared.domain.Product
import org.mz.killrs.shared.domain.ProductCategory
import org.mz.killrs.shared.domain.inventoryValidationError
import org.mz.killrs.shared.util.RequestState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
data class ManageProductState(
    val id: String = Uuid.random().toHexString(),
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val title: String = "", val description: String = "", val thumbnail: String = "",
    val category: ProductCategory = ProductCategory.Sativa, val strains: String = "",
    val amountOfSeeds: Int? = null, val price: Double = 0.0,
    val isNew: Boolean = false, val isPopular: Boolean = false, val isDiscounted: Boolean = false,
    val stockQuantity: Int? = 0, val isPublished: Boolean = false
) {
    fun toProduct() = Product(id = id, createdAt = createdAt, title = title.trim(), description = description.trim(),
        thumbnail = thumbnail, category = category.name, strains = strains.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        amountOfSeeds = amountOfSeeds?.toString(), price = price, isNew = isNew, isPopular = isPopular,
        isDiscounted = isDiscounted, stockQuantity = stockQuantity ?: -1, isPublished = isPublished)
}

class ManageProductViewModel(
    private val adminRepository: AdminRepository,
    /** Passed explicitly from the Navigation 3 key (Screen.ManageProduct.id); null = new product. */
    productIdArg: String?,
) : ViewModel() {
    private val productId = productIdArg.orEmpty()
    val isEditing get() = productId.isNotEmpty()
    val isAuthorized get() = adminRepository.isCurrentUserAdmin()
    var screenState by mutableStateOf(ManageProductState()); private set
    var priceInput by mutableStateOf(""); private set
    var stockInput by mutableStateOf("0"); private set
    var isLoading by mutableStateOf(isEditing); private set
    var loadError by mutableStateOf<String?>(null); private set
    var isSaving by mutableStateOf(false); private set
    var thumbnailUploaderState: RequestState<Unit> by mutableStateOf(RequestState.Idle); private set
    val validationMessage get() = screenState.toProduct().inventoryValidationError()
    val isFormValid get() = isAuthorized && !isLoading && loadError == null && !isSaving &&
        !thumbnailUploaderState.isLoading() && screenState.toProduct().inventoryValidationError() == null

    init { if (isEditing) loadProduct() }

    fun loadProduct() {
        if (!isAuthorized) { isLoading = false; loadError = "Administrator access is required."; return }
        isLoading = true; loadError = null
        viewModelScope.launch {
            try {
                when (val result = adminRepository.readProductById(productId)) {
                    is RequestState.Success -> {
                        val p = result.data
                        screenState = ManageProductState(id = p.id, createdAt = p.createdAt, title = p.title, description = p.description,
                            thumbnail = p.thumbnail, category = ProductCategory.entries.firstOrNull { it.name == p.category } ?: ProductCategory.Sativa,
                            strains = p.strains.orEmpty().joinToString(", "), amountOfSeeds = p.amountOfSeeds?.toIntOrNull(), price = p.price,
                            isNew = p.isNew, isPopular = p.isPopular, isDiscounted = p.isDiscounted,
                            stockQuantity = p.stockQuantity, isPublished = p.isPublished)
                        priceInput = p.price.toString(); stockInput = p.stockQuantity.toString()
                        thumbnailUploaderState = if (p.thumbnail.isBlank()) RequestState.Idle else RequestState.Success(Unit)
                    }
                    is RequestState.Error -> loadError = result.message
                    else -> loadError = "Could not load this product."
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                loadError = error.message ?: "Could not load this product."
            } finally { isLoading = false }
        }
    }

    fun uploadThumbnailToStorage(file: File?, onSuccess: () -> Unit) {
        if (!isAuthorized || isSaving || isLoading || loadError != null || file == null || thumbnailUploaderState.isLoading()) return
        thumbnailUploaderState = RequestState.Loading
        viewModelScope.launch {
            try {
                val url = adminRepository.uploadImageToStorage(file) ?: error("Upload did not return a photo URL.")
                screenState = screenState.copy(thumbnail = url)
                thumbnailUploaderState = RequestState.Success(Unit); onSuccess()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                thumbnailUploaderState = RequestState.Error(error.message ?: "Photo upload failed.")
            }
        }
    }

    private fun save(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isFormValid) { onError(screenState.toProduct().inventoryValidationError() ?: "Wait for the product to finish loading or saving."); return }
        isSaving = true
        val product = screenState.toProduct()
        viewModelScope.launch {
            try {
                if (isEditing) adminRepository.updateProduct(product, onSuccess, onError)
                else adminRepository.createNewProduct(product, onSuccess, onError)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                onError(error.message ?: "Unable to save product.")
            } finally { isSaving = false }
        }
    }
    fun createNewProduct(onSuccess: () -> Unit, onError: (String) -> Unit) = save(onSuccess, onError)
    fun updateProduct(onSuccess: () -> Unit, onError: (String) -> Unit) = save(onSuccess, onError)

    fun deleteThumbnailFromStorage(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isAuthorized || isSaving) { onError("Administrator access is required."); return }
        // Detach on save; do not delete a shared or still-published image from storage.
        screenState = screenState.copy(thumbnail = "")
        thumbnailUploaderState = RequestState.Idle; onSuccess()
    }
    fun deleteProduct(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isEditing || !isAuthorized || isSaving || isLoading || loadError != null) return
        isSaving = true
        viewModelScope.launch {
            try { adminRepository.deleteProduct(productId, onSuccess, onError) }
            finally { isSaving = false }
        }
    }
    fun updateTitle(value: String) { screenState = screenState.copy(title = value) }
    fun updateDescription(value: String) { screenState = screenState.copy(description = value) }
    fun updateThumbnailUrl(value: String) {
        screenState = screenState.copy(thumbnail = value.trim())
        thumbnailUploaderState = if (value.isBlank()) RequestState.Idle else RequestState.Success(Unit)
    }
    fun updateCategory(value: ProductCategory) { screenState = screenState.copy(category = value) }
    fun updateStrains(value: String) { screenState = screenState.copy(strains = value) }
    fun updateAmountOfSeeds(value: Int?) { screenState = screenState.copy(amountOfSeeds = value) }
    fun updatePriceInput(value: String) { priceInput = value; screenState = screenState.copy(price = value.toDoubleOrNull() ?: if (value.isBlank()) 0.0 else Double.NaN) }
    fun updateStockInput(value: String) { stockInput = value; screenState = screenState.copy(stockQuantity = value.toIntOrNull()) }
    fun updatePublished(value: Boolean) { screenState = screenState.copy(isPublished = value) }
    fun updateNew(value: Boolean) { screenState = screenState.copy(isNew = value) }
    fun updatePopular(value: Boolean) { screenState = screenState.copy(isPopular = value) }
    fun updateDiscounted(value: Boolean) { screenState = screenState.copy(isDiscounted = value) }
}