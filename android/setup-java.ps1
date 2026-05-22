# Script pour télécharger et configurer Java 21 LTS pour ce projet

Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Configuration Java 21 LTS pour le projet  ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Dossier de stockage Java
$javaDir = "D:\Projects\POC_SPEED_RF_INFO_STOCK\android\.java"
$javaHome = "$javaDir\jdk-21"

# Vérifier si Java 21 est déjà installé localement
if (Test-Path "$javaHome\bin\java.exe") {
    Write-Host "✅ Java 21 trouvé dans le projet!" -ForegroundColor Green
    Write-Host "Chemin: $javaHome" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Prochaine étape: Utilise ce script pour compiler:" -ForegroundColor Yellow
    Write-Host ".\build-with-java21.ps1 build" -ForegroundColor White
    exit 0
}

Write-Host "Java 21 n'est pas trouvé. Téléchargement..." -ForegroundColor Yellow
Write-Host ""

# Créer le dossier s'il n'existe pas
if (-not (Test-Path $javaDir)) {
    New-Item -ItemType Directory -Path $javaDir -Force | Out-Null
}

# Télécharger Java 21 LTS (Temurin)
$downloadUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.2%2B13/OpenJDK21U-jdk_x64_windows_hotspot_21.0.2_13.zip"
$zipFile = "$javaDir\java21.zip"

try {
    Write-Host "Téléchargement de Java 21 LTS..." -ForegroundColor Cyan
    Write-Host "URL: $downloadUrl" -ForegroundColor Gray
    Write-Host "(Cela peut prendre 2-3 minutes...)" -ForegroundColor Gray
    Write-Host ""

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile -UseBasicParsing

    Write-Host "✅ Téléchargement terminé!" -ForegroundColor Green
    Write-Host ""

    Write-Host "Extraction de Java 21..." -ForegroundColor Cyan
    Expand-Archive -Path $zipFile -DestinationPath $javaDir -Force

    # Renommer le dossier extrait
    $extractedFolder = Get-ChildItem $javaDir -Directory | Where-Object { $_.Name -like "jdk-21*" } | Select-Object -First 1
    if ($extractedFolder) {
        Rename-Item -Path $extractedFolder.FullName -NewName "jdk-21" -Force
    }

    Remove-Item $zipFile -Force

    Write-Host "✅ Java 21 installé!" -ForegroundColor Green
    Write-Host "Chemin: $javaHome" -ForegroundColor Cyan
    Write-Host ""

} catch {
    Write-Host "❌ Erreur lors du téléchargement de Java 21" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative: Installe Java 21 manuellement depuis:" -ForegroundColor Yellow
    Write-Host "https://adoptium.net/temurin/" -ForegroundColor Cyan
    exit 1
}

# Créer le script de build avec Java 21
$buildScript = @'
# Script pour compiler avec Java 21 LTS

param(
    [string]$Task = "build"
)

$javaHome = "D:\Projects\POC_SPEED_RF_INFO_STOCK\android\.java\jdk-21"

if (-not (Test-Path "$javaHome\bin\java.exe")) {
    Write-Host "❌ Java 21 n'est pas trouvé!" -ForegroundColor Red
    Write-Host "Exécute d'abord: .\setup-java.ps1" -ForegroundColor Yellow
    exit 1
}

# Configurer les variables d'environnement
$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"
$env:GRADLE_OPTS = "--enable-native-access=ALL-UNNAMED"

# Chemin de Gradle
$gradleHome = "$env:USERPROFILE\.gradle\gradle-8.9"
$gradleZip = "$env:TEMP\gradle-8.9-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"

# Télécharger Gradle si nécessaire
if (-not (Test-Path "$gradleHome\bin\gradle.bat")) {
    Write-Host "Téléchargement de Gradle 8.9..." -ForegroundColor Cyan
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip -UseBasicParsing
    Expand-Archive -Path $gradleZip -DestinationPath (Split-Path $gradleHome) -Force
    Remove-Item $gradleZip -Force -ErrorAction SilentlyContinue
    Write-Host "✅ Gradle téléchargé!" -ForegroundColor Green
}

$env:PATH = "$gradleHome\bin;$env:PATH"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Compilation avec Java 21 LTS" -ForegroundColor Cyan
Write-Host "JAVA_HOME: $javaHome" -ForegroundColor Gray
Write-Host "Task: $Task" -ForegroundColor Gray
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

& gradle.bat $Task

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "✅ BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Red
    Write-Host "❌ BUILD FAILED!" -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
}

exit $LASTEXITCODE
'@

# Sauvegarder le script
$buildScriptPath = "D:\Projects\POC_SPEED_RF_INFO_STOCK\android\build-with-java21.ps1"
$buildScript | Out-File -FilePath $buildScriptPath -Encoding UTF8 -Force

Write-Host ""
Write-Host "╔════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║     ✅ Tout est prêt!                      ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host "Pour compiler l'app, utilise:" -ForegroundColor Cyan
Write-Host ".\build-with-java21.ps1 build" -ForegroundColor Yellow
Write-Host ""
Write-Host "Ou pour installer sur le téléphone:" -ForegroundColor Cyan
Write-Host ".\build-with-java21.ps1 installDebug" -ForegroundColor Yellow
Write-Host ""
