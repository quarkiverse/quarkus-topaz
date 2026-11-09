# Quarkus Topaz

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.topaz/quarkus-topaz-parent?logo=apache-maven&style=flat-square)](https://central.sonatype.com/artifact/io.quarkiverse.topaz/quarkus-topaz-parent)
[![Build](https://github.com/quarkiverse/quarkus-topaz/actions/workflows/build.yml/badge.svg)](https://github.com/quarkiverse/quarkus-topaz/actions/workflows/build.yml)

Quarkus extension for [Topaz](https://github.com/aserto-dev/topaz), the aserto-dev/topaz authorizer. It provides a reactive gRPC client for the authorizer and the directory. It also adds a Dev Service, a readiness health check and a Dev UI card.

## Compatibility

| Quarkus      | Quarkus Topaz |
|--------------|---------------|
| 3.40.x (LTS) | 0.x           |

## Getting started

Add the dependency, replacing `LATEST_VERSION` with the version shown in the badge above:

```xml
<dependency>
    <groupId>io.quarkiverse.topaz</groupId>
    <artifactId>quarkus-topaz</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

Enforcement lives in a second artifact, `quarkus-topaz-security`. It provides:

- A `topaz` HTTP security policy for routes. It derives the policy path from the request, such as `todoApp.DELETE.todos.__id`.
- `@TopazAuthorize` and `@TopazCheck` interceptors for CDI bean methods.
- An opt-in `@PermissionsAllowed` bridge.

Every error denies. See [Security](https://docs.quarkiverse.io/quarkus-topaz/dev/security.html).

```xml
<dependency>
    <groupId>io.quarkiverse.topaz</groupId>
    <artifactId>quarkus-topaz-security</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

Quarkus-zanzibar users can add `quarkus-topaz-zanzibar`. It supplies a `RelationshipManager` backed by the Topaz directory. Topaz then answers `@FGARelation` and the other quarkus-zanzibar annotations. The artifact also enables the quarkus-zanzibar filter, which by default denies REST endpoints without an annotation. See [Zanzibar connector](https://docs.quarkiverse.io/quarkus-topaz/dev/zanzibar-connector.html).

```xml
<dependency>
    <groupId>io.quarkiverse.topaz</groupId>
    <artifactId>quarkus-topaz-zanzibar</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

Read the full [documentation](https://docs.quarkiverse.io/quarkus-topaz/dev/index.html).

## Building

```shell
JAVA_HOME=/path/to/jdk-21 mvn verify
```

The tests start Topaz in a container. With rootless Podman, add `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true`. See [Testing](https://docs.quarkiverse.io/quarkus-topaz/dev/testing.html).

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/jnbdz"><img src="https://avatars.githubusercontent.com/u/237008?v=4?s=100" width="100px;" alt="Jean-Nicolas Boulay Desjardins"/><br /><sub><b>Jean-Nicolas Boulay Desjardins</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-topaz/commits?author=jnbdz" title="Code">💻</a> <a href="#maintenance-jnbdz" title="Maintenance">🚧</a> <a href="https://github.com/quarkiverse/quarkus-topaz/commits?author=jnbdz" title="Documentation">📖</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
