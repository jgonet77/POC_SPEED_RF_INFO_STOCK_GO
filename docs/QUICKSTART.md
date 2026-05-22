# ⚡ Quick Start - Installation en 10 Minutes

Vous avez 10 minutes ? Suivez ce guide pour avoir l'app Android connectée à l'API.

---

## 🔄 Workflow Complet en 3 Étapes

```
Step 1: Préparer l'API (3 min)
Step 2: Installer l'APK Android (2 min)
Step 3: Configurer et Tester (5 min)
```

---

## Step 1️⃣ : Préparer l'API (3 minutes)

### Option A : Sur Windows

```bash
# 1. Ouvrir PowerShell dans D:\Projects\POC_SPEED_RF_INFO_STOCK\backend\

# 2. Double-cliquer sur setup.bat
# OU lancer manuellement:
.\setup.bat

# 3. Editer le fichier .env avec vos paramètres SQL Server
notepad .env

# Exemple .env:
# SQL_SERVER_HOST=VOTRE_SERVEUR\INSTANCE
# SQL_SERVER_USER=sa
# SQL_SERVER_PASSWORD=votre_password

# 4. Lancer l'API
python main.py
```

**Résultat attendu :**
```
INFO:     Uvicorn running on http://192.168.1.20:8000
```

### Option B : Sur Linux/Mac

```bash
# 1. Naviguer au dossier backend
cd POC_SPEED_RF_INFO_STOCK/backend

# 2. Rendre le script exécutable et lancer
chmod +x setup.sh
./setup.sh

# 3. Editer .env
nano .env

# 4. Lancer l'API
python main.py
```

### Vérifier que l'API Fonctionne

Ouvrir le navigateur et accéder à :
```
http://192.168.1.20:8000/docs
```

Vous devez voir la documentation Swagger interactive.

---

## Step 2️⃣ : Installer l'APK (2 minutes)

### Obtenir l'APK

**Option A : APK Pré-compilé**

L'APK se trouve à :
```
POC_SPEED_RF_INFO_STOCK/android/app/build/outputs/apk/debug/app-debug.apk
```

Si le dossier `build` n'existe pas, compiler via Android Studio:
```bash
cd android
./gradlew clean build
# Attendre ~3 min
```

**Option B : Via Airdrop / Email / USB**

1. Transférer `app-debug.apk` sur le téléphone
2. Téléphone reçoit l'APK dans Downloads/

### Installer sur le Téléphone

1. **Ouvrir Fichiers** (Files) sur le téléphone
2. Naviguer vers **Downloads**
3. Appuyer sur **app-debug.apk**
4. Appuyer sur **Installer**
5. Accepter les permissions
6. Attendre "Installation réussie"

---

## Step 3️⃣ : Configurer et Tester (5 minutes)

### Ouvrir l'App

1. L'app apparaît dans le drawer (liste des apps)
2. Appuyer pour ouvrir
3. L'app affiche "Connecting..." puis...

### Option A : Cela Fonctionne Directement ✅

Si l'app affiche **"Connected ✅"** :

1. Appuyer sur **Test API** → Doit afficher ✅ healthy
2. Appuyer sur **Test Database** → Doit afficher ✅ connected

**Succès ! Vous pouvez passer au step 4.**

### Option B : Cela Ne Fonctionne Pas ❌

Si l'app affiche **"Connection Failed ❌"** :

1. Appuyer sur le bouton **⚙️ Settings**
2. Modifier :
   - **Serveur API** : `192.168.1.20` → **Votre IP du serveur**
   - **Port** : `8000` (ne pas changer)
3. Appuyer sur **Enregistrer**
4. Retour à l'écran principal
5. Doit afficher "Connected ✅" maintenant

### Vérifier la Connectivité Réseau

Si cela ne fonctionne toujours pas :

**Sur le téléphone**, ouvrir un terminal et tester :
```bash
# Remplacer 192.168.1.20 par votre IP serveur
ping 192.168.1.20

# Résultat attendu: les packets passent
PING 192.168.1.20 (192.168.1.20): 56 data bytes
64 bytes from 192.168.1.20: icmp_seq=0 ttl=64 time=12.345 ms
```

Si ping ne fonctionne pas :
- ❌ Téléphone et serveur ne sont pas sur le même réseau WiFi
- ❌ Firewall bloque le port 8000
- ❌ Mauvaise IP serveur

---

## 📊 Tableau Récapitulatif

| Étape | Durée | Commande | Résultat |
|-------|-------|----------|----------|
| Setup Backend | 3 min | `.\setup.bat` → `python main.py` | API sur :8000 |
| Compiler APK | 3 min (optionnel) | `./gradlew clean build` | APK créé |
| Installer APK | 2 min | Cliquer sur APK → Installer | App sur téléphone |
| Configurer App | 2 min | Settings → IP/Port → Enregistrer | Connected ✅ |
| Tester | 1 min | Test API + Test DB | Tous ✅ |
| **TOTAL** | **~10 min** | | **Prêt à utiliser!** |

---

## 🎯 Points Clés

### ✅ Checklist Finale

- ☑️ API lancée et accessible via le navigateur
- ☑️ APK installé sur le téléphone
- ☑️ App affiche "Connected ✅"
- ☑️ Test API affiche ✅ healthy
- ☑️ Test Database affiche ✅ connected

### ⚠️ Si Quelque Chose N'Fonctionne Pas

1. **Vérifier les logs API** : Regarder la console Python
2. **Vérifier les logs Android** : Appuyer sur 📋 View Logs
3. **Vérifier la connectivité** : `ping <IP-SERVEUR>`
4. **Vérifier le .env** : Tous les paramètres correctement remplis

---

## 📞 Besoin de Plus de Détails ?

- **API Avancé** → Lire `DEPLOYMENT_BACKEND.md`
- **Android Avancé** → Lire `DEPLOYMENT_ANDROID.md`
- **Architecture** → Lire `architecture.html`

---

## 🚀 Prochaines Étapes (Optionnel)

Maintenant que tout fonctionne :

### 1️⃣ Ajouter Plus d'Endpoints API

Modifier `backend/routes/health.py` pour ajouter :
- `/api/stock/search?sku=...` - Rechercher un SKU
- `/api/stock/{sku}/details` - Détails du stock
- `/api/login` - Authentification

### 2️⃣ Améliorer l'Interface Android

- Ajouter un écran de recherche
- Afficher les résultats en liste
- Ajouter un écran de détails

### 3️⃣ Déployer en Production

- Héberger l'API sur Azure/AWS
- Distribuer l'app via Google Play Store
- Ajouter l'authentification

---

**Bonne utilisation ! 🎉**
