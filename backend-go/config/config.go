// Package config centralizes application configuration loaded from
// environment variables. A single *Settings instance (Default) is built at
// process startup and shared everywhere.
package config

import (
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"

	"github.com/joho/godotenv"
)

// Settings holds all runtime configuration sourced from environment variables.
type Settings struct {
	// SQL Server connection.
	SQLServerHost     string
	SQLServerPort     int
	SQLServerDB       string
	SQLServerUser     string
	SQLServerPassword string
	// SQLAuthType selects the authentication mode: "windows" for integrated
	// (trusted) authentication, anything else for SQL Server login.
	SQLAuthType string

	// API server.
	APIHost   string
	APIPort   int
	APIReload bool

	// Auth.
	SecretKey string

	// CORS allowed origins (raw, comma-separated).
	CORSOrigins string

	// Path to the authentication log file.
	LogPath string
}

// Default is the process-wide configuration singleton, initialised by init().
var Default *Settings

func init() {
	// Load .env before reading env vars — non-fatal if absent (production uses host env).
	_ = godotenv.Load()
	Default = load()
}

// load reads environment variables and applies sensible defaults. The SQL
// Server password is mandatory: an empty value panics to fail fast at startup.
func load() *Settings {
	authType := strings.ToLower(getEnv("SQL_AUTH_TYPE", "sql"))

	// Password is mandatory only for SQL login; Windows integrated auth uses
	// the current Windows identity and needs no password.
	password := os.Getenv("SQL_SERVER_PASSWORD")
	if authType != "windows" && password == "" {
		panic("config: SQL_SERVER_PASSWORD is required but not set")
	}

	return &Settings{
		SQLServerHost:     getEnv("SQL_SERVER_HOST", "localhost"),
		SQLServerPort:     getEnvInt("SQL_SERVER_PORT", 1433),
		SQLServerDB:       getEnv("SQL_SERVER_DB", "WMS_SPEED"),
		SQLServerUser:     getEnv("SQL_SERVER_USER", "sa"),
		SQLServerPassword: password,
		SQLAuthType:       authType,

		APIHost:   getEnv("API_HOST", "0.0.0.0"),
		APIPort:   getEnvInt("API_PORT", 8000),
		APIReload: getEnvBool("API_RELOAD", false),

		SecretKey: getEnv("SECRET_KEY", "dev-secret-key-change-in-prod"),

		CORSOrigins: getEnv("CORS_ORIGINS", "http://localhost:3000,http://10.0.2.2:3000"),

		LogPath: getEnv("LOG_PATH", "logs/auth.log"),
	}
}

// DSN builds the go-mssqldb connection string using the sqlserver:// URL scheme.
//
//   - Named instance (host contains a backslash, e.g. "HOST\INSTANCE"):
//     sqlserver://USER:PASS@HOST?database=DB&instance=INSTANCE
//   - Default instance:
//     sqlserver://USER:PASS@HOST:PORT?database=DB
func (s *Settings) DSN() string {
	query := url.Values{}
	query.Set("database", s.SQLServerDB)

	var host string
	if idx := strings.Index(s.SQLServerHost, `\`); idx >= 0 {
		// Named instance: pass instance name as query param to avoid URL-encoding issues.
		host = s.SQLServerHost[:idx]
		query.Set("instance", s.SQLServerHost[idx+1:])
	} else {
		host = fmt.Sprintf("%s:%d", s.SQLServerHost, s.SQLServerPort)
	}

	u := &url.URL{
		Scheme:   "sqlserver",
		Host:     host,
		RawQuery: query.Encode(),
	}
	// SQL login carries credentials in the URL userinfo. Windows integrated
	// auth omits userinfo entirely: go-mssqldb then uses the current Windows
	// identity (trusted connection) when no user id is supplied.
	if s.SQLAuthType != "windows" {
		u.User = url.UserPassword(s.SQLServerUser, s.SQLServerPassword)
	}
	return u.String()
}

// CORSOriginsList returns the configured CORS origins as a trimmed slice,
// split on commas. Empty entries are skipped.
func (s *Settings) CORSOriginsList() []string {
	parts := strings.Split(s.CORSOrigins, ",")
	origins := make([]string, 0, len(parts))
	for _, p := range parts {
		if trimmed := strings.TrimSpace(p); trimmed != "" {
			origins = append(origins, trimmed)
		}
	}
	return origins
}

// getEnv returns the value of the named environment variable or fallback when
// it is unset or empty.
func getEnv(name, fallback string) string {
	if v := os.Getenv(name); v != "" {
		return v
	}
	return fallback
}

// getEnvInt parses the named environment variable as an int, returning fallback
// when unset, and panicking on a malformed value to fail fast at startup.
func getEnvInt(name string, fallback int) int {
	raw := os.Getenv(name)
	if raw == "" {
		return fallback
	}
	v, err := strconv.Atoi(raw)
	if err != nil {
		panic(fmt.Sprintf("config: env var %s must be an integer, got %q", name, raw))
	}
	return v
}

// getEnvBool parses the named environment variable as a bool, returning
// fallback when unset or unparseable.
func getEnvBool(name string, fallback bool) bool {
	raw := os.Getenv(name)
	if raw == "" {
		return fallback
	}
	v, err := strconv.ParseBool(raw)
	if err != nil {
		return fallback
	}
	return v
}
