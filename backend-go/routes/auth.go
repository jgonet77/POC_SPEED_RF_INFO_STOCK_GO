package routes

import (
	"crypto/md5"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/golang-jwt/jwt/v5"

	"stock-api/config"
	"stock-api/models"
	"stock-api/repositories"
	"stock-api/services"
)

var validHashMethods = map[string]bool{
	"CLAIR":  true,
	"MD5":    true,
	"SHA256": true,
}

const tokenExpirySeconds = 86400

// RegisterAuth wires the authentication endpoint onto the application.
func RegisterAuth(app *fiber.App, repo *repositories.AuthRepository) {
	app.Post("/api/login", loginHandler(repo))
}

// loginHandler godoc
// @Summary     Authentification
// @Description Authentifie un utilisateur et retourne un JWT valable 24h
// @Tags        auth
// @Accept      json
// @Produce     json
// @Param       body body models.LoginRequest true "Identifiants"
// @Success     200 {object} models.LoginResponse "Login réussi — contient le token"
// @Failure     400 {object} models.LoginResponse "hash_method invalide"
// @Failure     401 {object} models.LoginResponse "Login ou mot de passe incorrect"
// @Failure     500 {object} models.LoginResponse "Erreur base de données ou JWT"
// @Router      /api/login [post]
func loginHandler(repo *repositories.AuthRepository) fiber.Handler {
	return func(c *fiber.Ctx) error {
		var req models.LoginRequest
		if err := c.BodyParser(&req); err != nil {
			return c.Status(fiber.StatusBadRequest).JSON(models.LoginResponse{
				Status:      "error",
				Message:     "Invalid request body",
				LogLocation: ptr(config.Default.LogPath),
			})
		}

		if !validHashMethods[req.HashMethod] {
			logAuthEvent(req.Login, req.HashMethod, false, "InvalidHashMethod")
			return c.Status(fiber.StatusBadRequest).JSON(models.LoginResponse{
				Status:      "error",
				Message:     "Invalid hash_method",
				LogLocation: ptr(config.Default.LogPath),
			})
		}

		user, err := repo.FindUserByLogin(req.Login)
		if err != nil {
			logAuthEvent(req.Login, req.HashMethod, false, "DB_ERROR: "+err.Error())
			return c.Status(fiber.StatusInternalServerError).JSON(models.LoginResponse{
				Status:      "error",
				Message:     "Database error",
				LogLocation: ptr(config.Default.LogPath),
			})
		}
		if user == nil {
			logAuthEvent(req.Login, req.HashMethod, false, "User not found")
			return c.Status(fiber.StatusUnauthorized).JSON(models.LoginResponse{
				Status:      "error",
				Message:     "Invalid login or password",
				LogLocation: ptr(config.Default.LogPath),
			})
		}

		if hashPassword(req.Password, req.HashMethod) != user.Password {
			logAuthEvent(req.Login, req.HashMethod, false, "Password mismatch")
			return c.Status(fiber.StatusUnauthorized).JSON(models.LoginResponse{
				Status:      "error",
				Message:     "Invalid login or password",
				LogLocation: ptr(config.Default.LogPath),
			})
		}

		claims := jwt.MapClaims{
			"login": user.Login,
			"exp":   time.Now().UTC().Add(24 * time.Hour).Unix(),
		}
		token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
		signed, err := token.SignedString([]byte(config.Default.SecretKey))
		if err != nil {
			logAuthEvent(req.Login, req.HashMethod, false, "JWT_ERROR: "+err.Error())
			return c.Status(fiber.StatusInternalServerError).JSON(models.LoginResponse{
				Status:      "error",
				Message:     "Token generation error",
				LogLocation: ptr(config.Default.LogPath),
			})
		}

		services.Store.Add(signed, user.Login)
		logAuthEvent(req.Login, req.HashMethod, true, "")

		return c.Status(fiber.StatusOK).JSON(models.LoginResponse{
			Status:    "success",
			Message:   "Login successful",
			Token:     ptr(signed),
			ExpiresIn: ptr(tokenExpirySeconds),
		})
	}
}

func hashPassword(password, method string) string {
	switch method {
	case "MD5":
		sum := md5.Sum([]byte(password))
		return hex.EncodeToString(sum[:])
	case "SHA256":
		sum := sha256.Sum256([]byte(password))
		return hex.EncodeToString(sum[:])
	default:
		return password
	}
}

func logAuthEvent(login, hashMethod string, success bool, errorMsg string) {
	status := "FAILED"
	if success {
		status = "SUCCESS"
	}
	timestamp := time.Now().Format("2006-01-02 15:04:05")
	line := fmt.Sprintf("[%s] LOGIN_ATTEMPT login=%s hash=%s status=%s",
		timestamp, login, hashMethod, status)
	if errorMsg != "" {
		line += " error=" + errorMsg
	}
	fmt.Println(line)
	logPath := config.Default.LogPath
	if dir := filepath.Dir(logPath); dir != "" && dir != "." {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			return
		}
	}
	f, err := os.OpenFile(logPath, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0o644)
	if err != nil {
		return
	}
	defer f.Close()
	_, _ = f.WriteString(line + "\n")
}
