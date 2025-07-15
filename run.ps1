# DESCRIPTION:
#
# Takes java project path and destination path (ending with html file) as input parameters.
# Destination path will have the generated report
#
# USAGE:
# ./run.ps1 <path to java project> <directory path for output html report file> <arangoDb-username> <arangoDb-pwd> <rule-names>

# run
param (
    [Parameter(Mandatory)] [string]$PathToJavaProject,
    [Parameter(Mandatory)] [string]$PathToOutputDirectory,
    [Parameter(Mandatory)] [string]$arangoDbUsername,
    [Parameter(Mandatory)] [string]$arangoDbPassword,
    [Parameter(Mandatory)] [string]$ruleNames
)

# Resolve script directory
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Ensure logs directory exists alongside the script
$LogDir = Join-Path $ScriptDir 'logs'
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

Write-Output "Starting App Server Migration Tool"
Write-Output "Source: $PathToJavaProject"
Write-Output "Output: $PathToOutputDirectory"
Write-Output "Rules:  $ruleNames"

# Ensure output directory exists
if (-not (Test-Path $PathToOutputDirectory)) {
    New-Item -ItemType Directory -Path $PathToOutputDirectory | Out-Null
}

# Copy HTML assets if they exist
$LibSource = Join-Path $ScriptDir 'src/main/resources/lib'
if (Test-Path $LibSource) {
    Write-Output "Copying HTML assets..."
    $LibDest = Join-Path $PathToOutputDirectory 'lib'
    Copy-Item -Path $LibSource -Destination $LibDest -Recurse -Force
} else {
    Write-Output "src/main/resources/lib not found. Skipping copy."
}

Write-Output "Running analysis..."
$Timestamp = Get-Date -Format "yyyy-MM-dd_HH_mm_ss"
#Generate unique timestamp (YYYY-MM-DD_HH_MM_SS) to create distinct log file names for each execution
$LogFile = Join-Path $LogDir "app-mig_${Timestamp}.log"

# Path to your fat JAR
$JarPath = Join-Path $ScriptDir 'target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar'

& java `
    --add-opens java.base/java.util=ALL-UNNAMED `
    --add-opens java.base/java.lang=ALL-UNNAMED `
    -Dlogfile="$LogFile" `
    -jar $JarPath `
    $PathToJavaProject $PathToOutputDirectory $arangoDbUsername $arangoDbPassword $ruleNames

Write-Output "Done! Report: $PathToOutputDirectory"
