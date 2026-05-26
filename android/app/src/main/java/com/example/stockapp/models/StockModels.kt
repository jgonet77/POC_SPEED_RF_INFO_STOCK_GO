package com.example.stockapp.models

/**
 * Response from GET /api/search?sku=...
 *
 * Contains a list of stock entries matching the search criteria.
 */
data class StockResponse(
    val status: String,         // "success" or "error"
    val message: String,
    val data: List<StockItem>?  // null if error
)

/**
 * Individual stock item in search results.
 */
data class StockItem(
    val sku: String,
    val description: String,
    val quantity: Int,
    val location: String,
    val unit: String
)

/**
 * Response from GET /api/details?sku=...
 *
 * Contains detailed information for a specific SKU.
 */
data class StockDetailsResponse(
    val status: String,         // "success" or "error"
    val message: String,
    val data: StockDetails?     // null if error
)

/**
 * Detailed stock information for a specific SKU.
 */
data class StockDetails(
    val sku: String,
    val description: String,
    val quantity: Int,
    val location: String,
    val unit: String,
    val batch: String?,
    val expiration_date: String?,
    val condition: String?,
    val last_update: String?
)
