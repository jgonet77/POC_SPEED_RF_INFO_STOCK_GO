#!/bin/bash

# ============================================
# Script d'installation automatique - Backend
# pour Linux/Mac
# ============================================

echo ""
echo "========================================"
echo "  STOCK API - Setup Automatique"
echo "  Linux/Mac Edition"
echo "========================================"
echo ""

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Vérifier Python
echo "[1/5] Vérification de Python..."
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}ERREUR: Python3 non trouvé!${NC}"
    echo "Installez Python 3.10+ :"
    echo "  Ubuntu/Debian: sudo apt-get install python3 python3-pip"
    echo "  Mac: brew install python3"
    exit 1
fi
PYTHON_VERSION=$(python3 --version | cut -d' ' -f2 | cut -d'.' -f1,2)
echo -e "${GREEN}OK: Python $PYTHON_VERSION installé${NC}"

# Vérifier pip
echo ""
echo "[2/5] Vérification de pip..."
if ! command -v pip3 &> /dev/null; then
    echo -e "${RED}ERREUR: pip3 non trouvé!${NC}"
    echo "Installez pip3 :"
    echo "  Ubuntu/Debian: sudo apt-get install python3-pip"
    echo "  Mac: brew install python3"
    exit 1
fi
echo -e "${GREEN}OK: pip3 installé${NC}"

# Créer venv
echo ""
echo "[3/5] Création de l'environnement virtuel..."
if [ -d "venv" ]; then
    echo -e "${GREEN}OK: venv existe déjà${NC}"
else
    python3 -m venv venv
    echo -e "${GREEN}OK: venv créé${NC}"
fi

# Activer venv
echo ""
echo "[4/5] Activation de l'environnement..."
source venv/bin/activate
echo -e "${GREEN}OK: venv activé${NC}"

# Installer dépendances
echo ""
echo "[5/5] Installation des dépendances..."
pip install -q -r requirements.txt
if [ $? -ne 0 ]; then
    echo -e "${RED}ERREUR: Impossible d'installer les dépendances${NC}"
    deactivate
    exit 1
fi
echo -e "${GREEN}OK: Dépendances installées${NC}"

# Créer .env si inexistant
echo ""
echo "[6/5] Configuration du fichier .env..."
if [ -f ".env" ]; then
    echo -e "${GREEN}OK: .env existe déjà${NC}"
    echo "Contenu actuel:"
    cat .env
else
    echo "Création du fichier .env..."
    cat > .env << 'EOF'
SQL_SERVER_HOST=LAPVMI116\SQL2019
SQL_SERVER_PORT=1433
SQL_SERVER_DB=POC_V8SpeedDeveloppement
SQL_SERVER_USER=sa
SQL_SERVER_PASSWORD=BKS
API_HOST=0.0.0.0
API_PORT=8000
API_RELOAD=false
CORS_ORIGINS=http://localhost:3000,http://10.0.2.2:3000
EOF
    echo -e "${GREEN}OK: .env créé avec valeurs par défaut${NC}"
    echo ""
    echo -e "${YELLOW}⚠️  IMPORTANT: Modifier .env avec vos paramètres SQL Server!${NC}"
fi

echo ""
echo "========================================"
echo "   Setup Terminé!"
echo "========================================"
echo ""
echo "Prochaines étapes:"
echo "1. Éditer le fichier .env avec vos paramètres:"
echo "   nano .env"
echo ""
echo "2. Lancer l'API:"
echo "   python main.py"
echo ""
echo "3. Tester via le navigateur:"
echo "   http://localhost:8000/"
echo "   http://localhost:8000/docs (Swagger)"
echo ""
