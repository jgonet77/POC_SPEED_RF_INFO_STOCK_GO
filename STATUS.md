# Statut du projet — POC Stock Mobile

**Dernière mise à jour :** 30 mai 2026  
**Phase courante :** Migration backend Go — **TERMINÉE ET FONCTIONNELLE**

---

## ✅ Ce qui est fait

### Backend Go (Fiber)
- [x] Génération complète via workflow multi-agents Opus 4.8 (9 agents, ~15 min, 324K tokens)
- [x] Architecture en couches : Routes → Services → Repositories
- [x] Connexion SQL Server via `go-mssqldb` (TCP/IP activé sur `LAPVMI116\SQL2019`)
- [x] Authentification JWT HS256, 24h, store en mémoire thread-safe
- [x] Hachage mot de passe : CLAIR / MD5 / SHA256
- [x] `POST /api/login` — authentification ✅ testé
- [x] `GET /api/activities` — liste activités depuis `ACT_PAR` ✅ testé
- [x] `POST /api/stock/search` — recherche stock dans `STK_DAT` ✅ testé
- [x] `GET /api/health/api` — liveness probe ✅ testé
- [x] `GET /api/health/database` — test connexion SQL Server ✅ testé
- [x] `GET /api/stock/details/:sku` — placeholder (non implémenté)
- [x] Swagger UI sur `/swagger/index.html`
- [x] Logger HTTP Fiber (toutes les requêtes loguées avec IP source)
- [x] Graceful shutdown (SIGTERM/SIGINT)
- [x] CORS configuré via `.env`
- [x] Compilation en binaire unique `stock-api.exe`

### Android
- [x] App connectée au backend Go depuis le téléphone (Samsung SM-A566B)
- [x] Fix réseau : build variants debug/release (cleartext autorisé en debug)
- [x] Login + activités + recherche stock fonctionnels depuis le téléphone

### Documentation
- [x] `MIGRATION.md` — documentation exhaustive du backend Python (référence migration)
- [x] `CLAUDE.md` — mis à jour pour refléter la stack Go
- [x] `docs/DEPLOYMENT_BACKEND_GO.md` — guide d'installation complet (Go, TCP SQL Server, .env, pare-feu)
- [x] `docs/migration-python-go.html` — rapport technique comparatif Python vs Go
- [x] `docs/DEPLOYMENT_ANDROID.md` — guide déploiement Android

---

## 🔲 Ce qui reste à faire

### Priorité haute
- [ ] **Implémenter `GET /api/stock/details/:sku`** — retourne les détails complets d'un article (quantité, emplacement, lot, date expiration, état)
- [ ] **Tests unitaires Go** — services (stock, health) et repositories (auth, stock, activity)
- [ ] **Supprimer `backend/`** — le code Python de référence peut être archivé une fois les tests Go écrits

### Priorité moyenne
- [ ] **Compte SQL dédié** — remplacer `sa` par un compte avec droits `SELECT` uniquement sur `USW_DAT`, `ACT_PAR`, `STK_DAT`
- [ ] **HTTPS** — certificat auto-signé ou Let's Encrypt pour sécuriser les échanges mobile ↔ API
- [ ] **Scanner code-barres Android** — intégrer la caméra pour scanner un SKU (Phase 2)
- [ ] **Recherche multi-critères Android** — l'UI de `StockSearchActivity` n'exploite pas encore `stk_lieu` et `stk_nosu`

### Priorité basse / Phase 2
- [ ] Pool de connexions SQL Server — actuellement 10 connexions max, à ajuster selon la charge
- [ ] Logs structurés (JSON) pour faciliter la supervision
- [ ] Historique des recherches côté Android
- [ ] Mode hors-ligne partiel (cache local)
- [ ] Google Play Store — signature APK release, publication

---

## Infrastructure et configuration

| Élément | Valeur |
|---|---|
| Serveur API | `LAPVMI116` / `192.168.1.20:8000` |
| SQL Server | `LAPVMI116\SQL2019` — base `POC_V8SpeedDeveloppement` |
| TCP SQL Server | Activé (port 1433 fixe) |
| Backend Go | `backend-go/` — module `stock-api` |
| Backend Python | `backend/` — conservé comme référence uniquement |
| Android | `android/` — Samsung SM-A566B testé |

---

## Notes techniques importantes

**SQL Server TCP** : go-mssqldb utilise TCP uniquement (pas de Named Pipes comme pyodbc). TCP doit être activé dans SQL Server Configuration Manager sur `LAPVMI116\SQL2019`. Voir `docs/DEPLOYMENT_BACKEND_GO.md` section 2.

**Android cleartext** : Le build Debug autorise HTTP en clair vers toute adresse (home + bureau). Changer l'IP du serveur dans les Paramètres de l'app selon le réseau. Le build Release exige HTTPS.

**Colonne STK_QTE** : Déclarée `decimal` en base, castée en `INT` dans la requête SQL du `stock_repository.go` pour correspondre au type Go/Android.

**DSN instance nommée** : go-mssqldb attend `?instance=SQL2019` en query param, pas `/SQL2019` dans le host (qui causerait un encodage `%2F`).
