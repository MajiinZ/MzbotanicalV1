package org.mz.data

import dev.gitlive.firebase.firestore.DocumentSnapshot
import org.mz.killrs.shared.domain.Product

/** Missing inventory metadata intentionally leaves old records as drafts. */
internal fun DocumentSnapshot.toProduct(): Product = Product(
    id = id,
    title = get<String?>("title") ?: "",
    description = get<String?>("description") ?: "",
    thumbnail = get<String?>("thumbnail") ?: "",
    category = get<String?>("category") ?: "Sativa",
    strains = get<List<String>?>("strains"),
    amountOfSeeds = get<String?>("amountOfSeeds"),
    price = get<Double?>("price") ?: 0.0,
    isPopular = get<Boolean?>("isPopular") ?: false,
    isNew = get<Boolean?>("isNew") ?: false,
    isDiscounted = get<Boolean?>("isDiscounted") ?: false,
    createdAt = get<Long?>("createdAt") ?: 0L,
    stockQuantity = get<Int?>("stockQuantity") ?: 0,
    isPublished = get<Boolean?>("isPublished") ?: false
)