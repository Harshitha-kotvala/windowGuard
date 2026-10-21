# ADR-0001: Use Maven as the Build Tool

- **Status:** Accepted
- **Date:** 2026-10-21
- **Project:** WindowGuard

## Context

WindowGuard is a Java library for implementing per-client sliding-window rate limiting. It needs a consistent way to compile source code, manage dependencies, run automated tests, and package the library into a distributable JAR.

As the project grows, I also want a repeatable build process that works locally and in CI without manually managing dependencies or compilation steps.

## Options Considered

### 1. Plain `javac`

Compile Java files directly using the JDK.

- No additional build tool is required.
- Suitable for small programs with few or no external dependencies.
- Dependency management, test execution, and packaging would require more manual work as the project grows.

### 2. Maven

Use Maven with a `pom.xml` to define the project, dependencies, plugins, and build configuration.

- Uses a conventional project directory structure.
- Manages external dependencies and their versions.
- Integrates with JUnit for automated testing.
- Provides standard commands for compiling, testing, and packaging.
- Can be integrated into CI pipelines.

### 3. Gradle

Use Gradle with a build script.

- Supports dependency management, testing, and packaging.
- Offers flexible build logic and strong performance options.
- Introduces a different build configuration approach that is not necessary for the current scope.

## Decision

I chose **Apache Maven** as WindowGuard's build tool.

The project benefits from Maven's standard conventions, declarative dependency configuration, and lifecycle commands. These features let me focus on implementing and validating the rate-limiting algorithm rather than maintaining custom build scripts.

Maven is a good fit for the current size and requirements of the project. This decision does not mean Maven is universally better than Gradle or plain `javac`.

## Consequences

**Positive consequences**
- A predictable project structure.
- Centralized dependency and plugin configuration in `pom.xml`.
- Straightforward integration with JUnit.
- Standard commands for building and testing the project.
- Easier integration with CI and library packaging.

**Trade-offs**
- Requires Maven to be installed or a Maven Wrapper to be included.
- Adds build-tool configuration that a tiny Java program might not need.
- Dependency versions and plugins must be maintained.

## Verification

The decision will be validated by ensuring that the following commands work:

- `mvn compile` — compile production code.
- `mvn test` — execute automated tests.
- `mvn package` — package the library as a JAR.

## References

- Maven Getting Started: https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html
- Maven Standard Directory Layout: https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html
