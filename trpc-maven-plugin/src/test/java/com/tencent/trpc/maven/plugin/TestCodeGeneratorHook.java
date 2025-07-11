/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent.
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.maven.plugin;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors;
import com.tencent.trpc.codegen.TRpcCodeGeneratorHook;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestCodeGeneratorHook implements TRpcCodeGeneratorHook {
    private final Path baseDirectory;

    public TestCodeGeneratorHook(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public List<Path> getAdditionalProtoDependencyPaths() {
        return Collections.singletonList(baseDirectory.resolve(Paths.get("src/main/imports")));
    }

    @Override
    public Map<String, Object> getCustomVariables(List<Descriptors.FileDescriptor> fileDescriptors) {
        return ImmutableMap.of("keyA", "foobar");
    }
}
