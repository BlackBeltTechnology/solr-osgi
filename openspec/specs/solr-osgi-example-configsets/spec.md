# solr-osgi-example-configsets Specification

## Purpose
Provides example Solr configset bundles that demonstrate how to package Solr configuration as OSGi bundles for use with solr-osgi-services. Includes a basic collection example and a DataImport handler example.

## Architecture
Each example is an OSGi bundle with a `Solr-Configset` manifest header. The bundle contains Solr configuration files (schema.xml, solrconfig.xml, etc.) at `configsets/<name>/conf/`. The `maven-bundle-plugin` generates the manifest with the required header. The exampleCollection provides a minimal setup; dataimportNorthwind demonstrates SQL data import from a MySQL Northwind database.

## Requirements

### Requirement: ConfigSet bundle manifest
Each example bundle SHALL have a `Solr-Configset` manifest header with the configset name.

#### Scenario: exampleCollection bundle
- **WHEN** the `solr-osgi-example-configsets-exampleCollection` bundle is inspected
- **THEN** its manifest contains `Solr-Configset: exampleCollection`

### Requirement: ConfigSet file structure
Each bundle SHALL contain Solr configuration files at the standard path `configsets/<name>/conf/`.

#### Scenario: exampleCollection files
- **WHEN** the exampleCollection bundle is deployed to an OSGi container
- **THEN** `SolrCoreContainerManager` finds and copies files from `configsets/exampleCollection/conf/` including `schema.xml`, `solrconfig.xml`, `stopwords.txt`, `synonyms.txt`, and `protwords.txt`

### Requirement: DataImport configuration
The dataimportNorthwind example SHALL include a `data-config.xml` configuring SQL import from a MySQL Northwind database.

#### Scenario: DataImport execution
- **GIVEN** a MySQL Northwind container is running on port 3306
- **WHEN** the dataimportNorthwind feature is installed and a DataImport execute command is issued
- **THEN** data is imported from the MySQL database into the Solr core's index
