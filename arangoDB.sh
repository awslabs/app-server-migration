#!/usr/bin/env bash

set -o pipefail
set -o nounset
set -o errexit

# DESCRIPTION:
#
# Pulls arangodb docker image and runs it in the foreground on port 8529
# Takes java project path and destination path (ending with html file) as input parameters.
# Destination path will have the generated report
#
# USAGE:
# bash arangodb.sh

## docker
if ! sudo docker info > /dev/null 2>&1; then
  echo "This script uses docker, and it isn't running - please start docker and try again!"
  exit 1
fi
# pull arangodb docker image and tag it
##### Running in-memory graph database ArangoDB

sudo docker run  -d -p 8529:8529 -e ARANGO_ROOT_PASSWORD=openSesame arangodb/arangodb:3.8.3

