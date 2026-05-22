# Script de compilation pour Android sans Gradle Wrapper
# Usage: .\build.ps1 build
#        .\build.ps1 installDebug

param(
    [string]$Task = "build"
)

# Couleurs pour l'affichage
$colors = @{
    Success = "Green"
    Error = "Red"
    Info = "Cyan"
    Warning = "Yellow"
}

function Write-Status {
    param([string]$Message, [string]$Type = "Info")
    $color = $colors[$Type]
    Write-Host $Message -ForegroundColor $color
}

# Vérifier si Java est installé
Write-Status "Vérification de Java..." "Info"
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Status "✅ Java trouvé!" "Success"
    }
} catch {
    Write-Status "❌ Java n'est pas installé ou accessible" "Error"
    Write-Status "Télécharge et installe Java depuis: https://www.oracle.com/java/technologies/downloads/" "Warning"
    exit 1
}

# Chemin de Gradle
$gradleHome = "$env:USERPROFILE\.gradle\gradle-8.9"
$gradleZip = "$env:TEMP\gradle-8.9-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/gradle-8.9-bin.zip"

# Télécharger Gradle si nécessaire
if (-not (Test-Path "$gradleHome\bin\gradle.bat")) {
    Write-Status "Téléchargement de Gradle..." "Info"

    if (-not (Test-Path $gradleHome)) {
        New-Item -ItemType Directory -Path $gradleHome -Force | Out-Null
    }

    try {
        Write-Status "Téléchargement depuis: $gradleUrl" "Info"
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip -UseBasicParsing

        Write-Status "Extraction de Gradle..." "Info"
        $extractPath = Split-Path $gradleHome
        Expand-Archive -Path $gradleZip -DestinationPath $extractPath -Force

        Remove-Item $gradleZip -Force -ErrorAction SilentlyContinue
        Write-Status "✅ Gradle téléchargé et extrait!" "Success"
    } catch {
        Write-Status "❌ Erreur lors du téléchargement de Gradle" "Error"
        Write-Status $_.Exception.Message "Error"
        exit 1
    }
}

# Ajouter Gradle au PATH
$env:PATH = "$gradleHome\bin;$env:PATH"

# Ajouter les flags pour Java 21+
$env:GRADLE_OPTS = "--enable-native-access=ALL-UNNAMED -XX:+IgnoreUnrecognizedVMOptions"

# Exécuter le build
Write-Status "Exécution: gradle $Task" "Info"
Write-Status "============================================" "Info"

& gradle.bat $Task

if ($LASTEXITCODE -eq 0) {
    Write-Status "============================================" "Info"
    Write-Status "✅ BUILD SUCCESSFUL!" "Success"
    exit 0
} else {
    Write-Status "============================================" "Info"
    Write-Status "❌ BUILD FAILED!" "Error"
    exit 1
}
