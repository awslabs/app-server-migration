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
# Run Database (default root password will be openSesame)
bash arangoDB.sh
# Run Analyzer
Option 1: 
# This option is helpful when you want to check out projects from SCM and run the scan
# In this mode we provide repository details of projects(which needs to be scanned) 
# in the configuration file and provide path of configuration file as shown below

./run.sh config:<path/to/configurationfile> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD>

or
Option 2:
# This option is helpful when you already have source code downloaded on your machine
# In this mode we provide local path of the project 

./run.sh source:<path/to/project> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD>
e.g.
./run.sh source:/usr/example/project/ ~/test.html root openSesame
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

