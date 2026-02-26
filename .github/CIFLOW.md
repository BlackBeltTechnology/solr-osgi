# CI/CD Flow — Development Versioning and Branch Handling

This document describes the branching strategy, version numbering, and GitHub Actions CI/CD pipelines used by this project.

## Branching Strategy

The project follows a GitFlow-based model with these branch types:

| Branch Pattern | Base | Purpose |
|----------------|------|---------|
| `develop` | — | Main development branch; contains latest sources for the active version |
| `feature/JNG-xxx_summary` | `develop` | New features for the current version |
| `release/x.y.z` or `x_y_z` | `develop` | Release stabilization branches |
| `bugfix/JNG-xxx_summary` | release branch | Bug fixes applied during release testing |
| `support/JNG-xxx_summary` | release branch | Minor post-release changes merged back to the release branch |
| `master` | — | Latest released sources |
| `hotfix/JNG-xxx_summary` | `master` | Urgent fixes applied to both release and master |

```mermaid
gitGraph
    commit id: "initial"
    branch develop
    commit id: "dev-1"
    branch feature/JNG-1
    commit id: "feat-1"
    commit id: "feat-2"
    checkout develop
    merge feature/JNG-1 id: "merge-feat"
    branch release/1.0-beta1
    commit id: "rc-1"
    branch bugfix/JNG-4
    commit id: "fix-1"
    checkout release/1.0-beta1
    merge bugfix/JNG-4 id: "merge-fix"
    checkout develop
    merge release/1.0-beta1 id: "merge-release"
    checkout main
    merge release/1.0-beta1 id: "release-1.0"
```

## Version Numbering

Version numbers follow semantic versioning with these rules:

| Event | Version Change |
|-------|---------------|
| Start a `feature/` branch | No change — inherits from `develop` |
| Start a `release/` branch | 2nd number on `develop` is incremented |
| Start a `bugfix/` branch | No change — applied on release branches before merging to master |
| Start a `support/` branch | 3rd number is incremented |
| Start a `hotfix/` branch | 4th number is incremented |

## GitHub Actions Pipelines

The CI/CD system is composed of four interconnected workflows that automate building, tagging, merging, and releasing.

### build.yml — Main Build Pipeline

Triggers on pushes to `develop` and pull requests to `develop`, `master`, `increment/*`, `release/*`.

```mermaid
flowchart TD
    Start["Push / PR trigger"]
    BranchCheck{"Base branch?"}
    SetRelVer["Set version from pom.xml<br/>(without -SNAPSHOT)"]
    SetDevVer["Set version as<br/>major.minor.qualifier.date_commitId_branch"]
    Build["Build & deploy to Nexus"]
    Tag["Create git tag v&lt;version&gt;"]
    IsMergeable{"increment/* or release/*?"}
    MergeTag["Create tag merge-pr/&lt;version&gt;"]
    TriggerMerge["Trigger merge-pr-tagged.yml"]
    IsDevelop{"develop branch?"}
    Changelog["Build changelog"]
    Release["Create GitHub prerelease"]

    Start --> BranchCheck
    BranchCheck -->|master, release/*| SetRelVer
    BranchCheck -->|develop, increment/*| SetDevVer
    SetRelVer --> Build
    SetDevVer --> Build
    Build --> Tag
    Tag --> IsMergeable
    IsMergeable -->|Yes| MergeTag --> TriggerMerge
    IsMergeable -->|No| IsDevelop
    TriggerMerge --> IsDevelop
    IsDevelop -->|Yes| Changelog --> Release
    IsDevelop -->|No| End["Done"]
    Release --> End
```

### merge-pr-tagged.yml — PR Merge Automation

Triggers when a `merge-pr/*` tag is pushed.

```mermaid
flowchart TD
    Start["merge-pr/* tag pushed"]
    GetVer["Extract version from tag"]
    VerCheck{"Version format?"}
    MergeMaster["Merge PR to master"]
    TriggerRelease["Trigger create-release-on-master.yml"]
    SquashDev["Squash PR to develop"]
    TriggerBuild["Trigger build.yml"]
    Cleanup["Delete merge-pr/* tag"]

    Start --> GetVer --> VerCheck
    VerCheck -->|major.minor.qualifier| MergeMaster --> TriggerRelease --> Cleanup
    VerCheck -->|other| SquashDev --> TriggerBuild --> Cleanup
```

### create-release-on-master.yml — Release Creation

Triggers on pushes to `master`.

Extracts the version from the tag, builds a changelog, and creates a GitHub release (marked as latest).

### release.yml — Manual Release Trigger

Manually triggered with a version parameter (`auto` or `major.minor.qualifier`).

```mermaid
flowchart TD
    Start["Manual trigger with version"]
    AutoCheck{"Version = 'auto'?"}
    FromPom["Read version from pom.xml<br/>(strip -SNAPSHOT)"]
    UseGiven["Use given version"]
    CalcNext["Calculate next version<br/>(qualifier + 1)"]
    PRMaster["Create PR to master<br/>with release version"]
    PRDevelop["Create PR to develop<br/>with next version"]
    BuildA["Trigger build.yml"]
    BuildB["Trigger build.yml"]

    Start --> AutoCheck
    AutoCheck -->|Yes| FromPom --> CalcNext
    AutoCheck -->|No| UseGiven --> CalcNext
    CalcNext --> PRMaster --> BuildA
    CalcNext --> PRDevelop --> BuildB
```

## Development Rules

> **Important:** Every commit must reference a JIRA ticket number (`JNG-xxx`). There is no commit without a ticket number.

Issue tracking: [JIRA](https://blackbelt.atlassian.net/jira/dashboards)
