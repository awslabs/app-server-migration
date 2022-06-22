# DESCRIPTION:
#
# Pulls arangodb docker image and runs it in the foreground on port 8529
#
# USAGE:
# ./arangoDB.ps1

# Running in-memory graph database ArangoDB

docker run  -d -p 8529:8529 -e ARANGO_ROOT_PASSWORD=openSesame arangodb/arangodb:3.8.3
