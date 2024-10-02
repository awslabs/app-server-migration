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

$USAGE="Usage: ./run.ps1 <path to java project> <directory path for output html report file> <arangoDb-username> <arangoDb-pwd> <rule-names>"
Write-Output $USAGE

Copy-Item -Path "src/main/resources/lib" -Destination $PathToOutputDirectory -Recurse
java --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar $PathToJavaProject $PathToOutputDirectory $arangoDbUsername $arangoDbPassword $ruleNames