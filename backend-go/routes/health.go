package routes

import (
	"github.com/gofiber/fiber/v2"

	"stock-api/models"
	"stock-api/repositories"
	"stock-api/services"
)

// RegisterHealth wires the health-check endpoints onto the application.
func RegisterHealth(app *fiber.App, repo *repositories.HealthRepository) {
	svc := services.NewHealthService(repo)
	app.Get("/api/health/api", healthAPIHandler)
	app.Get("/api/health/database", healthDatabaseHandler(svc))
}

// healthAPIHandler godoc
// @Summary     Santé de l'API
// @Description Retourne 200 si le processus API est actif (ne nécessite pas SQL Server)
// @Tags        health
// @Produce     json
// @Success     200 {object} map[string]string "status=healthy"
// @Router      /api/health/api [get]
func healthAPIHandler(c *fiber.Ctx) error {
	return c.JSON(fiber.Map{
		"status":  "healthy",
		"message": "API is running",
	})
}

// healthDatabaseHandler godoc
// @Summary     Connexion SQL Server
// @Description Teste la connexion à SQL Server et retourne la version et l'heure serveur
// @Tags        health
// @Produce     json
// @Success     200 {object} models.HealthResponse
// @Failure     500 {object} map[string]string
// @Router      /api/health/database [get]
func healthDatabaseHandler(svc *services.HealthService) fiber.Handler {
	return func(c *fiber.Ctx) error {
		result, err := svc.GetDatabaseHealth()
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(fiber.Map{
				"status":  "error",
				"message": "Health check failed",
				"detail":  err.Error(),
			})
		}
		return c.Status(fiber.StatusOK).JSON(result)
	}
}

// ensure models is used for swag doc generation
var _ models.HealthResponse
