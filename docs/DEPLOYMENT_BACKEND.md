# 🚀 Guide de Déploiement - API Backend (Python FastAPI)

## 📋 Vue d'ensemble

Ce guide vous permet de déployer l'API Stock sur un **nouveau serveur** et de la connecter à votre **base de données SQL Server**.

**Temps estimé :** 20-30 minutes  
**Prérequis :** Windows/Linux avec accès à SQL Server

---

## 1️⃣ Prérequis

### Logiciels à installer

- **Python 3.10+** → https://www.python.org/downloads/
- **ODBC Driver 17 for SQL Server** → https://aka.ms/downloadodbc
- **Git** (optionnel) → https://git-scm.com/

### Vérification

```bash
python --version
# Résultat attendu : Python 3.10.x ou supérieur

pip --version
# Résultat attendu : pip 23.x ou supérieur
```

### Accès à la Base de Données

Vous avez besoin de :
- 🖥️ **Hostname du serveur SQL Server** (ex: `LAPVMI116\SQL2019`)
- 🔌 **Port** (défaut: `1433`)
- 📊 **Nom de la base de données** (ex: `POC_V8SpeedDeveloppement`)
- 👤 **Username** (ex: `sa`)
- 🔐 **Password** (ex: `BKS`)

---

## 2️⃣ Récupérer le Code

### Option A : Via Git (Recommandé)

```bash
git clone <repository-url>
cd POC_SPEED_RF_INFO_STOCK/backend
```

### Option B : Télécharger le ZIP

1. Téléchargez le ZIP du projet
2. Extrayez dans un dossier (ex: `C:\stock-api`)
3. Naviguez dans le dossier `backend`

```bash
cd C:\stock-api\backend
```

---

## 3️⃣ Configuration de l'Environnement (.env)

### Étape 1 : Créer le fichier `.env`

À la racine du dossier `backend/`, créez un fichier nommé `.env` avec votre configuration :

```
# Base de données SQL Server
SQL_SERVER_HOST=LAPVMI116\SQL2019
SQL_SERVER_PORT=1433
SQL_SERVER_DB=POC_V8SpeedDeveloppement
SQL_SERVER_USER=sa
SQL_SERVER_PASSWORD=BKS

# Configuration API
API_HOST=192.168.1.20
API_PORT=8000
API_RELOAD=false

# CORS (autoriser la communication Android)
CORS_ORIGINS=http://localhost:3000,http://10.0.2.2:3000
```

### 🔐 Sécurité

⚠️ **IMPORTANT** :
- 🚫 Ne **pas** commiter le `.env` en Git
- 🔒 Utiliser un gestionnaire de secrets en production (Azure Key Vault, etc.)
- 📝 Le fichier `.env` est dans `.gitignore` (fichier local uniquement)

### Variables Clés

| Variable | Exemple | Description |
|----------|---------|-------------|
| `SQL_SERVER_HOST` | `LAPVMI116\SQL2019` | Adresse du serveur SQL Server |
| `SQL_SERVER_PORT` | `1433` | Port SQL Server (défaut: 1433) |
| `SQL_SERVER_DB` | `POC_V8SpeedDeveloppement` | Nom de la base de données |
| `SQL_SERVER_USER` | `sa` | Utilisateur SQL Server |
| `SQL_SERVER_PASSWORD` | `BKS` | Mot de passe (REQUIS) |
| `API_HOST` | `0.0.0.0` ou `192.168.1.20` | Interface d'écoute de l'API |
| `API_PORT` | `8000` | Port de l'API |

---

## 4️⃣ Installation des Dépendances

### Windows

```bash
# Créer l'environnement virtuel
python -m venv venv

# Activer l'environnement
venv\Scripts\activate

# Installer les dépendances
pip install -r requirements.txt
```

### Linux/Mac

```bash
# Créer l'environnement virtuel
python3 -m venv venv

# Activer l'environnement
source venv/bin/activate

# Installer les dépendances
pip install -r requirements.txt
```

**Résultat attendu :**
```
Successfully installed fastapi-0.104.1 uvicorn-0.24.0 ...
```

---

## 5️⃣ Test de Connexion à la Base de Données

Avant de lancer l'API, vérifiez que vous pouvez accéder à SQL Server :

### Option A : Via Python (Recommandé)

```bash
# Vérifier la connexion
python

# Dans Python :
import pyodbc
try:
    conn = pyodbc.connect(
        'Driver={ODBC Driver 17 for SQL Server};'
        'Server=LAPVMI116\SQL2019;'
        'Database=POC_V8SpeedDeveloppement;'
        'UID=sa;'
        'PWD=BKS;'
    )
    print("✅ Connexion réussie!")
    conn.close()
except Exception as e:
    print(f"❌ Erreur: {e}")
```

### Option B : Via sqlcmd (si installé)

```bash
sqlcmd -S LAPVMI116\SQL2019 -U sa -P BKS -d POC_V8SpeedDeveloppement -Q "SELECT @@VERSION"
```

### Option C : Via Azure Data Studio (Interface graphique)

1. Téléchargez Azure Data Studio
2. Créez une connexion SQL Server
3. Testez la connexion

---

## 6️⃣ Lancer l'API

### Commande Simple

```bash
python main.py
```

### Résultat Attendu

```
INFO:     Uvicorn running on http://192.168.1.20:8000
INFO:     Press CTRL+C to quit
```

### Lancement Avancé (avec auto-reload)

```bash
# Mettre API_RELOAD=true dans .env
API_RELOAD=true python main.py

# OU directement
uvicorn main:app --host 192.168.1.20 --port 8000 --reload
```

---

## 7️⃣ Vérifier que l'API Fonctionne

### 1️⃣ Test Root Endpoint

```bash
curl http://192.168.1.20:8000/
```

**Résultat attendu :**
```json
{"message": "Stock API is running"}
```

### 2️⃣ Test Health Check

```bash
curl http://192.168.1.20:8000/api/health/database
```

**Résultat attendu :**
```json
{
  "service": "Database Connection Test",
  "database_status": "connected",
  "details": {
    "version": "Microsoft SQL Server 2019...",
    "server_time": "2026-05-22 14:30:45.123"
  }
}
```

### 3️⃣ Accéder à la Documentation Interactive

Ouvrez dans le navigateur :
```
http://192.168.1.20:8000/docs
```

Vous verrez :
- 📚 Tous les endpoints disponibles
- 🧪 Possibilité de tester chaque endpoint
- 📖 Documentation Swagger auto-générée

---

## 8️⃣ Configuration pour Accès Distant

### Si l'API doit être accessible depuis d'autres machines

**Modifier `.env` :**
```
API_HOST=0.0.0.0  # Écouter sur toutes les interfaces
API_PORT=8000
```

**Depuis l'Android :**
```
Adresse API = http://<IP-SERVEUR>:8000
```

### Exemple

Sur le serveur Windows :
```bash
# Obtenir l'adresse IP
ipconfig

# Résultat : IPv4 Address: 192.168.1.20
```

Sur le téléphone Android :
```
Paramètres → IP du serveur : 192.168.1.20
                 Port : 8000
```

---

## 9️⃣ Déploiement en Production (Azure/AWS)

### Sur Azure App Service

1. **Créer une App Service (Python 3.11)**
2. **Configurer les variables d'environnement** :
   - `SQL_SERVER_HOST` → Votre serveur SQL Azure
   - `SQL_SERVER_PASSWORD` → Secret dans Key Vault
   - `API_HOST` → `0.0.0.0`
3. **Déployer via Git ou ZIP**
4. **Configurer HTTPS** obligatoire

### Script de Déploiement (Azure)

```bash
az appservice plan create \
  --name stock-api-plan \
  --resource-group my-rg \
  --sku B1

az webapp create \
  --resource-group my-rg \
  --plan stock-api-plan \
  --name stock-api-app \
  --runtime "PYTHON|3.11"

# Configurer les variables d'environnement
az webapp config appsettings set \
  --resource-group my-rg \
  --name stock-api-app \
  --settings \
  SQL_SERVER_HOST=myserver.database.windows.net \
  SQL_SERVER_PASSWORD=@KeyVault(secret-name) \
  API_HOST=0.0.0.0
```

---

## 🔟 Troubleshooting

### ❌ Erreur : "SQL_SERVER_PASSWORD not found in environment"

**Solution :**
```bash
# Vérifier le .env existe
type .env

# Vérifier le contenu
cat .env

# Vérifier que vous avez activé venv
which python  # doit montrer le chemin vers venv/bin/python
```

### ❌ Erreur : "[ODBC Driver 17 for SQL Server] not found"

**Solution :**
```bash
# Windows : Installer ODBC Driver
# https://aka.ms/downloadodbc

# Linux : 
apt-get install odbc-mdbtools

# Mac :
brew install unixodbc
```

### ❌ Erreur : "Unable to connect to SQL Server"

**Causes possibles :**
- 🔐 Mauvais mot de passe
- 🖥️ Mauvais hostname (vérifier `SQL_SERVER_HOST`)
- 🌐 Firewall bloque le port 1433
- 📊 Base de données inexistante

**Vérification :**
```bash
# Tester la connectivité
telnet LAPVMI116 1433

# Test simple Python
python -c "import pyodbc; pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};Server=LAPVMI116\\SQL2019;UID=sa;PWD=BKS')"
```

### ❌ L'app Android ne voit pas l'API

**Vérifications :**
1. Téléphone et serveur sur **même réseau WiFi**
2. Firewall serveur autorise le port 8000
3. `API_HOST=0.0.0.0` dans `.env` (ou adresse IP)
4. Vérifier l'IP dans les paramètres Android

```bash
# Depuis le téléphone, faire un ping
ping 192.168.1.20

# Ou tester l'API
curl http://192.168.1.20:8000/
```

---

## 📝 Checklist de Déploiement

- ☑️ Python 3.10+ installé et vérifié
- ☑️ ODBC Driver 17 installé
- ☑️ Fichier `.env` créé avec bonnes valeurs
- ☑️ Environnement virtuel activé (`venv`)
- ☑️ Dépendances installées (`pip install -r requirements.txt`)
- ☑️ Connexion à SQL Server vérifiée
- ☑️ API lancée sans erreur
- ☑️ Tests curl réussis (`/` et `/api/health/database`)
- ☑️ Documentation Swagger accessible (`/docs`)
- ☑️ Téléphone sur même réseau peut atteindre l'API

---

## 📞 Support

Si vous avez des problèmes, vérifiez :

1. **Les logs de la console** (messages d'erreur spécifiques)
2. **Le fichier .env** (valeurs correctes)
3. **La connectivité réseau** (ping, telnet)
4. **Les permissions ODBC** (utilisateur SQL Server correct)

Consultez `docs/architecture.html` pour plus de détails techniques sur l'API.
