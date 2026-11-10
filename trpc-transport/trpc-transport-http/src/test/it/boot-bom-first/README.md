# Spring Boot BOM compatibility regression

This consumer reproduces the dependency-management order from issue
[#152](https://github.com/trpc-group/trpc-java/issues/152): Spring Boot 3.5.16's BOM precedes
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

The JDK 17 and JDK 21 Maven install jobs run this consumer regression with Spring Boot
3.5.16 after installing the tRPC artifacts. Both BOMs manage Jetty 12.0.36.
