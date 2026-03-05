# solr-osgi-feature Specification

## Purpose
Defines the Apache Karaf feature descriptor that aggregates all solr-osgi runtime bundles and their transitive dependencies into installable Karaf features.

## Architecture
This is a POM-type module that uses the `karaf-maven-plugin` to generate a `feature.xml` file. The plugin analyzes Maven dependencies and produces Karaf feature definitions that bundle all required OSGi bundles. The `verify` goal ensures all bundles resolve correctly in a Karaf environment.

## Requirements

### Requirement: Feature descriptor generation
The module SHALL produce a valid Karaf `feature.xml` that includes all runtime bundles needed for solr-osgi.

#### Scenario: Feature installation
- **GIVEN** a running Karaf instance with CXF feature repository added
- **WHEN** the solr-osgi feature repository is added and `solr-http` feature is installed
- **THEN** all bundles from `solr-osgi-services`, `solr-osgi-http`, and their dependencies are resolved and started

### Requirement: Bundle verification
The feature descriptor SHALL pass Karaf's bundle resolution verification.

#### Scenario: Verify goal
- **WHEN** `./mvnw verify -pl solr-osgi-feature` is executed
- **THEN** the karaf-maven-plugin `verify` goal confirms all bundles resolve without missing imports
