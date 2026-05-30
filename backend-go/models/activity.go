package models

// ActivityItem is a single activity entry.
type ActivityItem struct {
	ActKeyu int    `json:"act_keyu"`
	ActCode string `json:"act_code"`
	ActLib  string `json:"act_lib"`
}

// ActivityListResponse is the response body for the activities endpoint.
type ActivityListResponse struct {
	Status     string         `json:"status"` // "success" or "error"
	Message    string         `json:"message"`
	Activities []ActivityItem `json:"activities"`
}
