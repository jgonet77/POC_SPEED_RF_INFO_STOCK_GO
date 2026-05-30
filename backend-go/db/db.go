package db

import (
	"database/sql"
	"fmt"
	"time"

	_ "github.com/microsoft/go-mssqldb"
)

// DB is the shared SQL Server connection pool. It is opened once by Init and
// reused everywhere through constructor injection into the repositories.
var DB *sql.DB

// Init opens the SQL Server connection pool, verifies connectivity with a Ping
// and configures the pool sizing. It must be called once at application startup.
func Init(dsn string) error {
	conn, err := sql.Open("sqlserver", dsn)
	if err != nil {
		return fmt.Errorf("failed to open database connection: %w", err)
	}

	// sql.DB is a pool of connections, not a single connection. Tune it once.
	conn.SetMaxOpenConns(10)
	conn.SetMaxIdleConns(5)
	conn.SetConnMaxLifetime(5 * time.Minute)

	// Ping is attempted but non-fatal: the pool is valid even if the DB is
	// temporarily unreachable. Errors will surface on the first query.
	if err := conn.Ping(); err != nil {
		fmt.Printf("WARNING: database not reachable at startup: %v\n", err)
	}

	DB = conn
	return nil
}

// Close releases the connection pool. Safe to call even if Init was never run.
func Close() {
	if DB != nil {
		_ = DB.Close()
		DB = nil
	}
}
