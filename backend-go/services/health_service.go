package services

import (
	"stock-api/models"
	"stock-api/repositories"
)

// HealthService is the service layer for health check operations.
type HealthService struct {
	Repo *repositories.HealthRepository
}

// NewHealthService creates a HealthService backed by the given repository.
func NewHealthService(repo *repositories.HealthRepository) *HealthService {
	return &HealthService{Repo: repo}
}

// GetDatabaseHealth checks the database connection and returns a formatted
// health response. On failure it returns a HealthResponse with a status of
// "error" and the underlying error so the caller can surface the detail.
func (s *HealthService) GetDatabaseHealth() (models.HealthResponse, error) {
	row, err := s.Repo.CheckConnection()
	if err != nil {
		return models.HealthResponse{
			Service:        "Database Connection Test",
			DatabaseStatus: "error",
			Details: map[string]interface{}{
				"error": err.Error(),
			},
		}, err
	}

	return models.HealthResponse{
		Service:        "Database Connection Test",
		DatabaseStatus: "connected",
		Details: map[string]interface{}{
			"version":     row.Version,
			"server_time": row.ServerTime,
		},
	}, nil
}
