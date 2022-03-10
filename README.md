## AppServerMigration

### About AppServerMigration

Application Server Migration automates the process of migrating the code from source server to target server

### Building the project
```
# build and install app-server-migration
git clone git@github.com:awslabs/app-server-migration.git
cd app-server-migration
mvn package
```

### Run the project
```bash
# Install dependencies
./setup.sh
# Run DB
bash arangodb.sh
# Run Analyzer
./run.sh <path/to/project> <destination/path/html/file> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD>
```

# To run ArangoDB in local (alternative to running arango.sh)
---
##### Running in-memory graph database ArangoDB

`docker run -p 8529:8529 -e ARANGO_ROOT_PASSWORD=openSesame arangodb/arangodb:3.8.3`

##### Connecting to ArangoDB UI
`http://localhost:8529/`

##### You can find the name of the container by running
`docker ps`

To retrieve the HOST run the following command with container ID obtained from `docker ps`

`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <<CONTAINER ID>>`

##### To stop a ArangoDB database instance, run the following command
`docker stop CONTAINER_NAME`


## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.

