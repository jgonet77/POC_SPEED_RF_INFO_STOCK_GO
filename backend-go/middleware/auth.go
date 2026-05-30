// Package middleware contains Fiber middleware handlers, notably the JWT
// authentication guard used to protect API endpoints.
package middleware

import (
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"

	"stock-api/config"
	"stock-api/services"
)

// tokenPreviewLength is the number of hex characters of the token's SHA256
// hash that are written to the audit log. Logging a hash prefix rather than the
// raw token preserves traceability without leaking credentials.
const tokenPreviewLength = 16

// authEndpoint labels failures originating from the protected-route guard in
// the audit log.
const authEndpoint = "protected"

// unauthorizedBody is the JSON envelope returned for every authentication
// failure. It is intentionally generic so as not to disclose which check failed
// to the client; the specific reason is recorded only in the server-side log.
func unauthorizedBody() fiber.Map {
	return fiber.Map{
		"status":  "error",
		"message": "Unauthorized: invalid or missing token",
		"detail":  "Please login first",
	}
}

// RequireAuth returns a Fiber handler that authenticates the request via a JWT
// bearer token. On success it stores the authenticated login under the "login"
// local and calls the next handler; on any failure it logs the reason and
// responds with HTTP 401.
//
// Validation steps, in order:
//  1. The Authorization header must be present.
//  2. It must be exactly "Bearer <token>".
//  3. The token must be a valid HS256 JWT signed with the configured secret and
//     must not be expired.
//  4. The token must still be active (present in services.Store).
func RequireAuth() fiber.Handler {
	return func(c *fiber.Ctx) error {
		authorization := c.Get("Authorization")
		if authorization == "" {
			logAuthFailure(authEndpoint, "", "MISSING")
			return c.Status(fiber.StatusUnauthorized).JSON(unauthorizedBody())
		}

		parts := strings.Split(authorization, " ")
		if len(parts) != 2 || parts[0] != "Bearer" {
			logAuthFailure(authEndpoint, authorization, "MALFORMED_HEADER")
			return c.Status(fiber.StatusUnauthorized).JSON(unauthorizedBody())
		}

		token := parts[1]

		claims := &jwt.MapClaims{}
		keyFunc := func(_ *jwt.Token) (interface{}, error) {
			return []byte(config.Default.SecretKey), nil
		}

		_, err := jwt.ParseWithClaims(token, claims, keyFunc, jwt.WithValidMethods([]string{"HS256"}))
		if err != nil {
			reason := "INVALID_SIGNATURE"
			if errors.Is(err, jwt.ErrTokenExpired) {
				reason = "EXPIRED"
			}
			logAuthFailure(authEndpoint, token, reason)
			return c.Status(fiber.StatusUnauthorized).JSON(unauthorizedBody())
		}

		// Revocation check: a structurally valid token is only honoured while it
		// remains in the active token store.
		if !services.Store.Contains(token) {
			logAuthFailure(authEndpoint, token, "NOT_IN_ACTIVE_TOKENS")
			return c.Status(fiber.StatusUnauthorized).JSON(unauthorizedBody())
		}

		login, _ := (*claims)["login"].(string)
		c.Locals("login", login)

		return c.Next()
	}
}

// tokenHashPreview returns the first tokenPreviewLength hex characters of the
// token's SHA256 digest, or "MISSING" when the token is empty. This is the only
// form of the token written to logs.
func tokenHashPreview(token string) string {
	if token == "" {
		return "MISSING"
	}
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])[:tokenPreviewLength]
}

// logAuthFailure records an authentication failure to the configured log file
// (creating the parent directory if needed) and echoes it to stdout. Logging is
// best-effort: an inability to write the file must not break request handling,
// so I/O errors are reported to stderr rather than propagated.
func logAuthFailure(endpoint, token, reason string) {
	timestamp := time.Now().Format("2006-01-02 15:04:05")
	line := fmt.Sprintf("[%s] AUTH_FAILED endpoint=%s token=%s reason=%s",
		timestamp, endpoint, tokenHashPreview(token), reason)

	logPath := config.Default.LogPath
	if dir := filepath.Dir(logPath); dir != "" && dir != "." {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			fmt.Fprintf(os.Stderr, "middleware: cannot create log directory %q: %v\n", dir, err)
		}
	}

	f, err := os.OpenFile(logPath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0o644)
	if err != nil {
		fmt.Fprintf(os.Stderr, "middleware: cannot open log file %q: %v\n", logPath, err)
	} else {
		if _, werr := f.WriteString(line + "\n"); werr != nil {
			fmt.Fprintf(os.Stderr, "middleware: cannot write log file %q: %v\n", logPath, werr)
		}
		_ = f.Close()
	}

	fmt.Println(line)
}
