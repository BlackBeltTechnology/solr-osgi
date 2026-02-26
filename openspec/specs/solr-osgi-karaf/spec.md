# solr-osgi-karaf Specification

## Purpose
Packages a complete, pre-configured offline Apache Karaf distribution with all solr-osgi features and examples pre-installed, ready to run without internet access.

## Architecture
This is a `karaf-assembly` type module. The `karaf-maven-plugin` assembles a full Karaf distribution that includes the solr-osgi feature, example configsets, and all transitive dependencies. The distribution is runnable via the `./run-karaf` script.

## Requirements

### Requirement: Offline distribution
The assembly SHALL include all required bundles and features so that the distribution runs without needing to download dependencies at startup.

#### Scenario: Run without internet
- **GIVEN** the project has been built with `./mvnw clean install`
- **WHEN** `./run-karaf` is executed on a machine without internet access
- **THEN** Karaf starts successfully with the Solr services available

### Requirement: Example features
The distribution SHALL include the example configset features (`exampleCollection` and `exampleNorthwind`) available for installation.

#### Scenario: Install example feature
- **GIVEN** the Karaf distribution is running
- **WHEN** the `exampleCollection` feature is installed via the web console or CLI
- **THEN** the example Solr configset bundle is deployed and a core can be created against it
