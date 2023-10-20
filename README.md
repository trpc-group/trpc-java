English | [中文](README.zh_CN.md)

# tRPC-Java Framework

[![LICENSE](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://github.com/trpc-group/trpc-java/blob/main/LICENSE)
[![Releases](https://img.shields.io/github/release/trpc.group/trpc-java.svg?style=flat-square)](https://github.com/trpc-group/trpc-java/releases)
[![Docs](https://img.shields.io/badge/docs-latest-green)](https://github.com/trpc-group/trpc-java/tree/main/docs/)
[![Coverage](https://codecov.io/gh/trpc.group/trpc-java/branch/main/graph/badge.svg)](https://app.codecov.io/gh/trpc.group/trpc-java/tree/main)

tRPC-Java, as the Java language implementation of [tRPC](https://github.com/trpc-group/trpc), is a
battle-tested microservices framework that has been extensively validated in production
environments. It not only delivers high performance but also offers ease of use and testability.

For more information, please refer to the [related documentation](#related-documentation).

## Overall Architecture

![Architecture](.resources/overall.png)

tRPC-Java has the following features:

- Works across languages
- Support multi-protocols
- Streaming RPC
- Rich plugin ecosystem
- Scalability
- Load balancing
- Flow & Overload control
- Support coroutine

## Tutorial

### Dependency environment

JDK 8+, Maven 3.6.3+

### Import dependencies (non-coroutine version)

```pom
<dependencies>
    <dependency>
        <groupId>com.tencent.trpc</groupId>
        <artifactId>trpc-mini</artifactId>
        <version>0.15.0</version>
    </dependency>
</dependencies>
```

### Import dependencies (coroutine version)

It is recommended to use [Tencent Kona JDK 8](https://github.com/Tencent/TencentKona-8).

```pom
<dependencies>
    <dependency>
        <groupId>com.tencent.trpc</groupId>
        <artifactId>trpc-mini</artifactId>
        <version>0.15.0-FIBER</version>
    </dependency>
</dependencies>
```

<h2 id="2">Related Documentation</h2>

- [Quick start](/docs/en/1.quick_start.md)
- [Basic tutorial](/docs/en/2.basic_tutorial.md)
- [Stub code generation tool](/docs/en/3.protobuf_stub_plugin.md)
- [Configuration parameters](/docs/en/4.configuration.md)
- [More examples](https://github.com/trpc-group/trpc-java-examples)

## How to Contribute

If you're interested in contributing, please take a look at
the [contribution guidelines](CONTRIBUTING.md) and check
the [unassigned issues](https://github.com/trpc-group/trpc-java/issues) in the repository. Claim a
task and let's contribute together to tRPC-Java.

## LICENSE

tRPC-Java is licensed under the [Apache License Version 2.0](LICENSE).