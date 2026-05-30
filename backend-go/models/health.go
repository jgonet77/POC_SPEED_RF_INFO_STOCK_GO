package models

// HealthResponse is the response body for the database health check endpoint.
type HealthResponse struct {
	Service        string                 `json:"service"`
	DatabaseStatus string                 `json:"database_status"`
	Details        map[string]interface{} `json:"details"`
}
