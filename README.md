# Banka-1-Backend

## Getting Started

### Prerequisites

- **Java 21**
- **Node.js** (for OpenAPI validation) — install `redocly`:
  ```bash
  npm install -g @redocly/cli
  ```
- **.NET SDK** (required if any .NET microservices are present)
- **Docker Desktop** (includes Docker Engine, CLI, and Compose) — [download here](https://www.docker.com/products/docker-desktop/)

### Setup after cloning

Run the setup script once to activate Git hooks:

```bash
./setup-hooks.sh
```

This configures Git to use the hooks in `.github/githooks/`.

> **This step is required.** Without it, pre-push checks will not run.

---

## Pre-push Hooks

Every `git push` automatically runs three checks. The push is aborted if any check fails.

### 1. Unit and Integration Tests

Runs all tests across all services. Test commands are auto-detected by build system:

| Build system | Detection | Command |
|---|---|---|
| Gradle (root multi-project) | `gradlew` + `settings.gradle` | `./gradlew test` |
| Gradle (per-service) | `gradlew` | `./gradlew test` |
| Maven | `pom.xml` | `./mvnw verify` |
| Node.js | `package.json` | `npm ci && npm test` |
| Go | `go.mod` | `go test ./...` |
| Python | `requirements.txt` / `pyproject.toml` | `python -m pytest` |
| .NET | `*.sln` / `*.csproj` | `dotnet test` |

Unit and integration tests are both placed in `src/test/java` (for JVM services).

### 2. Documentation

Every microservice must have:

**`README.md`** containing all three of the following sections:
- Docker Compose usage instructions
- `.env` / environment variable documentation
- Endpoint and/or event examples

**OpenAPI spec** at one of these paths:
```
<service>/docs/openapi.yml
<service>/docs/openapi.yaml
<service>/src/main/resources/openapi.yml
<service>/src/main/resources/openapi.yaml
```

Requires `redocly` to be installed (see Prerequisites). The spec is validated with `redocly lint`.

**Skipping documentation checks** (e.g. for shared libraries that have no HTTP API, docker-compose, or environment config):

```bash
touch <service>/.skip-docs      # skips README section checks
touch <service>/.skip-openapi   # skips OpenAPI spec requirement and validation
```

### 3. Docker Build Validation

Runs `docker compose build --no-cache` from the repo root.

Requires:
- Docker installed and running
- `docker-compose.yml` present at the repo root

If either is missing, this step is skipped with a warning.

