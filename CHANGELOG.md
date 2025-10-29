# Change Log

## v2.0.1-SNAPSHOT (Unreleased)

### Breaking Changes

- **deps**: Upgrade slf4j-api from 1.7.36 to 2.0.17 for Spring Boot 3.5.0 compatibility
- **deps**: Upgrade logback from 1.2.13 to 1.4.14 for SLF4J 2.0 support
- **deps**: Replace log4j-slf4j-impl with log4j-slf4j2-impl (2.24.3) for SLF4J 2.0 bridge

### Enhancements

- **core**: Implement SLF4J 2.0 new MDCAdapter interface methods (pushByKey, popByKey, clearDequeByKey, getCopyOfDequeByKey) in TrpcMDCAdapter
- **core**: Use reflection to initialize MDC adapter in SLF4J 2.0 compatible way
- **deps**: Ensure all slf4j dependencies are upgraded to 2.0.17 across all modules

### Compatibility

- **java**: Fully compatible with JDK 17
- **spring**: Fully compatible with Spring Boot 3.5.0
- **jakarta**: Aligned with Jakarta EE 10

## v1.1.0 (2023-12-20)

### Features

- workflow: add cla (#5)
- core: namingMap add default metadata map (#12)

### Enhancements

- configcenter test: remove useless test method of NacosConfigurationLoaderTest (#3)
- core test: StreamTests use available port (#2)

### Bug Fixes

- core: remove knock spi (#4)

## v1.0.0 (2023-12-05)

### Features

- all: first version