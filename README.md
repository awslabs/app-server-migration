## AppServerMigration

### About AppServerMigration

AppServerMigration, an open-source software solution, analyses Java applications to deliver precise effort estimations in person-days, facilitating a seamless transition from source to target states. This tool adeptly identifies necessary modifications, offers insightful recommendations, and presents a comprehensive HTML report for a well-guided migration. It accelerates the migration journey, eliminating the necessity for re-work, ensuring a successful and efficient transition with minimal setbacks.

AppServerMigration employs a rule-based analysis, meticulously examining Java applications according to predefined rules. This method ensures a thorough assessment, enabling precise identification of necessary changes and providing accurate effort estimations.

**NEW: AI-Powered Analysis** - AppServerMigration now includes AI-powered analysis using Amazon Bedrock, providing intelligent migration recommendations and effort estimations alongside traditional rule-based analysis.

Effort estimations in AppServerMigration leverage QSM's (Quantitative Software Management) industry-standard metrics and incorporate the backtracking technique. Customizations are then applied, drawing from firsthand migration experiences. This tailored approach ensures precise calculations, aligning with project nuances and enhancing the accuracy of migration effort predictions.

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

### AWS Bedrock Setup (Required for AI Features)

To use the AI-powered analysis features, you need to configure AWS Bedrock access:

#### Prerequisites

1. **AWS Account** with access to Amazon Bedrock
2. **AWS Credentials** configured on your machine (via AWS CLI, environment variables, or IAM role)
3. **Model Access** - Enable Claude 3.5 Sonnet model in Amazon Bedrock

#### Step 1: Configure AWS Credentials

Ensure you have AWS credentials configured on your machine. The application will use the default AWS credential provider chain. You can configure credentials using any of these methods:

- AWS CLI: `aws configure`
- Environment variables: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`
- IAM role (if running on EC2 or other AWS services)
- AWS credentials file (`~/.aws/credentials`)

For detailed instructions, see: [AWS CLI Configuration](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html)

#### Step 2: Enable Amazon Bedrock Model Access

Amazon Bedrock now offers simplified model access. Follow the instructions in this guide to enable Claude 3.5 Sonnet:

**[Simplified Amazon Bedrock Model Access Guide](https://aws.amazon.com/blogs/security/simplified-amazon-bedrock-model-access/)**

Key steps:

1. Navigate to the Amazon Bedrock Console
2. Enable access to Anthropic's Claude 3.5 Sonnet model
3. Model access is typically granted instantly

#### IAM Permissions Required

Ensure your AWS IAM user/role has the following permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["bedrock:InvokeModel"],
      "Resource": [
        "arn:aws:bedrock:*::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0"
      ]
    }
  ]
}
```

**Additional Resources:**

- [Amazon Bedrock User Guide](https://docs.aws.amazon.com/bedrock/latest/userguide/what-is-bedrock.html)
- [Model Access Documentation](https://docs.aws.amazon.com/bedrock/latest/userguide/model-access.html)

### Run the project

#### For Linux and MacOS machines

```bash
# Run Database
# During installation script may ask for password, enter your system password.
# After installation ArangoDB Web interface is accessible on http://localhost:8529
# (using default user name: root and password: openSesame)
bash arangoDB.sh

# Run Analyzer
Option 1:
# This option is helpful when you want to check out projects from SCM and run the scan
# In this mode we provide repository details of projects(which needs to be scanned)
# in the configuration file and provide path of configuration file as shown below

./run.sh config:<path/to/configurationfile> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD> <Single or multiple comma separated rule names>

or
Option 2:
# This option is helpful when you already have source code downloaded on your machine
# In this mode we provide local path of the project

./run.sh source:<path/to/project> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD> <Single or multiple comma separated rule names>

# Example without AI:
./run.sh source:/usr/example/project/ ~/test-directory root openSesame oracle-to-postgres,weblogic-to-tomcat

# Example with AI-powered analysis (NEW):
./run.sh source:/usr/example/project/ ~/test-directory root openSesame weblogic-to-tomcat AIEnabled
```

#### For Windows machines

```powershell
# Run Database (default root password will be openSesame)
powershell ./arangoDB.ps1

# Run Analyzer
Option 1:
# This option is helpful when you want to check out projects from SCM and run the scan
# In this mode we provide repository details of projects(which needs to be scanned)
# in the configuration file and provide path of configuration file as shown below

powershell ./run.ps1 config:<path/to/configurationfile> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD> <Single or multiple comma separated rule names>

or
Option 2:
# This option is helpful when you already have source code downloaded on your machine
# In this mode we provide local path of the project

powershell ./run.ps1 source:<path/to/project> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD> <Single or multiple comma separated rule names>

# Example without AI:
powershell ./run.ps1 source:/usr/example/project/ /usr/example/project/reports root openSesame oracle-to-postgres,weblogic-to-tomcat

# Example with AI-powered analysis (NEW):
powershell ./run.ps1 source:/usr/example/project/ /usr/example/project/reports root openSesame weblogic-to-tomcat AIEnabled
```

### AI-Powered Analysis (NEW Feature)

AppServerMigration now supports AI-powered analysis using Amazon Bedrock's Claude 3.5 Sonnet model. This feature provides:

#### Benefits of AI Analysis

- **Intelligent Code Analysis**: Deep understanding of code patterns and migration challenges
- **Context-Aware Recommendations**: Recommendations tailored to your specific codebase
- **Comprehensive Coverage**: Analyzes files that may not be covered by predefined rules
- **Detailed Explanations**: Clear descriptions of why changes are needed and how to implement them
- **Effort Estimation**: AI-generated effort estimates based on code complexity

#### How to Enable AI Analysis

Add `AIEnabled` as the 6th parameter when running the analyzer:

```bash
./run.sh source:<path/to/project> <destination/path/> <ARANGO_USERNAME> <ARANGO_ROOT_PASSWORD> <rule-names> AIEnabled
```

**Example:**

```bash
./run.sh source:/home/user/my-weblogic-app /home/user/reports root openSesame weblogic-to-tomcat AIEnabled
```

#### Understanding AI Reports

When AI analysis is enabled, you'll receive:

1. **Traditional Rule-Based Report** - Standard HTML report with predefined rule matches
2. **AI-Generated Report** - Separate HTML report with AI-powered findings and recommendations
3. **Summary Report** - Combined view showing both traditional and AI effort estimates

**AI Report Features:**

- **AI-Generated Findings**: Specific code issues identified by AI analysis
- **Smart Recommendations**: Actionable steps with priority levels (High/Medium/Low)
- **File-Level Details**: Exact file paths and line numbers for each finding
- **Complexity Assessment**: AI-evaluated complexity (minor/moderate/critical)
- **Effort Estimation**: Person-days estimates for migration work

#### AI Report Structure

The AI report includes:

- **Summary Cards**: Project overview, migration path, rules generated, estimated effort
- **Findings Section**: Detailed list of migration issues with:
  - Issue description
  - Affected file and line number
  - Complexity level
  - Rule ID for tracking
- **Recommendations Section**: Prioritized action items with:
  - Recommendation title and description
  - Priority level (High/Medium/Low)
  - Number of affected files
  - Implementation guidance

#### Comparison: Traditional vs AI Analysis

| Feature               | Traditional Analysis   | AI Analysis                      |
| --------------------- | ---------------------- | -------------------------------- |
| **Coverage**          | Predefined rules only  | Comprehensive code understanding |
| **Flexibility**       | Fixed rule patterns    | Adapts to code context           |
| **Recommendations**   | Generic guidance       | Context-specific advice          |
| **New Patterns**      | Requires rule updates  | Identifies novel issues          |
| **Effort Estimation** | Rule-based calculation | AI-evaluated complexity          |

#### Best Practices

1. **Use Both Approaches**: Run with `AIEnabled` to get comprehensive analysis
2. **Review AI Findings**: AI recommendations complement traditional rules
3. **Prioritize by Severity**: Focus on High priority AI recommendations first
4. **Validate Estimates**: Compare traditional and AI effort estimates
5. **AWS Costs**: Be aware that AI analysis uses Amazon Bedrock (charges apply)

### Generated Reports

After running the analysis, you'll find the following reports in your destination directory:

1. **`<project-name>-Report.html`** - Traditional rule-based analysis report
2. **`<project-name>-<source>-to-<target>-AI-Report.html`** - AI-powered analysis report (when AIEnabled)
3. **`summaryreport.html`** - Summary of all analyzed projects with combined estimates

### Create custom rules

You may create your own rules which can be fed to the rule engine in order to assess to your source files based on your rules. There are 2 files which you need to create `rules.json` and `recommendations.json`. For reference, you can check [oracle-to-postgres-javarules.json](https://github.com/awslabs/app-server-migration/blob/main/src/main/resources/oracle-to-postgres-javarules.json) and [oracle-to-postgres-recommendations.json](https://github.com/awslabs/app-server-migration/blob/main/src/main/resources/oracle-to-postgres-recommendations.json) respectively.

The `rules.json` file would look like:

```json
{
  "analyzer": "com.amazon.aws.am2.appmig.estimate.java.JavaFileAnalyzer",
  "file_type": "java",
  "rules": [
    {
      "id": 1,
      "name": "Name",
      "description": "Detailed Description",
      "complexity": "minor",
      "rule_type": "package",
      "remove": {
        "import": ["java.sql.DriverManager", "oracle.jdbc.driver.OracleDriver"]
      },
      "recommendation": 12
    }
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
  - `rule_type`: Denotes where to search to find a rule match. In the above example rule, it will look for `import` statements to search for imported packages. The processing logic is coded in the Analyzer.
  - `remove`: Action denoting elimination of attributes present inside it. In the above example, the rule will match against any `import` statement having `package` name either `java.sql.DriverManager` or `oracle.jdbc.driver.OracleDriver`.
  - `recommendation`: Maps to the identifier of recommendation present in the associated `recommendations.json` file.

The `recommendations.json` file would look like:

```json
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
