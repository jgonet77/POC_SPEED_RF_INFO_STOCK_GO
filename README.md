# 📱 POC Stock App - Android + Python API

Application mobile de gestion de stock en temps réel, connectée à SQL Server via API FastAPI.

**Status:** ✅ POC Fonctionnel | **Version:** 0.1.0 | **Date:** Mai 2026

---

## 📊 Vue d'Ensemble

```
┌─────────────────────────────────────────────────────────┐
│  Android App (Kotlin) ← HTTP REST → Python FastAPI   │
│                         (Port 8000)                    │
│                                 ↓                       │
│                         SQL Server WMS                 │
└─────────────────────────────────────────────────────────┘
```

- ✅ **Frontend:** Android natif (Kotlin)
- ✅ **Backend:** Python FastAPI (Architecture en couches)
- ✅ **Database:** Microsoft SQL Server (WMS SPEED)
- ✅ **Pattern:** MVVM (Mobile) + Repository Pattern (Backend)

---

## 🚀 Installation Rapide (10 minutes)

### Pour les Pressés

```bash
# Backend
cd backend
.\setup.bat          # Windows
# OR
./setup.sh           # Linux/Mac

# Éditer .env avec vos paramètres SQL Server
# Lancer
python main.py
```

```bash
# Android
# Installer l'APK depuis android/app/build/outputs/apk/debug/
# OU compiler depuis Android Studio
cd android
./gradlew clean build
```

**👉 Guide complet:** Voir `docs/QUICKSTART.md`

---

## 📚 Documentation

### Pour Commencer

| Document | Temps | Contenu |
|----------|-------|---------|
| [QUICKSTART.md](docs/QUICKSTART.md) | 10 min | Installation rapide |
| [DEPLOYMENT_BACKEND.md](docs/DEPLOYMENT_BACKEND.md) | 20 min | Setup API complet |
| [DEPLOYMENT_ANDROID.md](docs/DEPLOYMENT_ANDROID.md) | 15 min | Setup Android complet |

### Pour Comprendre

| Document | Contenu |
|----------|---------|
| [index.html](docs/index.html) | 📊 Vue générale du projet |
| [architecture.html](docs/architecture.html) | 🏗️ Architecture système détaillée |
| [etape-1-7.html](docs/etape-*-*.html) | 🔧 7 étapes de développement |
| [comparaison-detaillee.html](docs/comparaison-detaillee.html) | ⚖️ Android vs Web analysis |

---

## 📁 Structure du Projet

```
POC_SPEED_RF_INFO_STOCK/
│
├── backend/                          # API Python FastAPI
│   ├── main.py                       # Point d'entrée
│   ├── config.py                     # Configuration (varenv)
│   ├── requirements.txt               # Dépendances Python
│   ├── setup.bat / setup.sh          # Scripts d'installation
│   ├── .env                          # Configuration (local)
│   │
│   ├── models/
│   │   └── schemas.py                # Pydantic models
│   ├── routes/
│   │   └── health.py                 # Endpoints API
│   ├── services/
│   │   └── health_service.py         # Logique métier
│   ├── repositories/
│   │   ├── base_repository.py        # Accès DB (base)
│   │   └── health_repository.py      # Accès DB (health)
│   └── tests/                        # Tests unitaires
│
├── android/                          # App Android Kotlin
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/stockapp/
│   │   │   │   ├── MainActivity.kt           # Écran principal
│   │   │   │   ├── SettingsActivity.kt      # Paramètres
│   │   │   │   ├── LogsViewerActivity.kt    # Viewer logs
│   │   │   │   ├── models/                  # Data classes
│   │   │   │   ├── api/                     # Retrofit client
│   │   │   │   ├── repositories/            # Data layer
│   │   │   │   ├── viewmodels/              # MVVM ViewModel
│   │   │   │   └── config/                  # Configuration
│   │   │   └── res/                         # Resources
│   │   └── build.gradle               # Config Android
│   └── local.properties               # SDK path (local)
│
└── docs/                             # Documentation
    ├── QUICKSTART.md                 # 10-min quickstart
    ├── DEPLOYMENT_BACKEND.md         # Setup backend
    ├── DEPLOYMENT_ANDROID.md         # Setup android
    ├── index.html                    # Overview
    ├── architecture.html             # Architecture détaillée
    ├── etape-1-diagnostic.html      # Étape 1: Crash fix
    ├── etape-2-api-network.html     # Étape 2: API + Network
    ├── etape-3-ui-connection.html   # Étape 3: UI
    ├── etape-4-settings.html        # Étape 4: Settings
    ├── etape-5-state-management.html # Étape 5: State
    ├── etape-6-styling-ux.html      # Étape 6: Styling
    ├── etape-7-timing-logs.html     # Étape 7: Timing
    └── comparaison-detaillee.html   # Android vs Web
```

---

## 🔧 Prérequis

### Backend

- ✅ Python 3.10+
- ✅ ODBC Driver 17 for SQL Server
- ✅ Accès à base de données SQL Server

### Android

- ✅ Android Studio (optionnel, pour compiler)
- ✅ Téléphone Android 8.0+ (pour exécuter)
- ✅ Java 11+ (pour compiler)

---

## 📝 Configuration Rapide

### Backend (.env)

```bash
# SQL Server
SQL_SERVER_HOST=LAPVMI116\SQL2019
SQL_SERVER_PORT=1433
SQL_SERVER_DB=POC_V8SpeedDeveloppement
SQL_SERVER_USER=sa
SQL_SERVER_PASSWORD=YOUR_PASSWORD

# API
API_HOST=0.0.0.0
API_PORT=8000
```

### Android (Settings)

```
Écran Principal → ⚙️ Settings
└─ Serveur API: 192.168.1.20
└─ Port: 8000
```

---

## 🧪 Tests

### Backend

```bash
cd backend
python -m pytest tests/
```

### Android

```bash
cd android
./gradlew test
```

---

## 📊 Endpoints API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Health check API |
| GET | `/api/health/api` | Test API health |
| GET | `/api/health/database` | Test Database health |
| GET | `/docs` | Swagger documentation |

---

## 🔐 Sécurité

⚠️ **Important**: Ce POC utilise HTTP local non chiffré.

### Pour la Production

1. **HTTPS obligatoire** - Certificats SSL valides
2. **Authentification** - JWT tokens
3. **CORS restreint** - Origines autorisées uniquement
4. **Secrets sécurisés** - Azure Key Vault / AWS Secrets Manager

Voir `DEPLOYMENT_BACKEND.md` pour détails de sécurité.

---

## 📈 Métriques du Projet

| Métrique | Valeur |
|----------|--------|
| Temps de développement | ~15 heures |
| Jours calendaires | 3 jours |
| Nombre d'activities Android | 4 |
| Couches backend | 3 (Routes, Services, Repositories) |
| Endpoints API | 3 |
| Fichiers de documentation | 10+ |

---

## 🎯 Cas d'Usage

### ✅ Ce que Cela Peut Faire

- 📱 Afficher l'état de connexion API
- 🧪 Tester la connexion à SQL Server
- ⏱️ Mesurer les temps de réponse
- 📊 Voir les détails de la base de données
- ⚙️ Configurer l'IP/Port du serveur
- 📋 Consulter les logs API

### ❌ Ce que Cela Ne Fait Pas (POC)

- 🔐 Pas d'authentification
- 📦 Pas de gestion de stock complète
- 🔍 Pas de recherche SKU
- 📱 Pas de scan code-barres
- 🌐 Pas de synchronisation offline

Ces fonctionnalités peuvent être ajoutées en future.

---

## 🚀 Prochaines Étapes (Feuille de Route)

### Phase 2 (Court terme: 1-2 semaines)

- [ ] #15 SharedPreferences Manager (config persistante)
- [ ] #16 File Logger System (logs sur disque)
- [ ] #17 Améliorer Settings Activity

### Phase 3 (Moyen terme: 1-2 mois)

- [ ] Endpoint `/api/stock/search?sku=...`
- [ ] Écran de recherche SKU
- [ ] Écran de détails stock (quantités, localisations)
- [ ] Authentification JWT

### Phase 4 (Long terme: 2-3 mois)

- [ ] Scanner code-barres intégré
- [ ] Synchronisation offline (SQLite)
- [ ] Notifications push (Firebase)
- [ ] Google Play Store publication

---

## 📞 Support & Questions

### Documentation

- 📖 Lire les guides HTML dans `docs/`
- 📝 Consulter les commentaires de code
- 🧪 Regarder les tests pour des exemples

### Troubleshooting

1. **L'API ne démarre pas?**
   - Vérifier le `.env` et les paramètres SQL Server
   - Voir `DEPLOYMENT_BACKEND.md` → Troubleshooting

2. **L'app Android se ferme?**
   - Vérifier `View Logs` pour les erreurs
   - Voir `DEPLOYMENT_ANDROID.md` → Troubleshooting

3. **Connexion API impossible?**
   - Vérifier que téléphone et serveur sont sur le même réseau
   - Tester: `ping <IP-SERVEUR>`
   - Vérifier le firewall port 8000

---

## 👨‍💻 À Propos

**Développé:** Mai 2026  
**Statut:** POC Réussi ✅  
**Objectif:** Valider la faisabilité technique d'une app mobile de gestion de stock

---

## 📄 Licence

Ce projet est fourni à titre d'exemple POC. Adaptez-le selon vos besoins.

---

## 🎉 Bonne Utilisation!

**Commencez par:** 👉 [`docs/QUICKSTART.md`](docs/QUICKSTART.md)

Vous aurez une app fonctionnelle en 10 minutes! 🚀
