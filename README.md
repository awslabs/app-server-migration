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
# Run Database 
# During installation script may ask for password, enter your system password.
# After installation AurangoDB Web interface is accessible on http://localhost:8529 (using default user name: root and password: openSesame)
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

./run.sh source:<path/to/project> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD> <SINGLE_OR_MULTIPLE_COMMA_SEPARATED_RULE_NAMES_WITH_RECOMMENDATIONS_FILE>
e.g.
./run.sh source:/usr/example/project/ ~/test-directory root openSesame oracle-to-postgres-javarules,oracle-to-postgres-mvnrules,oracle-to-postgres-recommendations.json

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

### Create custom rules
You may create your own rules which can be fed to the rule engine in order to assess to your source files based on your rules. There are 2 files which you need to create `rules.json` and `recommendations.json`. For reference, you can check [oracle-to-postgres-javarules.json](https://github.com/awslabs/app-server-migration/blob/main/src/main/resources/oracle-to-postgres-javarules.json) and [oracle-to-postgres-recommendations.json](https://github.com/awslabs/app-server-migration/blob/main/src/main/resources/oracle-to-postgres-recommendations.json) respectively.

The `rules.json` file would look like:

``` json
{
	"analyzer": "com.amazon.aws.am2.appmig.estimate.java.JavaFileAnalyzer",
	"file_type": "java",
	"rules": [
		{
			"id": 1,
			"name": "Name",
			"description": "Detailed Description",
			"complexity": "minor",
			"mhrs": 1,
			"rule_type": "package",
			"remove": {
				"import": ["java.sql.DriverManager","oracle.jdbc.driver.OracleDriver"]
			},
			"recommendation": 12
		},
	]
}
```

Understanding each key of above JSON file:
- `analyzer`: Canonical name of the analyzer class. In the above example, we are using [JavaFileAnalyzer.java](https://github.com/awslabs/app-server-migration/blob/main/src/main/java/com/amazon/aws/am2/appmig/estimate/java/JavaFileAnalyzer.java). You may create your own Analyzer by implementing `IAnalyzer` interface.
- `file_type`: Type of source files which will be assessed
- `rules`: Array of objects, each corresonding to a `rule`
	- `id`: Rule identifier
	- `name`: Name of the rule
	- `description`: A verbose rule description 
	- `complexity`: AppServerMigration identifies the complexity of migration per application either as minor, major or critical, depending on the features that need to be converted to make the application target compatible. If the changes are only in the configurations and not in the code, then it is minor. Major category involves code changes. There might be features specific to the source server which are not supported on the target server. In such scenarios, the whole functionality needs to be re-written. Such categories fall under critical complexity. For instance, trying to migrate a web application from Oracle WebLogic to Apache Tomcat, which has EJB code.
	- `mhrs`: Effort estimations in person hours to migrate the application. Report shows the time required to migrate per feature, along with the total time needed to migrate the entire application.
	- `rule_type`: Denotes where to search to find a rule match. In the above example rule, it will look for `import` statements to search for imported packages. The processing logic is coded in the Analyzer.
	- `remove`: Action denoting elimination of attributes present inside it. In the above example, the rule will match against any `import` statement having `package` name either `java.sql.DriverManager` or `oracle.jdbc.driver.OracleDriver`.
	- `recommendation`: Maps to the identifier of recommendation present in the associated `recommendations.json` file.

The `recommendations.json` file would look like:

``` json
"recommendations": [
		{
			"id": 1,
			"name": "Replace Oracle database driver with Postgres database driver",
			"description": "Review the driver being loaded in this block of code and change the driver from oracle.jdbc.driver.OracleDriver to  org.postgresql.Driver"
		},
]

```

Understanding each key of above JSON file:
- `recommendations`: Array of objects, each representing a recommendation.
	- `id`: Recommendation identifier
	- `name`: Name of the recommendation, which will be displayed on the report
	- `description`: A verbose recommendation description, which will be displayed on the report


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

