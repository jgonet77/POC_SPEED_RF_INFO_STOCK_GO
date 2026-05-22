@echo off
REM ============================================
REM Script de nettoyage pour deployment
REM Supprime les fichiers qui ne doivent PAS
REM être copiés sur un autre serveur
REM ============================================

echo.
echo ========================================
echo   Nettoyage pour Deployment
echo ========================================
echo.

echo [1/5] Suppression de venv...
if exist venv (
    rmdir /s /q venv
    echo OK: venv supprimé
) else (
    echo OK: venv n'existe pas
)

echo.
echo [2/5] Suppression des cache Python...
for /d /r . %%d in (__pycache__) do (
    if exist "%%d" (
        rmdir /s /q "%%d"
        echo OK: __pycache__ supprimé
    )
)

echo.
echo [3/5] Suppression de .pytest_cache...
if exist .pytest_cache (
    rmdir /s /q .pytest_cache
    echo OK: .pytest_cache supprimé
)

echo.
echo [4/5] Suppression des fichiers .pyc...
for /r . %%f in (*.pyc) do (
    if exist "%%f" del /q "%%f"
)
echo OK: fichiers .pyc supprimés

echo.
echo [5/5] Suppression du dossier build...
if exist build (
    rmdir /s /q build
    echo OK: build supprimé
)

echo.
echo ========================================
echo   Nettoyage Terminé!
echo ========================================
echo.
echo Fichiers à COPIER sur le nouveau serveur:
echo ✅ main.py
echo ✅ config.py
echo ✅ requirements.txt
echo ✅ setup.sh / setup.bat
echo ✅ .env.example
echo ✅ models/
echo ✅ routes/
echo ✅ services/
echo ✅ repositories/
echo ✅ tests/
echo.
echo Fichiers à CRÉER manuellement:
echo 🔧 .env (copier depuis .env.example et remplir)
echo.
echo Prochaines étapes:
echo 1. Copier le dossier backend/ sur le nouveau serveur
echo 2. Sur le nouveau serveur, lancer setup.sh:
echo    chmod +x setup.sh && ./setup.sh
echo 3. Éditer .env avec les paramètres du serveur
echo 4. Lancer python main.py
echo.
pause
