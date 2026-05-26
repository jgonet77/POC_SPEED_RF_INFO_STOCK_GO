package com.example.stockapp.models

import com.google.gson.annotations.SerializedName


// ===== New Stock Search Models (Phase 7 Part 2) =====

data class StockSearchRequest(
    @SerializedName("art_code")
    val artCode: String?,
    @SerializedName("stk_lieu")
    val stkLieu: String?,
    @SerializedName("stk_nosu")
    val stkNosu: String?,
    @SerializedName("act_code")
    val actCode: String
)


data class StockItem(
    @SerializedName("art_code")
    val artCode: String,
    @SerializedName("stk_lieu")
    val stkLieu: String,
    @SerializedName("stk_nosu")
    val stkNosu: String,
    @SerializedName("qua_code")
    val quaCode: String,
    @SerializedName("stk_qte")
    val stkQte: Int
)


data class StockSearchResponse(
    val status: String,
    val message: String,
    val items: List<StockItem>
)


// ===== Legacy Stock Models (Phase 1) - Kept for backward compatibility =====

/**
 * Response from GET /api/search?sku=...
 *
 * Contains a list of stock entries matching the search criteria.
 */
data class StockResponse(
    val status: String,         // "success" or "error"
    val message: String,
    val data: List<LegacyStockItem>?  // null if error
)

/**
 * Individual legacy stock item in search results.
 */
data class LegacyStockItem(
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
