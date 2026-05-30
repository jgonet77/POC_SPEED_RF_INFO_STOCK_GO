# MIGRATION Python → Go (Fiber)

Ce document décrit exhaustivement le backend Python existant.
Il sert de référence unique pour la réécriture en Go avec Fiber.

---

## 1. Framework et dépendances Python

| Dépendance       | Version | Rôle                                      |
|------------------|---------|-------------------------------------------|
| fastapi          | 0.104.1 | Framework web (routing, validation, CORS) |
| uvicorn          | 0.24.0  | Serveur ASGI (HTTP)                       |
| pydantic         | 2.5.0   | Validation et sérialisation des modèles   |
| pyodbc           | 5.1.0   | Driver SQL Server (ODBC)                  |
| PyJWT            | 2.8.0   | Génération et vérification des tokens JWT |
| python-dotenv    | 1.0.0   | Chargement du fichier `.env`              |
| httpx            | 0.25.1  | Client HTTP (tests uniquement)            |
| pytest           | 7.4.3   | Tests unitaires                           |

Point d'entrée : `main.py`, serveur démarré avec `uvicorn`.

---

## 2. Variables d'environnement

| Variable            | Obligatoire | Défaut                                    | Description                                               |
|---------------------|-------------|-------------------------------------------|-----------------------------------------------------------|
| `SQL_SERVER_PASSWORD` | **OUI**   | *(aucun)*                                 | Mot de passe SQL Server — requis au démarrage             |
| `SQL_SERVER_HOST`   | non         | `localhost`                               | Hostname ou IP du serveur SQL. Accepte `HOST\INSTANCE`    |
| `SQL_SERVER_PORT`   | non         | `1433`                                    | Port SQL Server (ignoré si instance nommée avec `\`)      |
| `SQL_SERVER_DB`     | non         | `WMS_SPEED`                               | Nom de la base de données                                 |
| `SQL_SERVER_USER`   | non         | `sa`                                      | Login SQL Server                                          |
| `API_HOST`          | non         | `0.0.0.0`                                 | Interface d'écoute de l'API                               |
| `API_PORT`          | non         | `8000`                                    | Port d'écoute de l'API                                    |
| `API_RELOAD`        | non         | `false`                                   | Auto-reload en développement (`true`/`false`)             |
| `SECRET_KEY`        | non         | `dev-secret-key-change-in-prod`           | Clé HMAC pour signer/vérifier les JWT                     |
| `CORS_ORIGINS`      | non         | `http://localhost:3000,http://10.0.2.2:3000` | Origines CORS autorisées, séparées par virgule         |
| `LOG_PATH`          | non         | `logs/auth.log`                           | Chemin du fichier de log d'authentification               |

---

## 3. Connexion SQL Server

### Driver
- **Bibliothèque Python** : `pyodbc` 5.1.0
- **Driver ODBC** : `ODBC Driver 17 for SQL Server` (doit être installé sur le système)

### Chaîne de connexion
Construite dynamiquement dans `config.py` :

```
Driver={ODBC Driver 17 for SQL Server};Server=HOST,PORT;Database=DB;UID=USER;PWD=PASS;
```

Règle spéciale pour les **instances nommées** (ex. `LAPVMI116\SQL2019`) :
- Si `\` est présent dans `SQL_SERVER_HOST` → le port est omis de la chaîne
- Sinon → `Server=HOST,PORT`

### Pattern de connexion (BaseRepository)
- Une connexion est ouverte **à la demande** (lazy) lors du premier appel à `execute_query()`
- **Pas de pool de connexions** : chaque instance de repository ouvre sa propre connexion
- La connexion est fermée manuellement via `repo.close()` dans un bloc `finally` au niveau de la route
- Les curseurs sont fermés immédiatement après chaque requête (`cursor.close()` en `finally`)

### Hiérarchie des exceptions DB
```
RepositoryError (base)
├── DatabaseConnectionError  → échec de connexion pyodbc
└── QueryError               → échec d'exécution de requête
```

---

## 4. Authentification

### Mécanisme JWT
- **Algorithme** : HS256
- **Durée de validité** : 24 heures (`expires_in: 86400`)
- **Payload** : `{ "login": "<login>", "exp": <timestamp UTC> }`
- **Clé de signature** : variable d'environnement `SECRET_KEY`

### Store de tokens (en mémoire)
Un dictionnaire global `active_tokens: dict[str, str]` (dans `services/token_store.py`) mappe chaque token vers le login correspondant. Sert de **liste de révocation** : un token JWT cryptographiquement valide mais absent de ce dictionnaire est rejeté avec 401.

> **Point d'attention pour Go** : ce store est en mémoire — il est perdu au redémarrage. À implémenter avec le même comportement (ou décider de le persister).

### Middleware de vérification (protect endpoints)
Injecté via `Depends(verify_token)` sur chaque route protégée. Logique :
1. Vérifie la présence du header `Authorization`
2. Vérifie le format `Bearer <token>`
3. Décode le JWT (vérifie signature + expiration)
4. Vérifie que le token est dans `active_tokens`
5. Retourne le payload décodé (`{"login": "...", "exp": ...}`) à la route

En cas d'échec → `401` avec corps :
```json
{
  "status": "error",
  "message": "Unauthorized: invalid or missing token",
  "detail": "Please login first"
}
```

### Hachage du mot de passe à la connexion
Le client envoie `hash_method` dans le corps de la requête de login. Valeurs acceptées :
- `CLAIR` → mot de passe en clair, comparé directement
- `MD5` → `md5(password).hexdigest()`
- `SHA256` → `sha256(password).hexdigest()`

La valeur hachée est comparée au champ `USW_PASS` de la table `USW_DAT`.

---

## 5. Endpoints REST

### Préfixe global : `/api`

---

### `GET /`
**Auth** : aucune  
**Description** : Vérifie que le processus API tourne.  
**Réponse 200** :
```json
{ "message": "Stock API is running" }
```

---

### `GET /api/health/api`
**Auth** : aucune  
**Description** : Retourne toujours 200 si le processus est vivant.  
**Réponse 200** :
```json
{
  "status": "healthy",
  "message": "API is running"
}
```

---

### `GET /api/health/database`
**Auth** : aucune  
**Description** : Teste la connexion SQL Server en exécutant `SELECT @@VERSION, GETDATE()`. Retourne la version du serveur et l'heure serveur si connecté.  
**Logique** :
1. `HealthRepository.check_database_connection()` exécute `SELECT @@VERSION as version, GETDATE() as server_time`
2. `HealthService` formate le résultat

**Réponse 200** :
```json
{
  "service": "Database Connection Test",
  "database_status": "connected",
  "details": {
    "version": "Microsoft SQL Server 2019 ...",
    "server_time": "2026-05-30 10:00:00"
  }
}
```
**Réponse 500** (connexion échouée) :
```json
{
  "status": "error",
  "message": "<message d'erreur>",
  "detail": "<message d'erreur>"
}
```

---

### `POST /api/login`
**Auth** : aucune  
**Description** : Authentifie un utilisateur. Retourne un JWT valide 24h.

**Corps de la requête** :
```json
{
  "login": "string",
  "password": "string",
  "hash_method": "CLAIR" | "MD5" | "SHA256"
}
```

**Logique** :
1. Valide `hash_method` (doit être `CLAIR`, `MD5` ou `SHA256`) → 400 sinon
2. Interroge `USW_DAT` : `SELECT USW_LOGN, USW_PASS FROM USW_DAT WHERE USW_LOGN = ?`
3. Si utilisateur non trouvé → 401
4. Hache le mot de passe fourni selon `hash_method`
5. Compare avec `USW_PASS` stocké → 401 si différent
6. Génère un JWT HS256 avec payload `{"login": login, "exp": now+24h}`
7. Stocke le token dans `active_tokens[token] = login`
8. Logue l'événement dans `logs/auth.log`

**Réponse 200 (succès)** :
```json
{
  "status": "success",
  "message": "Login successful",
  "token": "<jwt>",
  "expires_in": 86400
}
```
**Réponse 400 (hash_method invalide)** :
```json
{
  "status": "error",
  "message": "Invalid hash_method",
  "log_location": "logs/auth.log"
}
```
**Réponse 401 (login/password incorrect)** :
```json
{
  "status": "error",
  "message": "Invalid login or password",
  "log_location": "logs/auth.log"
}
```
**Réponse 500 (erreur DB ou JWT)** :
```json
{
  "status": "error",
  "message": "Database error" | "Token generation error",
  "log_location": "logs/auth.log"
}
```

---

### `GET /api/activities`
**Auth** : JWT requis (`Authorization: Bearer <token>`)  
**Description** : Retourne la liste de toutes les activités actives de la table `ACT_PAR`.

**Logique** :
1. Vérifie le token JWT (middleware `verify_token`)
2. Exécute : `SELECT ACT_KEYU, ACT_CODE, ACT_LIB FROM ACT_PAR WHERE ACT_ACTF = 1 ORDER BY ACT_LIB`
3. Retourne la liste des activités

**Réponse 200** :
```json
{
  "status": "success",
  "message": "Activities retrieved successfully",
  "activities": [
    {
      "act_keyu": 1,
      "act_code": "BKS",
      "act_lib": "Activité BKS"
    }
  ]
}
```
**Réponse 401** : voir format d'erreur global  
**Réponse 500** :
```json
{
  "status": "error",
  "message": "Failed to fetch activities",
  "activities": []
}
```

---

### `POST /api/stock/search`
**Auth** : JWT requis  
**Description** : Recherche des articles en stock filtrés par activité. Au moins un critère de recherche est obligatoire.

**Corps de la requête** :
```json
{
  "act_code": "BKS",         // obligatoire
  "art_code": "ABC123",      // optionnel (LIKE %valeur%)
  "stk_lieu": "A-01",        // optionnel (LIKE %valeur%)
  "stk_nosu": "SUPP-001"     // optionnel (LIKE %valeur%)
}
```

**Validation** : au moins un parmi `art_code`, `stk_lieu`, `stk_nosu` doit être non-vide.  
Les chaînes sont trimmées avant traitement.

**Logique** :
1. Vérifie le token JWT
2. Valide la requête (Pydantic)
3. Construit la requête SQL :
   ```sql
   SELECT ART_CODE, STK_LIEU, STK_NOSU, QUA_CODE, STK_QTE
   FROM STK_DAT
   WHERE ACT_CODE = ?
     AND (ART_CODE LIKE ? OR STK_LIEU LIKE ? OR STK_NOSU LIKE ?)
   ORDER BY ART_CODE, STK_LIEU
   ```
   - `ACT_CODE` est un filtre `AND` obligatoire
   - Les autres critères fournis sont combinés en `OR` avec `LIKE %valeur%`
   - Seuls les critères non-vides sont ajoutés à la clause WHERE

**Réponse 200 (résultats trouvés)** :
```json
{
  "status": "success",
  "message": "Found 3 item(s)",
  "items": [
    {
      "art_code": "ARTICLE-001",
      "stk_lieu": "A-01-01",
      "stk_nosu": "SUPP-12345",
      "qua_code": "GOOD",
      "stk_qte": 150
    }
  ]
}
```
**Réponse 200 (aucun résultat)** :
```json
{
  "status": "success",
  "message": "No stock items found matching criteria",
  "items": []
}
```
**Réponse 200 (erreur métier)** :
```json
{
  "status": "error",
  "message": "Search failed due to invalid criteria or database error",
  "items": []
}
```
**Réponse 401** : voir format d'erreur global

---

### `GET /api/stock/details/{sku}`
**Auth** : JWT requis  
**Description** : Retourne le détail du stock pour un SKU donné.  
**État** : **NON IMPLÉMENTÉ** — retourne un placeholder.

**Paramètre de chemin** : `sku` (string)

**Réponse 200 (placeholder actuel)** :
```json
{
  "status": "success",
  "message": "Details endpoint (not yet implemented)",
  "sku": "<sku>",
  "user": "<login>"
}
```

---

## 6. Tables SQL Server utilisées

| Table     | Colonnes lues                                         | Usage                              |
|-----------|-------------------------------------------------------|------------------------------------|
| `USW_DAT` | `USW_LOGN`, `USW_PASS`                                | Authentification utilisateur       |
| `ACT_PAR` | `ACT_KEYU`, `ACT_CODE`, `ACT_LIB`, `ACT_ACTF`        | Liste des activités (filtre `ACT_ACTF = 1`) |
| `STK_DAT` | `ACT_CODE`, `ART_CODE`, `STK_LIEU`, `STK_NOSU`, `QUA_CODE`, `STK_QTE` | Recherche de stock |

---

## 7. Format d'erreur global

Toutes les erreurs HTTP retournent le même enveloppe :
```json
{
  "status": "error",
  "message": "<description générale>",
  "detail": "<détail spécifique>"
}
```

---

## 8. CORS

Configuré globalement pour toutes les routes :
- Origines : valeur de `CORS_ORIGINS` (liste séparée par virgules)
- Méthodes : toutes (`*`)
- Headers : tous (`*`)
- Credentials : autorisés

---

## 9. Logging

Un fichier `logs/auth.log` est alimenté pour les événements d'authentification.

Format des lignes :
```
[2026-05-30 10:00:00] LOGIN_ATTEMPT login=jdoe hash=MD5 status=SUCCESS
[2026-05-30 10:01:00] AUTH_FAILED endpoint=protected token=<sha256[:16]> reason=EXPIRED
```

Raisons d'échec possibles : `MISSING`, `MALFORMED_HEADER`, `EXPIRED`, `INVALID_SIGNATURE`, `NOT_IN_ACTIVE_TOKENS`.  
Le token n'est jamais loggé brut — seuls les 16 premiers caractères de son SHA256 apparaissent dans les logs.

---

## 10. Architecture en couches (à reproduire en Go)

```
Route handler  →  Service  →  Repository  →  SQL Server
     ↑
  Middleware (verify_token)
```

- **Route** : reçoit la requête HTTP, délègue au service, retourne la réponse JSON
- **Service** : logique métier, orchestre les appels repository, gère les erreurs métier
- **Repository** : accès données, construit et exécute les requêtes SQL paramétrées, retourne des structs
- **Middleware** : vérification JWT injectable par route (`Depends` en FastAPI → middleware Fiber en Go)
