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
# ./run.sh <path to java project> <path to html report file ending with .html extension>

USAGE="Usage: run.sh <path to java project> <path to html report file ending with .html extension> <arangoDb-username> <arangoDb-pwd>"
[ $# -ne 4 ] && { echo $USAGE; exit; }

USAGE="<path to html report> must end with .html extension"
[[ $2 != *.html ]]  && { echo $USAGE; exit; }

# run
java -jar target/app-server-migration-1.0.0-SNAPSHOT-jar-with-dependencies.jar $1 $2 $3 $4