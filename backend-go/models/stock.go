package models

// StockSearchRequest is the request body for the stock search endpoint.
// ArtCode, StkLieu and StkNosu are optional search criteria (at least one
// must be provided); ActCode is the required activity filter.
type StockSearchRequest struct {
	ArtCode *string `json:"art_code,omitempty"`
	StkLieu *string `json:"stk_lieu,omitempty"`
	StkNosu *string `json:"stk_nosu,omitempty"`
	ActCode string  `json:"act_code"` // Activity code (required, filtered from Android)
}

// StockItem is a single stock entry in search results.
type StockItem struct {
	ArtCode string `json:"art_code"`
	StkLieu string `json:"stk_lieu"`
	StkNosu string `json:"stk_nosu"`
	QuaCode string `json:"qua_code"`
	StkQte  int    `json:"stk_qte"`
}

// StockSearchResponse is the response body for the stock search endpoint.
type StockSearchResponse struct {
	Status  string      `json:"status"` // "success" or "error"
	Message string      `json:"message"`
	Items   []StockItem `json:"items"`
}
