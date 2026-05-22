#!/bin/bash

# ============================================
# Script de nettoyage pour deployment
# Supprime les fichiers qui ne doivent PAS
# être copiés sur un autre serveur
# ============================================

echo ""
echo "========================================"
echo "  Nettoyage pour Deployment"
echo "========================================"
echo ""

# Couleurs
GREEN='\033[0;32m'
NC='\033[0m'

echo "[1/5] Suppression de venv..."
if [ -d "venv" ]; then
    rm -rf venv
    echo -e "${GREEN}OK: venv supprimé${NC}"
else
    echo -e "${GREEN}OK: venv n'existe pas${NC}"
fi

echo ""
echo "[2/5] Suppression des cache Python..."
find . -type d -name __pycache__ -exec rm -rf {} + 2>/dev/null || true
find . -type d -name .pytest_cache -exec rm -rf {} + 2>/dev/null || true
find . -type f -name "*.pyc" -delete 2>/dev/null || true
echo -e "${GREEN}OK: caches supprimés${NC}"

echo ""
echo "[3/5] Suppression de build..."
if [ -d "build" ]; then
    rm -rf build
    echo -e "${GREEN}OK: build supprimé${NC}"
fi

echo ""
echo "[4/5] Suppression de .pytest_cache..."
if [ -d ".pytest_cache" ]; then
    rm -rf .pytest_cache
    echo -e "${GREEN}OK: .pytest_cache supprimé${NC}"
fi

echo ""
echo "[5/5] Suppression de .env (secret)..."
if [ -f ".env" ]; then
    rm .env
    echo -e "${GREEN}OK: .env supprimé (sera créé sur le nouveau serveur)${NC}"
fi

echo ""
echo "========================================"
echo "   Nettoyage Terminé!"
echo "========================================"
echo ""
echo "Fichiers à COPIER sur le nouveau serveur:"
echo "✅ main.py"
echo "✅ config.py"
echo "✅ requirements.txt"
echo "✅ setup.sh / setup.bat"
echo "✅ .env.example"
echo "✅ models/"
echo "✅ routes/"
echo "✅ services/"
echo "✅ repositories/"
echo "✅ tests/"
echo ""
echo "Fichiers à CRÉER manuellement:"
echo "🔧 .env (copier depuis .env.example et remplir)"
echo ""
echo "Prochaines étapes:"
echo "1. Copier le dossier backend/ sur le nouveau serveur"
echo "2. Sur le nouveau serveur, lancer setup.sh:"
echo "   chmod +x setup.sh && ./setup.sh"
echo "3. Éditer .env avec les paramètres du serveur"
echo "4. Lancer python main.py"
echo ""
