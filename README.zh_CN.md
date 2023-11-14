[English](README.md) | 中文

# tRPC-Java Framework

[![LICENSE](https://img.shields.io/badge/license-Apache--2.0-green.svg)](https://github.com/trpc-group/trpc-java/blob/master/LICENSE)
[![Releases](https://img.shields.io/github/release/trpc.group/trpc-java.svg?style=flat-square)](https://github.com/trpc-group/trpc-java/releases)
[![Docs](https://img.shields.io/badge/docs-latest-green)](https://github.com/trpc-group/trpc-java/tree/master/docs/)
[![Coverage](https://codecov.io/gh/trpc-group/trpc-java/graph/badge.svg?token=PCzWvpNAIk)](https://codecov.io/gh/trpc-group/trpc-java)

tRPC-Java，作为 [tRPC](https://github.com/trpc-group/trpc) 的 Java
语言版本，是经过大规模线上业务使用验证过的微服务框架，它不仅性能高，而且易于使用和测试。

更多信息见：[相关文档](#相关文档)

## 整体架构

![架构图](.resources/overall.png)

tRPC-Java 具有以下特点：

- 跨语言
- 多通信协议
- 流式rpc
- 丰富插件生态
- 可扩展性
- 负载均衡
- 流控和过载保护
- 支持协程

## 教程

### 依赖环境

JDK 8+, Maven 3.6.3+

### 引入依赖

```pom
<dependencies>
    <dependency>
        <groupId>com.tencent.trpc</groupId>
        <artifactId>trpc-mini</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### 使用协程

推荐使用 [Tencent Kona JDK FIBER 8](https://github.com/Tencent/TencentKona-8)
使用示例见[coroutine](https://github.com/trpc-group/trpc-java-examples/tree/master/trpc-coroutine)

<h2 id="2">相关文档</h2>

- [快速上手](/docs/zh/1.quick_start.md)
- [基础教程](/docs/zh/2.basic_tutorial.md)
- [tRPC-Java桩代码生成工具](/docs/zh/3.protobuf_stub_plugin.md)
- [配置参数文档](/docs/zh/4.configuration.md)
- [各种特性的示例代码](https://github.com/trpc-group/trpc-java-examples)

## 如何贡献

如果您有兴趣进行贡献，请查阅[贡献指南](CONTRIBUTING.zh_CN.md)
并检查 [issues](https://github.com/trpc-group/trpc-java/issues)
中未分配的问题。认领一个任务，让我们一起为 tRPC-Java 做出贡献。

## LICENSE

tRPC-Java 使用了 [Apache 2.0](LICENSE) 许可证.