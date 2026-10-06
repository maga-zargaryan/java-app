# java-app

> Part of **[Java Platform](https://github.com/maga-zargaryan/java-platform)** · **java-app** (source) → [infra-bootstrap](https://github.com/maga-zargaryan/infra-bootstrap) → [platform-infra](https://github.com/maga-zargaryan/platform-infra) → [java-ami](https://github.com/maga-zargaryan/java-ami) → [java-infra](https://github.com/maga-zargaryan/java-infra)
>
> See [java-platform](https://github.com/maga-zargaryan/java-platform) for how the layers fit together.

The application the platform runs: a small Java 21 HTTP service (`GET /health` → `200 ok`).
It is the source of every release; this repository's pipeline builds, tests and publishes it,
and starts the image build.

## Release flow

```text
git tag v0.2.0  ──►  Release workflow (this repo)
                       build + test (mvn verify), sha256, build-info.json
                       upload to s3://<artifacts>/java-app/0.2.0/   (never overwrites)
                 ──►  PR in java-ami: app_version, app_commit, recipe_version+1
                       merge: app AMI built and tested, tagged AppGitCommit
                 ──►  PR in java-infra: dev ami_id = new AMI
                       merge: dev rolls out
                 ──►  PR in java-infra: prod ami_id = dev's ami_id
                       merge + approval: prod runs exactly what dev ran
```

Every step is a reviewed pull request; nothing follows a "latest" pointer.

## Releasing

1. Set `<version>` in `pom.xml` (semantic version) and merge.
2. Tag the merge commit: `git tag v<version> && git push origin v<version>`.

The workflow fails if the tag and `pom.xml` disagree, if tests fail, or if the version already exists.
Its summary lists the values for the java-ami pull request.

## Platform contract

| Item | Value |
|---|---|
| Artifact | `target/app.jar` (executable), published with `app.jar.sha256` and `build-info.json` |
| Runtime | Java 21 (Corretto), arm64, user `javaapp` |
| Port | `-Dserver.port` (from `SERVER_PORT`, 8080) |
| Health | `GET /health` → 200 |
| Settings | environment variables from `/etc/java-app/app.env` (DB host, secret ARN, data dir…) |

## Workflows

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | pull request, push to main | `mvn verify`, Trivy dependency scan; `ci` is the required check |
| `release.yml` | tag `v*.*.*` | build, test, publish via OIDC role (write-only to `java-app/*`) |

## Local

```bash
mvn verify
java -Dserver.port=8080 -jar target/app.jar
```
