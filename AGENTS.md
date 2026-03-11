# Solr OSGi - Project Documentation

## Project Overview


**Repository:** BlackBeltTechnology/solr-osgi
**License:** Apache License 2.0
**Java Version:** 11 (source/target); 21 for Karaf assembly runtime
**Build System:** Maven 3.9.4 with Maven Wrapper (`./mvnw`)

1. Embeds Apache Solr 7.0.1 inside an OSGi container (Apache Karaf 4.4.7) as managed bundles
2. Dynamically manages Solr cores and configsets through OSGi configuration admin and bundle tracking
3. Stores configset files in a Google JimFS in-memory filesystem for zero-disk-IO configset loading
4. Provides HTTP access to Solr Admin UI and REST APIs via OSGi HTTP Whiteboard services
5. Ships as a pre-built offline Karaf distribution with example configsets for quick evaluation

## Code Instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

## Directory Structure

```
solr-osgi/
├── solr-osgi-services/          # Core module: container management, configsets, core lifecycle
├── solr-osgi-http/              # HTTP module: servlets, dispatch filter, admin UI
├── solr-osgi-feature/           # Karaf feature descriptor (feature.xml generation)
├── solr-osgi-karaf/             # Offline Karaf assembly distribution
├── solr-osgi-example-configsets-exampleCollection/  # Example: basic Solr configset bundle
├── solr-osgi-example-configsets-dataimportNorthwind/ # Example: SQL DataImport configset bundle
├── images/                      # Documentation screenshots
├── override/etc/                # Karaf override configuration
├── .github/workflows/           # CI/CD GitHub Actions
└── .mvn/                        # Maven wrapper and extensions
```

## Core Modules

### Service Layer

| Module | Type | Purpose |
|--------|------|---------|
| `solr-osgi-services/` | OSGi bundle | Manages the Solr `CoreContainer`, tracks configset bundles via `BundleTrackerManager`, tracks core configurations via `ConfigurationTrackerManager`, creates/stops cores through `OsgiSolrFactory` |
| `solr-osgi-http/` | OSGi bundle | Registers Solr HTTP endpoints using OSGi HTTP Whiteboard: `SolrOsgiDispatchFilter` for request routing, `SolrContentServlet` for static resources, `LoadAdminUiServlet` for admin UI, and a Restlet servlet for `/schema/*` |

### Packaging & Distribution

| Module | Type | Purpose |
|--------|------|---------|
| `solr-osgi-feature/` | POM (karaf-maven-plugin) | Generates Karaf `feature.xml` aggregating all runtime bundles; verifies bundle compatibility |
| `solr-osgi-karaf/` | karaf-assembly | Pre-built offline Karaf distribution with Solr features pre-installed |

### Examples

| Module | Type | Purpose |
|--------|------|---------|
| `solr-osgi-example-configsets-exampleCollection/` | OSGi bundle | Minimal Solr configset bundle demonstrating the `Solr-Configset` manifest header pattern |
| `solr-osgi-example-configsets-dataimportNorthwind/` | OSGi bundle | DataImport handler example using MySQL Northwind database |

## Technology Stack

### Core Technologies
- **Apache Solr 7.0.1** — embedded search engine (solr-core)
- **OSGi Core 6.0.0** — module system and service registry
- **OSGi Declarative Services 1.3.0** — component lifecycle annotations (`@Component`, `@Reference`)
- **Apache Karaf 4.4.7** — OSGi container runtime
- **Apache CXF 3.5.1** — HTTP services framework
- **Google JimFS 1.1** — in-memory filesystem for configset storage
- **Google Guava 20.0** — utility library
- **Lombok 1.18.34** — boilerplate reduction (`@Getter`, `@Setter`, `@Slf4j`)

### Build & Quality
- **Maven 3.9.4** with Maven Wrapper
- **JUnit 5** (Jupiter) — unit testing
- **Apache Sling OSGi Mock 2.3.10** — OSGi mock framework for tests
- **JaCoCo 0.8.12** — code coverage
- **SonarQube** — static analysis (sonar.judo.technology)
- **Flatten Maven Plugin** — CI-friendly `${revision}` versioning
- **Felix Maven Bundle Plugin 5.1.2** — OSGi bundle generation
- **Lombok Maven Plugin** — delombok for Javadoc generation

## Build Commands

```bash
# Full build (compile + test + package + install)
./mvnw clean install

# Build skipping tests
./mvnw clean install -DskipTests

# Run tests only
./mvnw clean test

# Build a single module
./mvnw clean install -pl solr-osgi-services

# Run a single test class
./mvnw test -pl solr-osgi-services -Dtest=OsgiSolrFactoryTest

# Skip submodule build (parent only)
./mvnw -DskipModules=true clean install
```

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Active by default; includes all submodules. Deactivate with `-DskipModules=true` |
| `sign-artifacts` | Signs artifacts with `sign-maven-plugin` for release |
| `release-central` | Deploys to Maven Central (OSS Sonatype) |
| `release-judong` | Deploys to Judong Nexus (internal) |
| `generate-github-asciidoc-diagrams` | Generates PNG diagrams from AsciiDoc PlantUML blocks |
| `update-source-code-license` | Updates Apache 2.0 license headers in source files |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Parent POM: version properties, dependency management, plugin configuration |
| `.mvn/extensions.xml` | Maven extensions: wagon-file, wagon-webdav, buildtime extension, profile-activator |
| `logback-test.xml` | Logback configuration for test execution |
| `solr-osgi-feature/src/main/feature/feature.xml` | Karaf feature template for bundle aggregation |
| `override/etc/` | Karaf override configuration files |

## Development Environment

**Required:**
- Java 11 JDK (Java 21 for running the Karaf assembly)
- Maven 3.9.4+ (or use `./mvnw`)

**Testing:**
- Tests use JUnit 5 with Apache Sling OSGi Mock
- Test logging configured via `logback-test.xml` at project root
- Surefire configured with `--add-opens` flags for Java module system compatibility
- JaCoCo agent attached for coverage collection

## Git Workflow

- **Main Branch:** `develop`
- **Release Branch:** `master` (latest released version)
- **Versioning:** CI-friendly `${revision}` property, currently `1.0.3-SNAPSHOT`
- **Branching Model:** GitFlow — feature/, release/, bugfix/, support/, hotfix/ branches
- **Commit Rule:** Every commit must reference a JIRA ticket (`JNG-xxx`)
- **CI:** GitHub Actions on `judong` runner (see [CIFLOW.md](.github/CIFLOW.md))

## Important Notes

1. **OSGi Configuration PIDs** drive the entire lifecycle: `solr.corecontainer` (container), `solr.core-<name>` (individual cores), `solr.http` (HTTP endpoints)
2. **ConfigSet bundles** must have a `Solr-Configset` manifest header and files at `configsets/<name>/conf/` inside the JAR
3. **In-memory filesystem** (JimFS) stores configsets — no disk I/O for configuration files
4. **Core lifecycle** is automatic: cores are created when both config AND configset are available, stopped when either disappears (data directory is preserved)
5. **Not all Solr features supported** — clustering and sharding are untested; only configSet-based cores work
6. The `deployOnly` property can skip install/test phases when only deployment is needed

## Related Documentation

- [README.md](README.md) — Installation, usage, and architecture overview with diagrams
- [CONTRIBUTING.md](CONTRIBUTING.md) — Development setup and submission guidelines
- [.github/CIFLOW.md](.github/CIFLOW.md) — CI/CD pipeline documentation with flow diagrams
