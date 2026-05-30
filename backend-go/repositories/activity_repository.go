package repositories

import (
	"database/sql"
	"fmt"
)

// ActivityRow is a single activity entry returned by the activity repository.
type ActivityRow struct {
	ActKeyu int
	ActCode string
	ActLib  string
}

// ActivityRepository provides access to activity data (table ACT_PAR).
type ActivityRepository struct {
	DB *sql.DB
}

// NewActivityRepository wires the shared connection pool into the repository.
func NewActivityRepository(db *sql.DB) *ActivityRepository {
	return &ActivityRepository{DB: db}
}

// GetAllActivities returns the active activities (ACT_ACTF = 1) ordered by label.
func (r *ActivityRepository) GetAllActivities() ([]ActivityRow, error) {
	const query = "SELECT ACT_KEYU, ACT_CODE, ACT_LIB FROM ACT_PAR WHERE ACT_ACTF = 1 ORDER BY ACT_LIB"

	rows, err := r.DB.Query(query)
	if err != nil {
		return nil, fmt.Errorf("get all activities failed: %w", err)
	}
	defer rows.Close()

	var activities []ActivityRow
	for rows.Next() {
		var activity ActivityRow
		if err := rows.Scan(&activity.ActKeyu, &activity.ActCode, &activity.ActLib); err != nil {
			return nil, fmt.Errorf("activity scan failed: %w", err)
		}
		activities = append(activities, activity)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("activity iteration failed: %w", err)
	}

	return activities, nil
}
