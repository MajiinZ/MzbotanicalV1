package org.mz.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.mz.killrs.shared.Constants.ADMIN_EMAIL
import org.mz.killrs.shared.PhoneNumber
import org.mz.killrs.shared.domain.CartItem
import org.mz.killrs.shared.domain.Customer
import org.mz.killrs.shared.domain.Product

/** Row shapes of the Supabase tables (snake_case columns) and their mapping to app models. */

@Serializable
internal data class ProductRow(
    val id: String,
    val title: String,
    val description: String = "",
    val price: Double = 0.0,
    val thumbnail: String = "",
    val category: String = "",
    val strains: List<String>? = null,
    @SerialName("amount_of_seeds") val amountOfSeeds: Int? = null,
    @SerialName("is_popular") val isPopular: Boolean = false,
    @SerialName("is_new") val isNew: Boolean = false,
    @SerialName("is_discounted") val isDiscounted: Boolean = false,
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("created_at") val createdAt: Long = 0L,
)

internal fun ProductRow.toProduct() = Product(
    id = id, title = title, description = description, price = price, thumbnail = thumbnail,
    category = category.ifBlank { "Sativa" }, strains = strains, amountOfSeeds = amountOfSeeds?.toString(),
    isPopular = isPopular, isNew = isNew, isDiscounted = isDiscounted, stockQuantity = stockQuantity,
    isPublished = isPublished, createdAt = createdAt,
)

internal fun Product.toRow() = ProductRow(
    id = id, title = title.lowercase(), description = description, price = price, thumbnail = thumbnail,
    category = category, strains = strains?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
    amountOfSeeds = amountOfSeeds?.trim()?.toIntOrNull()?.takeIf { it > 0 },
    isPopular = isPopular, isNew = isNew, isDiscounted = isDiscounted, stockQuantity = stockQuantity,
    isPublished = isPublished, createdAt = createdAt,
)

@Serializable
internal data class CustomerRow(
    val id: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val email: String = "",
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    @SerialName("phone_dial_code") val phoneDialCode: Int? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String = "",
    val gender: String = "",
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
)

@Serializable
internal data class CartItemRow(
    val id: String,
    @SerialName("product_id") val productId: String,
    @SerialName("amount_of_seeds") val amountOfSeeds: Int? = null,
    val quantity: Int,
)

internal fun CartItemRow.toCartItem() = CartItem(id = id, productId = productId, amountOfSeeds = amountOfSeeds, quantity = quantity)

internal fun CustomerRow.toCustomer(cart: List<CartItem>, signedInEmail: String?) = Customer(
    id = id, firstName = firstName, lastName = lastName, email = signedInEmail ?: email,
    address = address, city = city, state = state, zip = postalCode,
    phoneNumber = phoneNumber?.let { PhoneNumber(number = it) },
    dateOfBirth = dateOfBirth, gender = gender, cart = cart,
    // Only used to show admin screens; the database enforces admin rights itself.
    isAdmin = signedInEmail.equals(ADMIN_EMAIL, ignoreCase = true),
    profileImageUrl = profileImageUrl,
)
