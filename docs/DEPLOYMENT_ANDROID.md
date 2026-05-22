# 📱 Guide de Déploiement - Application Android

## 📋 Vue d'ensemble

Ce guide vous permet d'installer l'application Stock sur un **téléphone Android** et de la configurer pour se connecter à votre **API Backend**.

**Temps estimé :** 15-20 minutes  
**Prérequis :** Android 8.0+ (API 26+)

---

## 1️⃣ Prérequis

### Sur votre Ordinateur

- **Android Studio** (optionnel si vous avez l'APK pré-compilé)
- **ADB (Android Debug Bridge)**
- **USB drivers** pour votre téléphone (Windows)

### Sur votre Téléphone

- **Android 8.0 ou supérieur** (API 26+)
- **Connectivité WiFi**
- **Espace disque** : ~50 MB minimum
- **Mode Développeur** activé (optionnel, pour ADB)

---

## 2️⃣ Option A : Installation via APK Pré-compilé (Plus Simple)

### Étape 1 : Obtenir l'APK

L'APK compilé se trouve à :
```
POC_SPEED_RF_INFO_STOCK/android/app/build/outputs/apk/debug/app-debug.apk
```

**Si le dossier `build` n'existe pas **, consultez la **Section 3** pour compiler.

### Étape 2 : Transférer l'APK sur le Téléphone

#### Via USB (Recommandé)

```bash
# Windows
adb connect <PHONE-IP>:<PORT>
adb push app-debug.apk /sdcard/

# Or via fichier
# 1. Connecter téléphone en USB
# 2. Activer transfert de fichiers
# 3. Copier app-debug.apk dans Downloads/

```

#### Via Email/Cloud

1. Envoyer l'APK par email à vous-même
2. Télécharger sur le téléphone
3. L'APK apparaît automatiquement

### Étape 3 : Installer l'APK

**Via Explorateur de Fichiers (Téléphone)**

1. Ouvrir l'app **Fichiers** (Files)
2. Naviguer vers le dossier contenant `app-debug.apk`
3. Appuyer sur le fichier APK
4. Appuyer sur **Installer**
5. Accepter les permissions

**Résultat attendu :**
- ✅ "Installation réussie"
- L'app apparaît dans le drawer (liste des apps)

---

## 3️⃣ Option B : Compiler l'APK Vous-Même (Advanced)

### Prérequis

- **Android Studio** installé
- **Java 11+**
- **Gradle** (géré par Android Studio)

### Étapes

```bash
# 1. Ouvrir le projet dans Android Studio
Open → POC_SPEED_RF_INFO_STOCK/android

# 2. Attendre le sync Gradle (peut prendre 5-10 min)
# Menu : File → Sync Now

# 3. Compiler l'APK
# Menu : Build → Build Bundle(s) / APK(s) → Build APK(s)

# 4. Attendre la compilation (~3 min)
# Message : "Built successfully"

# 5. L'APK est généré à :
# android/app/build/outputs/apk/debug/app-debug.apk
```

### Si Erreurs de Compilation

```bash
# Nettoyer et reconstruire
./gradlew clean build

# Vérifier les dépendances
./gradlew dependencies

# Vérifier que le SDK Android est à jour
# Android Studio → SDK Manager → Installer API 26+ et Android Gradle Plugin 7.x+
```

---

## 4️⃣ Configuration de l'Application

### Première Utilisation

1. **Ouvrir l'application** (icône sur l'écran d'accueil)
2. **L'app tente automatiquement** de se connecter à `192.168.1.20:8000`

### Si Cela Ne Fonctionne Pas

**Aller aux Paramètres** :

1. Appuyer sur le bouton **⚙️ Settings**
2. Champs qui apparaissent :
   - **Serveur API** : `192.168.1.20` (adapter à votre IP)
   - **Port API** : `8000`

3. Appuyer sur **Enregistrer**
4. Retourner à l'écran principal
5. L'app devrait afficher "Connected ✅"

---

## 5️⃣ Vérification de la Connexion

### Sur l'Application

✅ Écran principal doit afficher :

```
Connected ✅
API: 192.168.1.20:8000
```

### Test des Endpoints

Appuyer sur les boutons :

1. **Test API** : Doit afficher ✅ healthy
2. **Test Database** : Doit afficher ✅ connected + version

**Résultat attendu :**

```
API Health Status
✅ healthy
Completed in 125ms

Database Health Status
✅ connected
Version: Microsoft SQL Server 2019 ...
Server Time: 2026-05-22 14:30:45
Completed in 250ms
```

### Consulter les Logs

Appuyer sur **📋 View Logs** pour voir tous les appels API :

```
[14:30:30.123] API CALL: GET /api/health/api
[14:30:30.245] API RESPONSE: /api/health/api [200] ApiHealthResponse(...)
[14:30:31.100] API CALL: GET /api/health/database
[14:30:31.350] API RESPONSE: /api/health/database [200] HealthCheckResponse(...)
```

---

## 6️⃣ Configuration Avancée

### Changer l'IP du Serveur

**Scénario :** L'API est sur une autre machine

1. Aller à **⚙️ Settings**
2. Modifier **Serveur API** → nouvelle IP
3. Appuyer sur **Enregistrer**
4. Retourner à l'écran principal
5. Cliquer sur **Retry** si nécessaire

### Exemple

```
Votre API est sur :    192.168.1.50:8000
Dans l'app Android :   192.168.1.50
                       8000
```

### Déploiement Réseau Local

**Votre LAN :**

```
Routeur WiFi (192.168.1.1)
├── PC Serveur API (192.168.1.20)
└── Téléphone Android (192.168.1.100)

Dans Android Settings :
  API Host : 192.168.1.20
  API Port : 8000
```

### Déploiement Internet (Production)

**Si l'API est en cloud (Azure, AWS) :**

```
Dans Android Settings :
  API Host : api.company.com
  API Port : 443 (HTTPS)
```

⚠️ **Important** : L'app actuelle supporte UNIQUEMENT HTTP local. Pour HTTPS distant, modifier `network_security_config.xml`.

---

## 7️⃣ Troubleshooting

### ❌ App ne démarre pas

**Symptômes :**
- Crash immédiat
- Message d'erreur lors du lancement

**Solutions :**

```bash
# Vérifier les logs
adb logcat | grep -i "error\|exception"

# Réinstaller l'app
adb uninstall com.example.stockapp
adb install app-debug.apk
```

### ❌ État "Disconnected" ou "Connection Failed"

**Causes possibles :**

1. **L'API n'est pas lancée**
   ```bash
   # Sur le serveur, vérifier que l'API tourne
   http://192.168.1.20:8000/
   # Doit répondre : {"message": "Stock API is running"}
   ```

2. **Téléphone et serveur pas sur le même réseau**
   ```bash
   # Sur le téléphone, ouvrir un terminal
   ping 192.168.1.20
   # Doit répondre avec des packets
   ```

3. **Mauvaise IP configurée**
   - Vérifier l'IP du serveur : `ipconfig` (Windows) ou `ifconfig` (Linux)
   - Mettre à jour dans ⚙️ Settings

4. **Firewall bloque le port 8000**
   ```bash
   # Windows : Ouvrir Windows Defender Firewall
   # Ajouter une règle autorisant le port 8000
   
   # Linux :
   sudo ufw allow 8000
   ```

### ❌ "API Health : ❌ Failed"

**Cause :** L'API répond mais SQL Server n'est pas accessible

**Solutions :**

```bash
# Sur le serveur, tester la BD
python -c "import pyodbc; pyodbc.connect('...')"

# Vérifier le fichier .env
cat .env

# Relancer l'API avec les bons paramètres
python main.py
```

### ❌ "Database Health : Unknown"

**Cause :** Version de la BD n'a pas pu être récupérée

**Solutions :**

1. Vérifier que la requête SQL retourne la version
   ```sql
   SELECT @@VERSION
   ```

2. Vérifier l'utilisateur SQL Server a les permissions
   ```sql
   SELECT CURRENT_USER
   ```

### ❌ App se ferme en testant la connexion

**Cause :** Timeout ou erreur réseau

**Solutions :**

1. Vérifier la latence réseau
   ```bash
   # Sur le téléphone
   ping -c 4 192.168.1.20
   ```

2. Augmenter le timeout dans le code (avancé)
   - Fichier : `api/StockApiService.kt`
   - Ajouter un Timeout interceptor Retrofit

---

## 8️⃣ Considérations de Sécurité

### ⚠️ En Développement (Votre Réseau Local)

✅ HTTP sur 192.168.x.x : OK  
✅ Pas d'authentification requise : OK  
✅ Logs visibles dans l'app : OK  

### 🔒 En Production

❌ HTTP uniquement : PAS BON  
❌ Pas d'authentification : PAS BON  
❌ Logs exposés : PAS BON  

**À faire :**

1. **Mettre HTTPS obligatoire**
   - Certificat SSL valide
   - Modifier `network_security_config.xml` pour accepter HTTPS

2. **Ajouter l'authentification**
   - Token JWT ou session
   - Envoyer token dans headers HTTP

3. **Masquer les logs**
   - Désactiver `AppLogger` en production
   - Envoyer logs à un serveur centralisé

4. **Chiffrer les données locales**
   - Utiliser EncryptedSharedPreferences pour stocker IP/Port

---

## 9️⃣ Mises à Jour de l'Application

### Méthode Simple (Pendant le Développement)

1. Compiler une nouvelle APK
2. Transférer via USB ou email
3. Réinstaller en appuyant sur l'APK

### Méthode Automatique (Production)

- Intégrer **Firebase App Distribution**
- Ou utiliser **Google Play Store**
- Utilisateurs reçoivent les mises à jour automatiquement

---

## 🔟 Checklist d'Installation

- ☑️ APK obtenu (pré-compilé ou compilé)
- ☑️ Téléphone Android 8.0+ vérifié
- ☑️ APK transféré sur le téléphone
- ☑️ APK installé avec succès
- ☑️ Mode Développeur activé (pour ADB)
- ☑️ Paramètres configurés (IP: `192.168.1.20`, Port: `8000`)
- ☑️ Téléphone et serveur sur même réseau WiFi
- ☑️ Ping du serveur depuis téléphone réussit
- ☑️ App affiche "Connected ✅"
- ☑️ Tests API et Database réussis

---

## 📝 Exemple Complet (Copier-Coller)

### Serveur Windows (PowerShell)

```powershell
# 1. Naviguer au dossier backend
cd C:\projects\POC_SPEED_RF_INFO_STOCK\backend

# 2. Créer .env
@"
SQL_SERVER_HOST=LAPVMI116\SQL2019
SQL_SERVER_PORT=1433
SQL_SERVER_DB=POC_V8SpeedDeveloppement
SQL_SERVER_USER=sa
SQL_SERVER_PASSWORD=BKS
API_HOST=0.0.0.0
API_PORT=8000
"@ | Out-File -Encoding UTF8 .env

# 3. Créer venv et installer
python -m venv venv
.\venv\Scripts\activate
pip install -r requirements.txt

# 4. Lancer l'API
python main.py
# Résultat : INFO:     Uvicorn running on http://0.0.0.0:8000
```

### Téléphone Android

1. **Recevoir l'APK**
2. **Ouvrir Fichiers** → Appuyer sur `app-debug.apk`
3. **Installer** → Accepter
4. **Ouvrir l'app**
5. **Aller aux Settings** (⚙️)
   - Serveur API : `192.168.1.20` (remplacer par votre IP)
   - Port : `8000`
6. **Enregistrer**
7. **Retour** → Doit afficher "Connected ✅"
8. **Test API** → ✅ healthy
9. **Test Database** → ✅ connected + version

---

## 📞 Support

Pour toute question :

1. **Consultez les logs** : Appuyer sur 📋 View Logs
2. **Vérifiez la connectivité** : ping/telnet vers l'API
3. **Vérifiez le .env** : Tous les paramètres correctement saisis
4. **Lisez la documentation** : `docs/architecture.html`

Bonne installation ! 🚀
