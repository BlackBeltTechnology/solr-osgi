# solr-osgi-services Specification

## Purpose
Manages the embedded Apache Solr `CoreContainer` within an OSGi environment, dynamically tracking configset bundles and core configuration PIDs to create, start, and stop Solr cores at runtime.

## Architecture
The module centers on `SolrCoreContainerManager`, an OSGi DS component activated by the `solr.corecontainer` configuration PID. It uses `BundleTrackerManager` to detect bundles with `Solr-Configset` manifest headers and `ConfigurationTrackerManager` to detect `solr.core-*` PIDs. `OsgiSolrFactory` creates the `CoreContainer` and individual cores. `OsgiConfigSetService` replaces Solr's default configset loader to read from a JimFS in-memory filesystem. `SolrXmlGenerator` produces the `solr.xml` configuration from OSGi config properties.

## Requirements

### Requirement: CoreContainer lifecycle management
The `SolrCoreContainerManager` SHALL activate when a `solr.corecontainer` configuration PID is present and create an embedded Solr `CoreContainer`.

#### Scenario: Container activation
- **GIVEN** no `solr.corecontainer` configuration exists
- **WHEN** a `solr.corecontainer.cfg` file is deployed with `solrHome=/tmp/solr`
- **THEN** `SolrCoreContainerManager.activate()` is called and a `CoreContainer` is created and registered as an OSGi service

#### Scenario: Container deactivation
- **GIVEN** a running `CoreContainer` managed by `SolrCoreContainerManager`
- **WHEN** the `solr.corecontainer` configuration is removed
- **THEN** `deactivate()` shuts down the `CoreContainer`, unregisters the OSGi service, and closes the JimFS filesystem

### Requirement: ConfigSet bundle tracking
The manager SHALL detect OSGi bundles with a `Solr-Configset` manifest header and copy their configuration files into the in-memory filesystem.

#### Scenario: ConfigSet bundle deployment
- **GIVEN** a running `SolrCoreContainerManager`
- **WHEN** a bundle with manifest header `Solr-Configset: exampleCollection` is deployed
- **THEN** files at `configsets/exampleCollection/conf/` inside the bundle are copied to the JimFS filesystem and `exampleCollection` is added to `loadedConfigSet`

#### Scenario: ConfigSet bundle removal
- **GIVEN** a configset `exampleCollection` is loaded and cores reference it
- **WHEN** the bundle providing `exampleCollection` is uninstalled
- **THEN** the configset is removed from `loadedConfigSet` and `refreshCores()` stops any cores that depended on it

### Requirement: Core configuration tracking
The manager SHALL track OSGi configuration PIDs matching `solr.core` or `solr.core-*` and create Solr cores when both configuration and configset are available.

#### Scenario: Core creation
- **GIVEN** configset `exampleCollection` is loaded
- **WHEN** a configuration with PID `solr.core-test1` is created with properties `name=test1` and `configSet=exampleCollection`
- **THEN** `OsgiSolrFactory.createServer()` creates the core and an `EmbeddedSolrServer` is returned

#### Scenario: Core removal
- **GIVEN** core `test1` is running
- **WHEN** the `solr.core-test1` configuration is deleted
- **THEN** `OsgiSolrFactory.stopServer()` unloads the core via `CoreAdminRequest.Unload` but preserves the data directory

### Requirement: solr.xml generation
`SolrXmlGenerator.getSolrXml()` SHALL produce valid Solr XML configuration from `SolrCoreContainerConfig` properties.

#### Scenario: Minimal configuration
- **WHEN** `SolrCoreContainerConfig` has default values
- **THEN** the generated XML contains `<solr>` root with `<str name="coreRootDirectory">` and `<str name="configSetBaseDir">` elements

#### Scenario: SolrCloud configuration
- **GIVEN** `SolrCoreContainerConfig` has `zkHost` set to `localhost:2181`
- **WHEN** `getSolrXml()` is called
- **THEN** the generated XML includes a `<solrcloud>` section with ZooKeeper host configuration

### Requirement: In-memory configset storage
`OsgiConfigSetService` SHALL load configsets from the JimFS in-memory filesystem rather than the disk.

#### Scenario: Resource loader creation
- **GIVEN** a core descriptor referencing configset `exampleCollection`
- **WHEN** `createCoreResourceLoader()` is called
- **THEN** it returns a `SolrResourceLoader` pointing to the JimFS path `configsets/exampleCollection/conf/`
