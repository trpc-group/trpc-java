/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.codegen.protoc;

import com.tencent.trpc.codegen.CodegenTestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class ProtocTest {
    private static final Path importPath = CodegenTestHelper.IMPORT_ROOT;
    private static final Path rootPath = CodegenTestHelper.TEST_ROOT;
    private static final String protocExecutable = CodegenTestHelper.PROTOC_EXECUTABLE;
    private static final Path protoPath = rootPath.resolve("TEST-3");

    /**
     * test generate descriptor file via protoc
     */
    @Test
    public void testGenerateDescriptorSet() {
        Protoc protoc = new Protoc(protocExecutable);
        ProtocExecutionResult result = protoc.generateDescriptorSet(ProtocInstruction.builder()
                .sourceDirectory(protoPath)
                .sourceFiles(Collections.singletonList("hello.proto"))
                .importPaths(Collections.singletonList(importPath))
                .output(rootPath.resolve("hello.pb"))
                .build());
        Assertions.assertTrue(result.isSuccess());
    }

    /**
     * test generateDescriptorSet error
     */
    @Test
    public void testGenerateDescriptorSetError() {
        Protoc protoc = new Protoc("non-exist-cmd");
        ProtocExecutionResult result = protoc.generateDescriptorSet(ProtocInstruction.builder()
                .sourceDirectory(protoPath)
                .sourceFiles(Collections.singletonList("hello2.proto"))
                .importPaths(Collections.singletonList(importPath))
                .output(rootPath.resolve("hello2.pb"))
                .build());
        Assertions.assertFalse(result.isSuccess());
    }

    /**
     * test generateDescriptorSet failed
     */
    @Test
    public void testGenerateDescriptorSetFailed() {
        Protoc protoc = new Protoc(protocExecutable);
        ProtocExecutionResult result = protoc.generateDescriptorSet(ProtocInstruction.builder()
                .sourceDirectory(protoPath)
                .sourceFiles(Collections.singletonList("non-exist.proto"))
                .importPaths(Collections.singletonList(importPath))
                .output(rootPath.resolve("non-exist.pb"))
                .build());
        Assertions.assertFalse(result.isSuccess());
    }

    /**
     * test generate stub code via protoc
     */
    @Test
    public void testGenerateStub() throws IOException {
        Path outPath = rootPath.resolve("out");
        Files.createDirectories(outPath);
        Protoc protoc = new Protoc(protocExecutable);
        ProtocExecutionResult result = protoc.generateStub(ProtocInstruction.builder()
                .sourceDirectory(protoPath)
                .sourceFiles(Collections.singletonList("hello.proto"))
                .importPaths(Collections.singletonList(importPath))
                .language(Language.JAVA)
                .output(outPath)
                .build());
        Assertions.assertTrue(result.isSuccess());
    }
}
