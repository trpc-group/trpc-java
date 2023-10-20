<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>trpc-stub-example</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.tencent.trpc</groupId>
            <artifactId>trpc-core</artifactId>
            <version>${version[1..]}</version>
        </dependency>
<#if protocol == "GRPC">
        <dependency>
            <groupId>com.tencent.trpc</groupId>
            <artifactId>trpc-proto-grpc</artifactId>
            <version>${version[1..]}</version>
        </dependency>
</#if>
<#if sourceInfo.usingValidator>
        <dependency>
            <groupId>com.tencent.trpc</groupId>
            <artifactId>trpc-validation-pgv</artifactId>
            <version>${version[1..]}</version>
        </dependency>
        <dependency>
            <groupId>io.envoyproxy.protoc-gen-validate</groupId>
            <artifactId>pgv-java-stub</artifactId>
            <version>0.4.1</version>
        </dependency>
</#if>
    </dependencies>
</project>