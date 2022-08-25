# DESCRIPTION:
#
# Takes java project path and destination path (ending with html file) as input parameters.
# Destination path will have the generated report
#
# USAGE:
# ./run.ps1 <path to java project> <directory path for output html report file> <arangoDb-username> <arangoDb-pwd>

# run
param (
    [Parameter(Mandatory)] [string]$PathToJavaProject,
    [Parameter(Mandatory)] [string]$PathToOutputDirectory,
    [Parameter(Mandatory)] [string]$arangoDbUsername,
    [Parameter(Mandatory)] [string]$arangoDbPassword
)

$USAGE="Usage: ./run.ps1 <path to java project> <directory path for output html report file> <arangoDb-username> <arangoDb-pwd>"
Write-Output $USAGE

Copy-Item -Path "src/main/resources/lib" -Destination $PathToOutputDirectory -Recurse
java -jar target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar $PathToJavaProject $PathToOutputDirectory $arangoDbUsername $arangoDbPassword