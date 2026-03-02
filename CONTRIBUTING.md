# Contributing to Solr OSGi

## Development Environment

Make sure your development environment meets the requirements described in the parent project's [CONTRIBUTING guide](https://github.com/BlackBeltTechnology/judo-community/blob/develop/CONTRIBUTING.adoc). In particular, you need:

- **Java 11** JDK (Java 21 for the Karaf assembly module at runtime)
- **Maven 3.9.4+** (or use the included Maven Wrapper: `./mvnw`)

## Code Structure

This project follows a standard Maven multi-module layout. See [README.md](README.md) for the module architecture and how the components interact.

## Build Commands

```bash
# Run tests only
./mvnw clean test

# Full build (compile + test + package + install)
./mvnw clean install

# Build a single module
./mvnw clean install -pl solr-osgi-services

# Run a single test class
./mvnw test -pl solr-osgi-services -Dtest=OsgiSolrFactoryTest
```

## Submitting an Issue

Before filing a new issue, search the [issue tracker](https://github.com/BlackBeltTechnology/solr-osgi/issues) — your problem may already be reported or resolved.

When reporting a bug, include:

- Output of `java -version` and `mvn -version`
- The relevant `pom.xml` or `.flattened-pom.xml`
- A minimal reproducible scenario — this is the most important part and helps maintainers fix bugs faster

File new issues via the [issue form](https://github.com/BlackBeltTechnology/solr-osgi/issues/new/choose).

## Submitting a Pull Request

This project uses [GitHub's standard forking model](https://guides.github.com/activities/forking/). Fork the repository, make your changes on a feature branch, and submit a pull request.
