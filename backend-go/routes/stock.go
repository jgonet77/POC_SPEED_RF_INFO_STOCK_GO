package routes

import (
	"fmt"
	"strings"

	"github.com/gofiber/fiber/v2"

	"stock-api/middleware"
	"stock-api/models"
	"stock-api/repositories"
	"stock-api/services"
)

// RegisterStock wires the stock endpoints onto the application.
func RegisterStock(app *fiber.App, repo *repositories.StockRepository) {
	svc := services.NewStockService(repo)
	app.Post("/api/stock/search", middleware.RequireAuth(), stockSearchHandler(svc))
	app.Get("/api/stock/details/:sku", middleware.RequireAuth(), stockDetailsHandler)
}

// stockSearchHandler godoc
// @Summary     Recherche stock
// @Description Recherche des articles en stock filtrés par activité. Au moins un critère parmi art_code, stk_lieu, stk_nosu est obligatoire.
// @Tags        stock
// @Accept      json
// @Produce     json
// @Security    BearerAuth
// @Param       body body models.StockSearchRequest true "Critères de recherche"
// @Success     200 {object} models.StockSearchResponse "Résultats (items peut être vide)"
// @Failure     401 {object} map[string]string "Token manquant ou invalide"
// @Failure     422 {object} map[string]string "Corps invalide ou act_code manquant"
// @Router      /api/stock/search [post]
func stockSearchHandler(svc *services.StockService) fiber.Handler {
	return func(c *fiber.Ctx) error {
		var req models.StockSearchRequest
		if err := c.BodyParser(&req); err != nil {
			return c.Status(fiber.StatusUnprocessableEntity).JSON(fiber.Map{
				"status":  "error",
				"message": "Invalid request body",
				"detail":  err.Error(),
			})
		}

		trimPtr(&req.ArtCode)
		trimPtr(&req.StkLieu)
		trimPtr(&req.StkNosu)
		req.ActCode = strings.TrimSpace(req.ActCode)

		if req.ActCode == "" {
			return c.Status(fiber.StatusUnprocessableEntity).JSON(fiber.Map{
				"status":  "error",
				"message": "act_code is required",
				"detail":  "act_code must not be empty",
			})
		}

		login, _ := c.Locals("login").(string)
		fmt.Printf("STOCK_SEARCH_REQUEST act_code=%s art_code=%s stk_lieu=%s stk_nosu=%s user=%s\n",
			req.ActCode, ptrStr(req.ArtCode), ptrStr(req.StkLieu), ptrStr(req.StkNosu), login)

		response, err := svc.SearchStock(req)
		if err != nil {
			if !hasAnyCriterion(req) {
				return c.Status(fiber.StatusUnprocessableEntity).JSON(fiber.Map{
					"status":  "error",
					"message": response.Message,
					"detail":  err.Error(),
				})
			}
			response.Message = "Search failed due to invalid criteria or database error"
			fmt.Printf("STOCK_SEARCH_RESPONSE act_code=%s status=%s items_found=%d user=%s\n",
				req.ActCode, response.Status, len(response.Items), login)
			return c.Status(fiber.StatusOK).JSON(response)
		}

		if len(response.Items) == 0 {
			response.Message = "No stock items found matching criteria"
		} else {
			response.Message = fmt.Sprintf("Found %d item(s)", len(response.Items))
		}

		fmt.Printf("STOCK_SEARCH_RESPONSE act_code=%s status=%s items_found=%d user=%s\n",
			req.ActCode, response.Status, len(response.Items), login)

		return c.Status(fiber.StatusOK).JSON(response)
	}
}

// stockDetailsHandler godoc
// @Summary     Détail stock par SKU
// @Description Retourne les informations détaillées d'un article (non encore implémenté)
// @Tags        stock
// @Produce     json
// @Security    BearerAuth
// @Param       sku path string true "Code article (SKU)"
// @Success     200 {object} map[string]string
// @Failure     401 {object} map[string]string "Token manquant ou invalide"
// @Router      /api/stock/details/{sku} [get]
func stockDetailsHandler(c *fiber.Ctx) error {
	sku := c.Params("sku")
	login, _ := c.Locals("login").(string)
	return c.Status(fiber.StatusOK).JSON(fiber.Map{
		"status":  "success",
		"message": "Details endpoint (not yet implemented)",
		"sku":     sku,
		"user":    login,
	})
}

func trimPtr(p **string) {
	if *p == nil {
		return
	}
	trimmed := strings.TrimSpace(**p)
	*p = &trimmed
}

func hasAnyCriterion(req models.StockSearchRequest) bool {
	return ptrStr(req.ArtCode) != "" || ptrStr(req.StkLieu) != "" || ptrStr(req.StkNosu) != ""
}

func ptrStr(p *string) string {
	if p == nil {
		return ""
	}
	return *p
}
