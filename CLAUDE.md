# CLAUDE.md - POC Stock Mobile (Android + API Go)

## Vision du projet

POC d'application mobile Android pour permettre aux préparateurs/caristes de consulter le stock de l'entrepôt en temps réel via une API REST Go, connectée au WMS SPEED (SQL Server).

**Objectif :** Valider la faisabilité technique (connexion mobile ↔ BD, authentification, performance, scan code-barres).

**État actuel :** Migration du backend Python (FastAPI) vers Go (Fiber) en cours. Le backend Python est conservé dans `backend/` comme référence uniquement — ne pas modifier.

---

## Stack technique

- **Frontend :** Android natif (Kotlin)
- **Backend :** Go 1.22 (Fiber v2) — module `stock-api`
- **Base de données :** Microsoft SQL Server (WMS SPEED)
- **Driver SQL :** `github.com/microsoft/go-mssqldb` (pur Go, sans ODBC)
- **Auth :** JWT HS256 via `github.com/golang-jwt/jwt/v5`
- **Infrastructure :** Local (accessible depuis le réseau local du téléphone)

---

## Architecture

```
┌─────────────────────────────────────────────┐
│         ANDROID KOTLIN APP                  │
│  (UI: Login, Activités, Recherche stock)    │
└─────────────┬───────────────────────────────┘
              │ HTTP/REST (Retrofit + Bearer token)
              ↓
┌─────────────────────────────────────────────┐
│      GO FIBER SERVER (Couches)              │
├─────────────────────────────────────────────┤
│  Routes (handlers Fiber)                    │
├─────────────────────────────────────────────┤
│  Services (logique métier)                  │
├─────────────────────────────────────────────┤
│  Repositories (accès données)               │
└─────────────┬───────────────────────────────┘
              │ go-mssqldb (sqlserver:// DSN)
              ↓
┌─────────────────────────────────────────────┐
│      MICROSOFT SQL SERVER                   │
│      WMS SPEED                              │
│  USW_DAT · ACT_PAR · STK_DAT               │
└─────────────────────────────────────────────┘
```

---

## Structure du projet

```
POC_SPEED_RF_INFO_STOCK_GO/
├── backend/                    # Référence Python (FastAPI) — NE PAS MODIFIER
│   ├── main.py / config.py
│   ├── models/ routes/ services/ repositories/ middleware/
│   ├── tests/
│   ├── .env                    # Config locale réelle
│   └── requirements.txt
├── backend-go/                 # Backend Go — source active
│   ├── go.mod / go.sum
│   ├── main.go                 # Point d'entrée Fiber
│   ├── config/config.go        # Settings + DSN()
│   ├── db/db.go                # Pool *sql.DB
│   ├── middleware/auth.go      # RequireAuth() fiber.Handler
│   ├── models/                 # Structs request/response (json tags snake_case)
│   │   ├── auth.go · stock.go · activity.go · health.go
│   ├── repositories/           # Accès SQL Server (paramétré @p1/@p2)
│   │   ├── auth_repository.go · stock_repository.go
│   │   ├── activity_repository.go · health_repository.go
│   ├── services/               # Logique métier + token store
│   │   ├── token_store.go · health_service.go · stock_service.go
│   ├── routes/                 # Handlers Fiber par domaine
│   │   ├── auth.go · stock.go · activity.go · health.go · helpers.go
│   └── logs/                   # Logs auth (auth.log)
├── android/                    # App Android Kotlin (MVVM)
│   └── app/src/main/java/com/example/stockapp/
│       ├── api/ models/ repositories/ viewmodels/
│       └── (LoginActivity, StockSearchActivity, ActivitySelectionActivity…)
├── docs/                       # Specs fonctionnelles et techniques (HTML)
├── MIGRATION.md                # Documentation exhaustive du backend Python
│                               # (endpoints, tables SQL, env vars, logique)
└── CLAUDE.md                   # Ce fichier
```

---

## Conventions de code

### Backend (Go)

- **Nommage :** `PascalCase` pour types/fonctions exportées, `camelCase` pour non-exportés
- **Erreurs :** retournées explicitement `(value, error)` — pas de panic sauf au démarrage
- **SQL :** toujours paramétré avec `@p1, @p2, …` (syntaxe go-mssqldb) — jamais de `fmt.Sprintf` dans les requêtes
- **Injection :** `*sql.DB` injecté par constructeur `New*(db)` — jamais accédé globalement dans les repositories
- **Concurrence :** `services.Store` (TokenStore) utilise `sync.RWMutex` — tous les handlers Fiber sont concurrents
- **JSON :** tags snake_case obligatoires, `omitempty` sur les champs optionnels des réponses
- **Enveloppe d'erreur HTTP :** toujours `{"status":"error","message":"...","detail":"..."}`

### Frontend (Android/Kotlin)

- **Pattern :** MVVM (ViewModel + Repository)
- **Réseau :** Retrofit avec `AuthInterceptor` (injection Bearer token, gestion 401)
- **UI updates :** LiveData + Coroutines
- **Nommage :** `PascalCase` classes, `camelCase` propriétés

---

## Authentification

1. L'utilisateur saisit login/password sur l'écran de connexion
2. `POST /api/login` — valide contre la table `USW_DAT` (colonnes `USW_LOGN`, `USW_PASS`)
3. Le mot de passe peut être envoyé en clair, MD5 ou SHA256 (`hash_method`)
4. L'API retourne un JWT HS256 valable 24 h
5. L'app stocke le token dans SharedPreferences
6. Chaque requête protégée inclut `Authorization: Bearer <token>`
7. Le token est aussi vérifié dans le store mémoire `services.Store` (révocation)

**Tables SQL :**
- `USW_DAT` — utilisateurs (`USW_LOGN`, `USW_PASS`)
- `ACT_PAR` — activités (`ACT_KEYU`, `ACT_CODE`, `ACT_LIB`, `ACT_ACTF`)
- `STK_DAT` — stock (`ACT_CODE`, `ART_CODE`, `STK_LIEU`, `STK_NOSU`, `QUA_CODE`, `STK_QTE`)

---

## Endpoints REST

| Méthode | Route | Auth | Description |
|---------|-------|------|-------------|
| GET | `/` | — | Liveness check |
| GET | `/api/health/api` | — | Santé de l'API |
| GET | `/api/health/database` | — | Test connexion SQL Server |
| POST | `/api/login` | — | Authentification → JWT |
| GET | `/api/activities` | JWT | Liste des activités actives |
| POST | `/api/stock/search` | JWT | Recherche stock (art_code / stk_lieu / stk_nosu) |
| GET | `/api/stock/details/:sku` | JWT | Détail stock (non implémenté) |

Voir `MIGRATION.md` pour la description complète de chaque endpoint (corps, réponses, logique).

---

## Variables d'environnement

| Variable | Obligatoire | Défaut |
|---|---|---|
| `SQL_SERVER_PASSWORD` | **OUI** | — |
| `SQL_SERVER_HOST` | non | `localhost` |
| `SQL_SERVER_PORT` | non | `1433` |
| `SQL_SERVER_DB` | non | `WMS_SPEED` |
| `SQL_SERVER_USER` | non | `sa` |
| `API_HOST` | non | `0.0.0.0` |
| `API_PORT` | non | `8000` |
| `SECRET_KEY` | non | `dev-secret-key-change-in-prod` |
| `CORS_ORIGINS` | non | `http://localhost:3000,http://10.0.2.2:3000` |
| `LOG_PATH` | non | `logs/auth.log` |

Le fichier `.env` de référence est dans `backend/.env`.

---

## Tests

- **Backend Go :** tests unitaires (services), tests intégration (repositories avec vraie BD)
- **Frontend Android :** tests unitaires (ViewModels), tests UI (Espresso)
- Coverage cible : ≥ 80% sur le code critique

---

## Déploiement

**Développement :**
```bash
cd backend-go
go mod tidy
go run .            # écoute sur 0.0.0.0:8000
```

**Build production :**
```bash
cd backend-go
go build -o stock-api .
./stock-api
```

L'API est accessible depuis le téléphone via WiFi local sur le port configuré.

---

## Points de contact

- **WMS SPEED DB :** `LAPVMI116\SQL2019` — base `POC_V8SpeedDeveloppement`
- **Utilisateurs cibles :** Préparateurs/caristes
- **Modèles recommandés :** Sonnet 4.6 pour specs/docs, Opus 4.8 pour dev et migration

---

## Checklist démarrage

- [x] Backend Python documenté dans `MIGRATION.md`
- [x] Backend Go généré dans `backend-go/` (20 fichiers)
- [ ] `go mod tidy` + `go build ./...` (nécessite Go 1.22+)
- [ ] Test de connexion SQL Server (`GET /api/health/database`)
- [ ] Test login (`POST /api/login`)
- [ ] Test recherche stock (`POST /api/stock/search`)
- [ ] Implémenter `GET /api/stock/details/:sku`
- [ ] Tests unitaires Go (services + repositories)
