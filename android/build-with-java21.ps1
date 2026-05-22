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
