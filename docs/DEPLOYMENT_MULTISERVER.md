# 🖥️ Guide: Déployer sur Plusieurs Serveurs

Guide pour **copier le projet** d'un serveur à un autre et le faire fonctionner facilement.

---

## 🎯 Scénario

Vous avez développé l'API sur votre machine (Windows) et voulez la déployer sur :
- Un serveur Linux de production
- Un serveur Windows client
- Un serveur cloud (Azure, AWS)
- N'importe quel autre endroit

---

## ⚠️ Le Piège Courant

Si vous faites simplement **"Copier-Coller"** du dossier entier :

```
❌ MAUVAIS: Copier tout
├── venv/               ← Binaires Windows (ne fonctionnent PAS sur Linux)
├── __pycache__/        ← Bytecode compilé (incompatible)
├── build/              ← Artefacts de build (inutiles)
└── .env                ← SECRETS (ne JAMAIS partager!)
```

---

## ✅ La Bonne Méthode (3 Étapes)

### **Étape 1️⃣ : Préparer le Code Source (Serveur Source)**

Nettoyer tous les fichiers qui ne doivent **pas** être copiés :

```bash
# Windows
cd D:\Projects\POC_SPEED_RF_INFO_STOCK\backend
.\clean_for_deployment.bat

# Linux/Mac
cd ~/projects/POC_SPEED_RF_INFO_STOCK/backend
chmod +x clean_for_deployment.sh
./clean_for_deployment.sh
```

**Ce que cela supprime :**
- `venv/` (100+ MB)
- `__pycache__/` (fichiers compilés)
- `.pytest_cache/` (cache de tests)
- `*.pyc` (bytecode)
- `.env` (secrets)
- `build/` (artefacts)

**Reste à copier :**
```
backend/
├── main.py            ✅
├── config.py          ✅
├── requirements.txt   ✅
├── setup.sh           ✅
├── setup.bat          ✅
├── .env.example       ✅
├── models/            ✅
├── routes/            ✅
├── services/          ✅
├── repositories/      ✅
└── tests/             ✅
```

### **Étape 2️⃣ : Copier sur le Nouveau Serveur**

#### Option A : Via USB/Disk

```bash
# Sur serveur source (Windows)
# Sélectionner dossier backend/ → Copier → USB

# Sur serveur destination
# Coller dans un dossier (ex: /var/api/)
```

#### Option B : Via Git

```bash
# Push sur GitHub/GitLab
git add .
git commit -m "Deploy version 1.0"
git push

# Sur nouveau serveur
git clone <repository>
cd POC_SPEED_RF_INFO_STOCK/backend
```

#### Option C : Via Fichier ZIP

```bash
# Sur serveur source
# Cliquer droit → Compresser → backend.zip

# Sur serveur destination
unzip backend.zip
cd backend
```

### **Étape 3️⃣ : Initialiser sur le Nouveau Serveur**

```bash
# Sur Linux destination
cd /path/to/backend

# Lancer setup automatique
chmod +x setup.sh
./setup.sh

# Résultat: venv créé + dépendances installées automatiquement ✅

# Éditer configuration
nano .env
# Remplir vos paramètres SQL Server

# Lancer l'API
python main.py
```

---

## 🔄 Workflow Complet (Pas à Pas)

### **Jour 1: Développement sur Windows**

```bash
# Dev sur Windows
C:\projects\backend>
python main.py
# Tout fonctionne ✅
```

### **Jour 2: Préparer pour Deployment**

```bash
# Nettoyer le code
C:\projects\backend> .\clean_for_deployment.bat
# Résultat: venv/ supprimé, __pycache__/ supprimé, .env supprimé

# Copier sur USB/ZIP
# Le dossier est maintenant ~1 MB (au lieu de 200+ MB)
```

### **Jour 3: Installer sur Serveur Linux Production**

```bash
# Sur serveur Linux
$ cd /opt/api
$ unzip backend.zip
$ cd backend

# Setup automatique
$ chmod +x setup.sh
$ ./setup.sh
# Résultat:
#   - venv créé
#   - Python 3.10+ détecté
#   - pip installé
#   - requirements.txt appliqué
#   - .env créé avec valeurs par défaut

# Configuration
$ nano .env
# Remplacer:
#   SQL_SERVER_HOST=production-sql.local
#   SQL_SERVER_USER=prod_user
#   SQL_SERVER_PASSWORD=secure_password_123

# Lancer
$ python main.py
# INFO: Uvicorn running on http://0.0.0.0:8000
```

---

## 📊 Tableau Comparatif

| Situation | Action | Résultat |
|-----------|--------|----------|
| **Copier TOUT** (venv inclus) | Coller sur Linux | ❌ ERREUR: Binaires Windows incompatibles |
| **Copier PROPRE** (sans venv) | Coller sur Linux + `./setup.sh` | ✅ Fonctionne parfaitement |
| **Copier avec .env** | Coller sur serveur destinataire | ❌ DANGER: Secrets exposés! |
| **Copier sans .env** | Coller + créer `.env` localement | ✅ Sécurisé |

---

## 🛡️ Points de Sécurité

### ✅ À Faire

1. **Supprimer `.env` avant de copier**
   ```bash
   rm .env  # Jamais de secrets en transit
   ```

2. **Créer `.env` manuellement sur le serveur destination**
   ```bash
   cp .env.example .env
   nano .env  # Remplir les paramètres locaux
   ```

3. **Utiliser Git avec `.gitignore`**
   ```bash
   .env          # dans .gitignore
   venv/         # dans .gitignore
   __pycache__/  # dans .gitignore
   ```

### ❌ À Éviter

- ❌ Ne JAMAIS partager le `.env` en clair
- ❌ Ne JAMAIS commiter le `.env` en Git
- ❌ Ne JAMAIS copier le `venv/` d'une autre plateforme
- ❌ Ne PAS utiliser les mêmes mots de passe sur tous les serveurs

---

## 🤔 FAQ

### Q: Est-ce que le `setup.sh` fonctionne vraiment automatiquement?

**R:** Oui! Voici ce qu'il fait:

```bash
#!/bin/bash
1. Vérifie Python 3 installé
2. Vérifie pip3 installé
3. Crée le venv
4. Active le venv
5. Installe toutes les dépendances de requirements.txt
6. Crée le .env par défaut

# Vous n'avez juste qu'à éditer .env après
```

### Q: Et si Python n'est pas installé sur le serveur destination?

**R:** `setup.sh` retournera une erreur claire:

```
ERREUR: Python3 non trouvé!
Installez Python 3.10+ :
  Ubuntu/Debian: sudo apt-get install python3 python3-pip
  Mac: brew install python3
```

Installez d'abord Python, puis relancez `setup.sh`.

### Q: La taille du dossier pour copie?

**R:**

```
❌ Avant nettoyage: ~200 MB (venv inclus)
✅ Après nettoyage: ~2 MB (code uniquement)
✅ Avec pip install: ~100 MB (après setup.sh)
```

### Q: Puis-je utiliser Git directement?

**R:** Oui! Si vous avez un `.gitignore` correct:

```bash
# .gitignore doit contenir:
venv/
__pycache__/
.pytest_cache/
.env
build/
*.pyc
```

Alors `git clone` + `./setup.sh` = **Déploiement en 2 minutes** ✅

### Q: Et pour Docker?

**R:** Version future! On pourrait créer:

```dockerfile
FROM python:3.11
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["python", "main.py"]
```

Mais pas nécessaire pour ce POC.

---

## 🚀 Checklist Deployment Multi-Serveur

### **Serveur Source (Préparation)**

- ☑️ Code développé et testé
- ☑️ `./clean_for_deployment.sh` exécuté
- ☑️ Vérifier que `.env` est supprimé
- ☑️ Vérifier que `venv/` est supprimé
- ☑️ Compresser en ZIP ou faire un commit Git

### **Serveur Destination (Installation)**

- ☑️ Copier/cloner le code
- ☑️ Python 3.10+ installé
- ☑️ `chmod +x setup.sh` exécuté (Linux)
- ☑️ `./setup.sh` exécuté
- ☑️ `.env.example` copié en `.env`
- ☑️ `.env` édité avec paramètres locaux
- ☑️ `python main.py` lancé
- ☑️ `/docs` accessible dans le navigateur

---

## 📝 Exemple Real-World

### Jour 1: Développement (Windows)

```powershell
PS C:\api\backend> python main.py
INFO:     Uvicorn running on http://192.168.1.20:8000

# Test local ✅
curl http://localhost:8000/docs
```

### Jour 2: Préparer (Windows)

```powershell
PS C:\api\backend> .\clean_for_deployment.bat
# Supprime venv, __pycache__, .env

# Copier sur USB
# C:\api\backend → D:\usb\backend\
```

### Jour 3: Déployer (Linux Production)

```bash
$ sudo mkdir -p /opt/api
$ sudo cp -r /usb/backend /opt/api/
$ cd /opt/api/backend

$ chmod +x setup.sh
$ ./setup.sh
# [1/5] Python 3.11 ✅
# [2/5] pip3 ✅
# [3/5] venv créé ✅
# [4/5] dépendances installées ✅
# [5/5] .env créé ✅

$ nano .env
# SQL_SERVER_HOST=prod-sql.company.com
# SQL_SERVER_PASSWORD=SecureP@ss123

$ python main.py
INFO:     Uvicorn running on http://0.0.0.0:8000
```

---

## 💡 Conseil Final

**Pour un déploiement vraiment sans tracas :**

1. ✅ Utiliser Git + `.gitignore`
2. ✅ Exécuter `./setup.sh` automatiquement
3. ✅ Créer `.env` localement sur chaque serveur
4. ✅ Utiliser des secrets managers (Azure Key Vault, etc.) en production

**Le résultat :**
```
git clone <repo>
cd backend
./setup.sh
nano .env          # Remplir localement
python main.py     # C'est tout!
```

---

**Bon déploiement! 🚀**
