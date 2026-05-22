@echo off
REM ============================================
REM Script d'installation automatique - Backend
REM pour Windows (PowerShell recommandé)
REM ============================================

echo.
echo ========================================
echo   STOCK API - Setup Automatique
echo   Windows Edition
echo ========================================
echo.

REM Vérifier Python
echo [1/5] Vérification de Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo ERREUR: Python non trouvé!
    echo Installez Python 3.10+ depuis https://www.python.org/downloads/
    pause
    exit /b 1
)
echo OK: Python installé

REM Créer venv
echo.
echo [2/5] Création de l'environnement virtuel...
if exist venv (
    echo OK: venv existe déjà
) else (
    python -m venv venv
    echo OK: venv créé
)

REM Activer venv
echo.
echo [3/5] Activation de l'environnement...
call venv\Scripts\activate.bat
echo OK: venv activé

REM Installer dépendances
echo.
echo [4/5] Installation des dépendances...
pip install -r requirements.txt --quiet
if errorlevel 1 (
    echo ERREUR: Impossible d'installer les dépendances
    pause
    exit /b 1
)
echo OK: Dépendances installées

REM Créer .env si inexistant
echo.
echo [5/5] Configuration du fichier .env...
if exist .env (
    echo OK: .env existe déjà
    echo Contenu actuel:
    type .env
) else (
    echo Création du fichier .env...
    (
        echo SQL_SERVER_HOST=LAPVMI116\SQL2019
        echo SQL_SERVER_PORT=1433
        echo SQL_SERVER_DB=POC_V8SpeedDeveloppement
        echo SQL_SERVER_USER=sa
        echo SQL_SERVER_PASSWORD=BKS
        echo API_HOST=0.0.0.0
        echo API_PORT=8000
        echo API_RELOAD=false
        echo CORS_ORIGINS=http://localhost:3000,http://10.0.2.2:3000
    ) > .env
    echo OK: .env créé avec valeurs par défaut
    echo.
    echo ⚠️  IMPORTANT: Modifier .env avec vos paramètres SQL Server!
)

echo.
echo ========================================
echo   Setup Terminé!
echo ========================================
echo.
echo Prochaines étapes:
echo 1. Éditer le fichier .env avec vos paramètres:
echo    - SQL_SERVER_HOST
echo    - SQL_SERVER_USER / PASSWORD
echo.
echo 2. Lancer l'API:
echo    python main.py
echo.
echo 3. Tester via le navigateur:
echo    http://localhost:8000/
echo    http://localhost:8000/docs (Swagger)
echo.
pause
