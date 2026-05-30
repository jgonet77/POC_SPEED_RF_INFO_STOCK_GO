package repositories

import (
	"database/sql"
	"fmt"
)

// HealthRow holds the SQL Server diagnostics returned by a connectivity check.
type HealthRow struct {
	Version    string
	ServerTime string
}

// HealthRepository provides health-check operations (DB connectivity tests).
type HealthRepository struct {
	DB *sql.DB
}

// NewHealthRepository wires the shared connection pool into the repository.
func NewHealthRepository(db *sql.DB) *HealthRepository {
	return &HealthRepository{DB: db}
}

// CheckConnection verifies SQL Server connectivity and returns its version and
// current time. The timestamp is converted to a string directly in SQL so it
// can be scanned into a plain string.
func (r *HealthRepository) CheckConnection() (HealthRow, error) {
	const query = "SELECT @@VERSION, CONVERT(varchar, GETDATE(), 120)"

	row := r.DB.QueryRow(query)

	var health HealthRow
	if err := row.Scan(&health.Version, &health.ServerTime); err != nil {
		return HealthRow{}, fmt.Errorf("database health check failed: %w", err)
	}

	return health, nil
}
