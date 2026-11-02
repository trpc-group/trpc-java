# Spring Boot BOM compatibility regression

This consumer reproduces the dependency-management order from issue
[#152](https://github.com/trpc-group/trpc-java/issues/152): Spring Boot's BOM precedes
tRPC's BOM. It reuses the transport tests against the installed tRPC artifacts,
covering HTTP/1.1, HTTPS, h2c, h2 with ALPN, request bodies and asynchronous Servlet
responses. All server sockets bind explicitly to `127.0.0.1`.

Run from the repository root with JDK 17 or 21:

```sh
mvn -B -ntp -pl trpc-dependencies/trpc-dependencies-bom,trpc-dependencies/trpc-bom,trpc-transport/trpc-transport-http -am -Dmaven.test.skip=true install
mvn -B -ntp -f trpc-transport/trpc-transport-http/src/test/it/boot-bom-first/pom.xml test
```

To test the released implementation as a negative control, add
`-Dtrpc.version=2.0.5` to the second command. With Spring Boot 3.5.16 this mixes
Jetty 12 core artifacts with Jetty 11 Servlet/HTTP2 artifacts and fails during
server initialization. The fixed snapshot uses Jetty 12 core and EE10 BOMs,
EE10 Servlet coordinates, and the renamed HTTP/2 artifacts.

The JDK 17 and JDK 21 Maven install CI jobs run the compatibility matrix after
installing the reactor artifacts:

```sh
python3 trpc-transport/trpc-transport-http/src/test/it/boot-bom-first/run.py
```

The runner reads the current `springboot.version` from tRPC's dependency BOM and
Maven Central's release metadata. It selects every published stable patch of
that baseline's minor line and all later stable versions, plus the latest patch
of the two preceding minor lines within the same major. For the 3.5.0 baseline,
this covers every 3.5.x patch and
later stable releases, with 3.4.x and 3.3.x as backward compatibility samples.
Milestones, release candidates and snapshots are excluded. Boot 3.1 and Jetty 11
are outside this support range; their incompatibility is not a release blocker.
New stable versions are picked up on subsequent CI runs, rather than assumed
compatible in advance.

Jetty core and EE10 BOMs default to the 12.1 line. Boot 3.5's BOM manages both
core and EE10 artifacts, selecting its aligned 12.0.x versions. Boot 4 manages
EE11 instead of EE10, so tRPC must provide a 12.1-compatible EE10 default to
avoid mixing EE10 12.0 with a 12.1 core. The Servlet-based tRPC executor contract
is unchanged. These tests cover the HTTP transport under each BOM, not the full
Spring Boot starter, application auto-configuration or gateway application.

Each version runs in a separate Maven invocation. Results and dependency trees
are saved under the repository's `target/jetty-boot-compatibility/` directory.
The normal reactor `test` command does not invoke this fixture. Additional
arguments to `run.py` are passed to Maven, so isolated settings and repository
options can be reused. For a single version, use the standalone POM command
above with `-Dspring-boot.version=...`.
