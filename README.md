## AppServerMigration

### About AppServerMigration

Application Server Migration automates the discovery process of migrating the code from source server to target server based on Java programming language.

### Cloning the project
```bash
git clone git@github.com:awslabs/app-server-migration.git
cd app-server-migration
```

### Build the project
Prior to building the project, ensure that you have the following tools installed in your machine:
- Java 8
- Docker
- Maven
- Git

For Linux (Ubuntu, CentOS and RHEL) and Mac OS, you may execute the `./setup.sh` script to install the above dependencies.
For Windows, kindly follow their official documentation guide for installation.
- Java 8
	- https://www.java.com/en/download/help/windows_manual_download.html
	- https://www.oracle.com/java/technologies/downloads/#java8-windows
- Docker
	- https://docs.docker.com/desktop/windows/install/
	- https://docs.microsoft.com/en-us/windows/wsl/tutorials/wsl-containers
- Maven
	- https://maven.apache.org/download.cgi
	- https://maven.apache.org/guides/getting-started/windows-prerequisites.html
	- https://maven.apache.org/install.html
- Git
	- https://git-scm.com/download/win

Build the project using `mvn package` command.

### Run the project
#### For Linux and MacOS machines
```bash
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

#### For Windows machines
```powershell
# Run Database (default root password will be openSesame)
powershell ./arangoDB.ps1 config:<path/to/configurationfile> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD>
# Run Analyzer
Option 1: 
# This option is helpful when you want to check out projects from SCM and run the scan
# In this mode we provide repository details of projects(which needs to be scanned) 
# in the configuration file and provide path of configuration file as shown below

powershell ./run.ps1 config:<path/to/configurationfile> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD>

or
Option 2:
# This option is helpful when you already have source code downloaded on your machine
# In this mode we provide local path of the project 

powershell ./run.ps1 source:<path/to/project> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD>
e.g.
powershell ./run.ps1 source:/usr/example/project/ ~/test.html root openSesame
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

