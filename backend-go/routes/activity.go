package routes

import (
	"github.com/gofiber/fiber/v2"

	"stock-api/middleware"
	"stock-api/models"
	"stock-api/repositories"
)

// RegisterActivity wires the activities endpoint onto the application.
func RegisterActivity(app *fiber.App, repo *repositories.ActivityRepository) {
	app.Get("/api/activities", middleware.RequireAuth(), activitiesHandler(repo))
}

// activitiesHandler godoc
// @Summary     Liste des activités
// @Description Retourne toutes les activités actives (ACT_ACTF=1) de la table ACT_PAR
// @Tags        activities
// @Produce     json
// @Security    BearerAuth
// @Success     200 {object} models.ActivityListResponse
// @Failure     401 {object} map[string]string "Token manquant ou invalide"
// @Failure     500 {object} models.ActivityListResponse
// @Router      /api/activities [get]
func activitiesHandler(repo *repositories.ActivityRepository) fiber.Handler {
	return func(c *fiber.Ctx) error {
		rows, err := repo.GetAllActivities()
		if err != nil {
			return c.Status(fiber.StatusInternalServerError).JSON(models.ActivityListResponse{
				Status:     "error",
				Message:    "Failed to fetch activities",
				Activities: []models.ActivityItem{},
			})
		}

		activities := make([]models.ActivityItem, 0, len(rows))
		for _, row := range rows {
			activities = append(activities, models.ActivityItem{
				ActKeyu: row.ActKeyu,
				ActCode: row.ActCode,
				ActLib:  row.ActLib,
			})
		}

		return c.Status(fiber.StatusOK).JSON(models.ActivityListResponse{
			Status:     "success",
			Message:    "Activities retrieved successfully",
			Activities: activities,
		})
	}
}
