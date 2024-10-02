#!/usr/bin/env bash

set -o pipefail
set -o nounset
set -o errexit
# DESCRIPTION:
#
# Takes java project path and destination path (ending with html file) as input parameters.
# Destination path will have the generated report
#
# USAGE:
# chmod +x run.sh
# .run.sh <path to java project> <directory path for output html report file> <arangoDb-username> <arangoDb-pwd> <rule-names>

USAGE="Usage: run.sh <path to java project> <directory path for output html report file> <arangoDb-username> <arangoDb-pwd> <rule-names>"
[ $# -ne 5 ] && { echo "$USAGE"; exit; }

cp -r src/main/resources/lib "$2"

# run
java --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED -jar target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar "$1" "$2" "$3" "$4" "$5"