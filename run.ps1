# DESCRIPTION:
#
# Takes java project path and destination path (ending with html file) as input parameters.
# Destination path will have the generated report
#
# USAGE:
# ./run.ps1 <path to java project> <path to html report file ending with .html extension>

# run
param (
    [Parameter(Mandatory)] [string]$PathToJavaProject,
    [Parameter(Mandatory)] [string]$PathToHTMLReportFile,
    [Parameter(Mandatory)] [string]$arangoDbUsername,
    [Parameter(Mandatory)] [string]$arangoDbPassword
)

$USAGE="Usage: ./run.ps1 <path to java project> <path to html report file ending with .html extension> <arangoDb-username> <arangoDb-pwd>"
Write-Output $USAGE

java -jar target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar $PathToJavaProject $PathToHTMLReportFile $arangoDbUsername $arangoDbPassword