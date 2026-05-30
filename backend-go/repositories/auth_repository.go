package repositories

import (
	"database/sql"
	"errors"
	"fmt"
)

// AuthUser holds the credentials read from the USW_DAT table.
type AuthUser struct {
	Login    string
	Password string
}

// AuthRepository provides access to user authentication data (table USW_DAT).
type AuthRepository struct {
	DB *sql.DB
}

// NewAuthRepository wires the shared connection pool into the repository.
func NewAuthRepository(db *sql.DB) *AuthRepository {
	return &AuthRepository{DB: db}
}

// FindUserByLogin returns the user matching the given login, or (nil, nil) when
// no such user exists. Any database/query failure is returned as an error.
func (r *AuthRepository) FindUserByLogin(login string) (*AuthUser, error) {
	const query = "SELECT USW_LOGN, USW_PASS FROM USW_DAT WHERE USW_LOGN = @p1"

	row := r.DB.QueryRow(query, login)

	var user AuthUser
	if err := row.Scan(&user.Login, &user.Password); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, fmt.Errorf("find user by login failed: %w", err)
	}

	return &user, nil
}
