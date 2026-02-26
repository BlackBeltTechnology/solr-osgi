# solr-osgi-http Specification

## Purpose
Provides HTTP access to the embedded Solr instance by registering servlets, filters, and an HTTP context through the OSGi HTTP Whiteboard pattern, exposing the Solr Admin UI and REST API.

## Architecture
`SolrHttpServiceManager` is an OSGi DS component activated by the `solr.http` configuration PID. It holds a `@Reference` to `CoreContainer` (provided by `solr-osgi-services`). On activation it registers five HTTP whiteboard services: `SolrOsgiHttpContext` (named context), `SolrContentServlet` (static resources), `SolrOsgiDispatchFilter` (request routing), `LoadAdminUiServlet` (admin UI), and a Restlet `ServerServlet` (schema REST API). `SolrOsgiDispatchFilter` extends Solr's `SolrDispatchFilter` with configurable URL exclusion patterns.

## Requirements

### Requirement: HTTP endpoint activation
`SolrHttpServiceManager` SHALL activate when both `solr.http` configuration and a `CoreContainer` OSGi service are available.

#### Scenario: HTTP service startup
- **GIVEN** a running `CoreContainer` and no `solr.http` configuration
- **WHEN** `solr.http.cfg` is deployed with `contextRoot=/solr`
- **THEN** `SolrHttpServiceManager.activate()` registers all HTTP whiteboard services and Solr becomes accessible at the configured context root

#### Scenario: HTTP service shutdown
- **GIVEN** the HTTP services are registered
- **WHEN** the `solr.http` configuration is removed
- **THEN** `deactivate()` unregisters all five `ServiceRegistration` objects

### Requirement: Request dispatch with exclusion patterns
`SolrOsgiDispatchFilter` SHALL route requests to Solr's `SolrDispatchFilter` except for paths matching configured exclusion patterns.

#### Scenario: Normal Solr request
- **GIVEN** the dispatch filter is registered with default exclude patterns
- **WHEN** a request arrives for `/solr/test1/select?q=*:*`
- **THEN** the request is passed to `SolrDispatchFilter.doFilter()` for Solr processing

#### Scenario: Excluded static resource
- **GIVEN** the exclude pattern includes `/img/.+`
- **WHEN** a request arrives for `/solr/img/logo.png`
- **THEN** the request bypasses Solr dispatch and is passed to `FilterChain.doFilter()`

### Requirement: Static resource serving
`SolrContentServlet` SHALL serve Solr Admin UI static resources (HTML, CSS, JS, images) with correct MIME types and security headers.

#### Scenario: Serve static file
- **WHEN** a GET request arrives for a `.css` file
- **THEN** `SolrContentServlet.doGet()` returns the file content with `Content-Type: text/css` and `X-Frame-Options` header set

### Requirement: HTTP context registration
`SolrOsgiHttpContext` SHALL provide a named HTTP context (`solr`) that resolves resources from the Solr bundle.

#### Scenario: Resource resolution
- **WHEN** a resource is requested via `getResource("/index.html")`
- **THEN** the context resolves it from the Solr bundle's classpath

### Requirement: Schema REST API
The manager SHALL register a Restlet `ServerServlet` at `/schema/*` for Solr's schema REST API.

#### Scenario: Schema endpoint
- **GIVEN** the HTTP services are active
- **WHEN** a request arrives at `/solr/schema/fields`
- **THEN** the Restlet servlet handles the schema API request
