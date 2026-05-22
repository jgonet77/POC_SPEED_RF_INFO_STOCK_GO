# CLAUDE.md - POC Stock Mobile (Android + API Python)

## 🎯 Vision du projet

POC d'application mobile Android pour permettre aux préparateurs/caristes de consulter le stock de l'entrepôt en temps réel via une API REST Python, connectée au WMS SPEED (SQL Server).

**Objectif:** Valider la faisabilité technique (connexion mobile ↔ BD, authentification, performance, scan code-barres).

---

## 📋 Stack technique

- **Frontend:** Android natif (Kotlin)
- **Backend:** Python (FastAPI) - Architecture en couches (API → Service → Repository)
- **Base de données:** Microsoft SQL Server (WMS SPEED)
- **Infrastructure:** Local (accessible depuis le réseau local du téléphone)

---

## 🏗️ Architecture (Approche 2 - Layered)

```
┌─────────────────────────────────────────────┐
│         ANDROID KOTLIN APP                  │
│  (UI: Accueil, Recherche, Détails stock)   │
└─────────────┬───────────────────────────────┘
              │ HTTP/REST (Retrofit)
              ↓
┌─────────────────────────────────────────────┐
│      PYTHON FASTAPI SERVER (Couches)        │
├─────────────────────────────────────────────┤
│  API Layer (Routes/Controllers)             │
├─────────────────────────────────────────────┤
│  Service Layer (Logique métier)             │
├─────────────────────────────────────────────┤
│  Repository Layer (Accès données)           │
└─────────────┬───────────────────────────────┘
              │ pyodbc (SQL Server Driver)
              ↓
┌─────────────────────────────────────────────┐
│      MICROSOFT SQL SERVER                   │
│      WMS SPEED (Tables stock)               │
└─────────────────────────────────────────────┘
```

---

## 📁 Structure du projet

```
POC_SPEED_RF_INFO_STOCK/
├── docs/
│   ├── FONCTIONNEL.md         # Specs fonctionnelles (simple)
│   ├── TECHNIQUE.md            # Specs techniques (architecture)
│   └── superpowers/specs/      # Design docs signés
├── backend/
│   ├── main.py                 # Point d'entrée FastAPI
│   ├── config.py               # Configuration (DB, env)
│   ├── models/                 # Pydantic models (request/response)
│   ├── routes/                 # API endpoints
│   ├── services/               # Logique métier
│   ├── repositories/           # Accès données
│   ├── schemas/                # Structures de données
│   └── requirements.txt         # Dépendances Python
├── android/
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/stockapp/
│   │   │   ├── MainActivity.kt
│   │   │   ├── models/         # Data classes
│   │   │   ├── api/            # Retrofit services
│   │   │   ├── ui/             # Activities et composants
│   │   │   ├── viewmodels/     # ViewModels
│   │   │   └── repositories/   # Data repositories
│   │   └── res/                # Resources (layout, string, etc.)
│   └── build.gradle
└── CLAUDE.md                    # Ce fichier
```

---

## ✨ Conventions de code

Voir le CLAUDE.md global. Adaptations spécifiques:

### Backend (Python)

- **Repository Pattern** : Abstraction de l'accès données
- **Service Layer** : Logique métier indépendante de la persistance
- **Validation** : Pydantic models pour validation des requêtes
- **Nommage** : `snake_case` pour fonctions/variables Python
- **Erreurs** : Exceptions custom par domaine (AuthError, StockError, etc.)

### Frontend (Android/Kotlin)

- **MVVM Pattern** : ViewModel + Repository
- **Retrofit** : HTTP client typé
- **LiveData** : Observable pour UI updates
- **Nommage** : `PascalCase` pour classes, `camelCase` pour propriétés
- **Coroutines** : Async/await pour requêtes réseau

---

## 🔐 Authentification

1. Utilisateur saisit login/password sur l'écran de connexion
2. API valide contre la table `Utilisateurs` du SQL Server
3. API retourne un token simple (session ID) OU une liste d'activités associées
4. App stocke localement (SharedPreferences) pour les futures requêtes
5. Chaque requête inclut le token en header

**Données requises** (à préciser avec la doc des tables) :
- Table: `Utilisateurs` (login, password, id_utilisateur)
- Table: `Activites_Utilisateur` (lien utilisateur ↔ activités)

---

## 📊 Flux fonctionnel principal

1. **Login** : Authentification utilisateur + sélection d'activité
2. **Accueil** : Liste des options (Rechercher, Historique, Paramètres)
3. **Recherche** :
   - Saisie SKU OU Scan code-barres
   - Requête `/api/stock/search?sku=...`
   - Affichage des résultats
4. **Détails** :
   - Click sur un résultat
   - Requête `/api/stock/{sku}/details`
   - Affichage : Quantité + Localisation + Infos additionnelles (lot, date exp, état)

---

## 🧪 Testing

- **Backend** : Tests unitaires (services), tests intégration (repositories)
- **Frontend** : Tests unitaires (ViewModels), tests UI (Espresso)
- **E2E** : Scénario complet login → recherche → détails

Pas de TDD strict pour le POC, mais coverage ≥ 80% attendue pour le code critique.

---

## 🚀 Déploiement POC

**Phase 1 (Validation technique):**
- API: Tourne sur localhost:8000 (accessible via USB ou WiFi local)
- App: Testée sur appareil physique connecté au même réseau
- BD: SQL Server accessible depuis la machine de dev

**Phase 2 (si POC réussit):**
- API: Hosting Azure/AWS
- App: Google Play Store
- BD: Configuration production WMS SPEED

---

## 📞 Points de contact

- **WMS SPEED DB** : [À fournir : host, port, authentification, tables]
- **Utilisateurs cibles** : Préparateurs/caristes
- **Modèle recommandé** : Sonnet 4.6 pour specs, Opus 4.7 pour dev

---

## ✅ Checklist de démarrage

- [ ] CLAUDE.md du projet validé ✓
- [ ] Documentation fonctionnelle révisée
- [ ] Documentation technique révisée
- [ ] Accès à la doc des tables SQL Server (Utilisateurs, Stock, Emplacements, etc.)
- [ ] Environnement de dev setup (Python 3.10+, Android Studio, Git)
- [ ] Première itération de l'API (Login + Search endpoint)
