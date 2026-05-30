# Installation et déploiement — Backend Go (Fiber)

Guide complet pour installer le serveur stock API sur un nouveau PC Windows.

---

## Vue d'ensemble

```
Nouveau PC Windows
      │
      ├─ 1. Installer Go 1.22+
      ├─ 2. Activer TCP/IP sur SQL Server
      ├─ 3. Récupérer le code source (git clone)
      ├─ 4. Créer le fichier .env
      ├─ 5. go mod tidy  (télécharge les dépendances)
      └─ 6. go run .     (lancer le serveur)
```

---

## Étape 1 — Installer Go

1. Télécharger le **Windows installer (.msi)** sur **https://go.dev/dl/**  
   → choisir la version **1.22.x** ou supérieure (amd64)

2. Lancer le `.msi` et suivre l'assistant (installation par défaut)  
   → Go s'installe dans `C:\Program Files\Go\`  
   → Le PATH est mis à jour automatiquement

3. **Ouvrir un nouveau terminal PowerShell** (le PATH n'est pris en compte que dans les nouveaux terminaux)

4. Vérifier :
   ```powershell
   go version
   # go version go1.22.x windows/amd64
   ```

---

## Étape 2 — Activer TCP/IP sur SQL Server

> **Obligatoire.** Le driver Go (`go-mssqldb`) utilise uniquement TCP/IP.  
> pyodbc (Python) peut se connecter via Named Pipes, mais Go ne le supporte pas.  
> Cette étape est à faire **une seule fois** sur la machine SQL Server.

### 2.1 Activer TCP/IP dans SQL Server Configuration Manager

1. Ouvrir **SQL Server Configuration Manager**  
   *(Démarrer → taper "SQL Server Configuration Manager")*

2. Dans l'arbre gauche :  
   **SQL Server Network Configuration → Protocols for `NOM_INSTANCE`**  
   *(ex. : Protocols for SQL2019)*

3. Double-cliquer sur **TCP/IP**

4. Onglet **Protocol** :
   - `Enabled` → **Yes**

5. Onglet **IP Addresses** → descendre jusqu'à la section **IPAll** :
   - `TCP Dynamic Ports` → **vider le champ** (effacer toute valeur)
   - `TCP Port` → **1433**

6. Cliquer **OK**

7. Dans l'arbre gauche :  
   **SQL Server Services → SQL Server (`NOM_INSTANCE`)** → clic droit → **Restart**

### 2.2 Activer SQL Server Browser

Le Browser permet aux clients de découvrir le port d'une instance nommée.

Dans l'arbre gauche :  
**SQL Server Services → SQL Server Browser** → clic droit → **Start**  
*(si l'état est "Stopped" : clic droit → Properties → Service → Start Mode → Automatic, puis Start)*

### 2.3 Vérifier depuis PowerShell

```powershell
Test-NetConnection -ComputerName "NOM_SERVEUR" -Port 1433
# TcpTestSucceeded : True  ← attendu
```

---

## Étape 3 — Récupérer le code source

```powershell
git clone https://github.com/jgonet77/POC_SPEED_RF_INFO_STOCK_GO.git
cd POC_SPEED_RF_INFO_STOCK_GO\backend-go
```

---

## Étape 4 — Créer le fichier `.env`

Dans le dossier `backend-go\`, créer un fichier nommé `.env` avec ce contenu :

```env
# ── Base de données SQL Server ──────────────────────────────────────────────
# Format instance nommée : SERVEUR\INSTANCE  (ex: LAPVMI116\SQL2019)
# Format instance défaut : SERVEUR           (ex: 192.168.1.20)
SQL_SERVER_HOST=LAPVMI116\SQL2019

# Port TCP de SQL Server (1433 = port standard)
SQL_SERVER_PORT=1433

# Nom de la base de données
SQL_SERVER_DB=POC_V8SpeedDeveloppement

# Compte SQL Server (doit avoir les droits SELECT sur USW_DAT, ACT_PAR, STK_DAT)
SQL_SERVER_USER=sa
SQL_SERVER_PASSWORD=BKS

# ── Serveur API ──────────────────────────────────────────────────────────────
# 0.0.0.0    = accessible depuis tout le réseau (recommandé pour les tests mobiles)
# 127.0.0.1  = local uniquement
# 192.168.x.x = interface réseau spécifique
API_HOST=0.0.0.0
API_PORT=8000

# ── Sécurité JWT ─────────────────────────────────────────────────────────────
# Changer cette valeur en production (chaîne longue et aléatoire)
SECRET_KEY=dev-secret-key-change-in-prod

# ── CORS ─────────────────────────────────────────────────────────────────────
# Origines autorisées à appeler l'API (séparées par virgule)
# 10.0.2.2 = adresse de la machine hôte depuis l'émulateur Android
CORS_ORIGINS=http://localhost:3000,http://10.0.2.2:3000

# ── Logs ─────────────────────────────────────────────────────────────────────
LOG_PATH=logs/auth.log
```

> Le fichier `.env` est lu automatiquement au démarrage.  
> Ne pas le committer en Git (il contient le mot de passe).

---

## Étape 5 — Télécharger les dépendances

```powershell
cd backend-go
go mod tidy
```

Cette commande télécharge tous les modules Go nécessaires et génère le fichier `go.sum`.  
Connexion internet requise (une seule fois).

---

## Étape 6 — Lancer le serveur

### Mode développement (recompile à chaque lancement)

```powershell
cd backend-go
go run .
```

### Mode production (compiler puis exécuter)

```powershell
cd backend-go
go build -o stock-api.exe .
.\stock-api.exe
```

### Sortie attendue au démarrage

```
 ┌───────────────────────────────────────────────────┐
 │                   Fiber v2.52.5                   │
 │               http://0.0.0.0:8000                 │
 │       (bound on host 0.0.0.0 and port 8000)       │
 │                                                   │
 │ Handlers ............ 18  Processes ........... 1 │
 └───────────────────────────────────────────────────┘
2026/05/30 13:35:57 Starting server on 0.0.0.0:8000
```

> Si un `WARNING: database not reachable at startup` apparaît, vérifier l'étape 2.  
> Le serveur démarre quand même — seuls les endpoints SQL retourneront une erreur 500.

---

## Étape 7 — Vérifier que tout fonctionne

Ouvrir un second terminal et tester dans l'ordre :

```powershell
$base = "http://localhost:8000"

# 1. Santé de l'API (ne nécessite pas SQL Server)
Invoke-RestMethod "$base/api/health/api"
# → {"status":"healthy","message":"API is running"}

# 2. Connexion SQL Server
Invoke-RestMethod "$base/api/health/database"
# → {"service":"Database Connection Test","database_status":"connected",...}

# 3. Login
$r = Invoke-RestMethod "$base/api/login" -Method POST -ContentType "application/json" `
     -Body '{"login":"bks","password":"BKS","hash_method":"CLAIR"}'
$token = $r.token
Write-Host "Token: $token"

# 4. Liste des activités (token requis)
Invoke-RestMethod "$base/api/activities" -Headers @{Authorization="Bearer $token"}

# 5. Recherche stock
Invoke-RestMethod "$base/api/stock/search" -Method POST -ContentType "application/json" `
  -Headers @{Authorization="Bearer $token"} `
  -Body '{"act_code":"BKS","art_code":"A"}'
```

---

## Pare-feu Windows

Si l'API doit être accessible depuis d'autres machines (téléphone Android, autre PC) :

```powershell
# Ouvrir le port de l'API (à lancer en PowerShell administrateur)
New-NetFirewallRule -DisplayName "Stock API Go 8000" `
  -Direction Inbound -Protocol TCP -LocalPort 8000 -Action Allow

# Ouvrir le port SQL Server (si accès distant à la BD nécessaire)
New-NetFirewallRule -DisplayName "SQL Server 1433" `
  -Direction Inbound -Protocol TCP -LocalPort 1433 -Action Allow

# Ouvrir le port SQL Server Browser (découverte instance nommée)
New-NetFirewallRule -DisplayName "SQL Server Browser 1434" `
  -Direction Inbound -Protocol UDP -LocalPort 1434 -Action Allow
```

---

## Résolution des problèmes

| Symptôme | Cause | Solution |
|----------|-------|----------|
| `go: command not found` | Go non installé ou PATH non rechargé | Installer Go, ouvrir un **nouveau** terminal |
| `panic: SQL_SERVER_PASSWORD is required` | Fichier `.env` absent ou dans le mauvais dossier | Créer `.env` dans `backend-go\` |
| `WARNING: database not reachable` + `unable to open tcp connection` | TCP désactivé sur SQL Server | Suivre l'**étape 2** |
| `WARNING: database not reachable` + `connectex: No connection could be made` | SQL Server non démarré ou pare-feu | Démarrer le service SQL Server, ouvrir le port 1433 |
| `bind: The requested address is not valid` | `API_HOST` pointe sur une IP absente de cette machine | Mettre `API_HOST=0.0.0.0` dans `.env` |
| `bind: Only one usage of each socket address` | Port 8000 déjà occupé par un autre processus | Identifier et arrêter le processus : `Get-NetTCPConnection -LocalPort 8000` |
| Login retourne `Invalid login or password` | Identifiants incorrects **ou** mot de passe haché | Vérifier login/password dans `USW_DAT`, adapter `hash_method` (CLAIR/MD5/SHA256) |
| Recherche stock retourne `error` | Colonnes SQL inattendues (types, NULLs) | Vérifier les colonnes de `STK_DAT` avec `INFORMATION_SCHEMA.COLUMNS` |
