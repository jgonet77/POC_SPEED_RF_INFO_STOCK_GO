package models

// LoginRequest is the request body for the login endpoint.
type LoginRequest struct {
	Login      string `json:"login"`
	Password   string `json:"password"`
	HashMethod string `json:"hash_method"` // "CLAIR", "MD5", or "SHA256"
}

// LoginResponse is the response body for the login endpoint.
// Token and ExpiresIn are only set on success; LogLocation is only set on error.
// Optional fields are pointers so they are omitted from the JSON when nil.
type LoginResponse struct {
	Status      string  `json:"status"`  // "success" or "error"
	Message     string  `json:"message"`
	Token       *string `json:"token,omitempty"`        // Only on success
	ExpiresIn   *int    `json:"expires_in,omitempty"`   // Only on success (86400 for 24h)
	LogLocation *string `json:"log_location,omitempty"` // Only on error
}
