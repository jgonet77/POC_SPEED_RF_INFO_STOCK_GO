// Command stock-api is the HTTP entry point for the Stock API. It wires the
// configuration, database pool, repositories and routes together, then serves
// a Fiber application until interrupted.

// @title          Stock API — WMS SPEED
// @version        0.1.0
// @description    API de consultation du stock pour l'application mobile Android (POC)
// @contact.name   BK Systèmes
// @contact.email  jerome.gouez@bksystemes.fr
// @host
// @BasePath       /
// @securityDefinitions.apikey BearerAuth
// @in             header
// @name           Authorization
// @description    JWT obtenu via POST /api/login. Format : "Bearer <token>"
package main

import (
	"errors"
	"log"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/cors"
	"github.com/gofiber/fiber/v2/middleware/logger"
	fiberSwagger "github.com/gofiber/swagger"

	_ "stock-api/docs"

	"stock-api/config"
	"stock-api/db"
	"stock-api/repositories"
	"stock-api/routes"
)

func main() {
	// config.Default is already initialised by config.init() which loads .env.

	// Open the shared SQL Server connection pool once.
	if err := db.Init(config.Default.DSN()); err != nil {
		log.Printf("WARNING: database initialisation failed: %v — DB endpoints will return 500", err)
	}
	defer db.Close()

	// Build the Fiber application with a uniform error envelope.
	app := fiber.New(fiber.Config{
		ErrorHandler: func(c *fiber.Ctx, err error) error {
			code := fiber.StatusInternalServerError
			var e *fiber.Error
			if errors.As(err, &e) {
				code = e.Code
			}
			return c.Status(code).JSON(fiber.Map{
				"status":  "error",
				"message": err.Error(),
				"detail":  err.Error(),
			})
		},
	})

	// HTTP access log — affiche chaque requête entrante avec IP, méthode, path, status, durée.
	app.Use(logger.New(logger.Config{
		Format: "[${time}] ${ip} ${method} ${path} → ${status} (${latency})\n",
	}))

	// Cross-origin support for the mobile/web clients.
	app.Use(cors.New(cors.Config{
		AllowOrigins:     strings.Join(config.Default.CORSOriginsList(), ","),
		AllowMethods:     "GET,POST,PUT,DELETE,OPTIONS",
		AllowHeaders:     "*",
		AllowCredentials: true,
	}))

	// Swagger UI — accessible sur /swagger/index.html
	app.Get("/swagger/*", fiberSwagger.HandlerDefault)

	// Root liveness route.
	app.Get("/", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{"message": "Stock API is running"})
	})

	// Instantiate repositories over the shared pool.
	authRepo := repositories.NewAuthRepository(db.DB)
	stockRepo := repositories.NewStockRepository(db.DB)
	activityRepo := repositories.NewActivityRepository(db.DB)
	healthRepo := repositories.NewHealthRepository(db.DB)

	// Register all route groups.
	routes.RegisterHealth(app, healthRepo)
	routes.RegisterAuth(app, authRepo)
	routes.RegisterActivity(app, activityRepo)
	routes.RegisterStock(app, stockRepo)

	// Graceful shutdown on SIGINT/SIGTERM.
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	go func() {
		<-quit
		log.Println("shutting down server...")
		_ = app.Shutdown()
	}()

	addr := config.Default.APIHost + ":" + strconv.Itoa(config.Default.APIPort)
	log.Printf("Starting server on %s", addr)
	log.Fatal(app.Listen(addr))
}
