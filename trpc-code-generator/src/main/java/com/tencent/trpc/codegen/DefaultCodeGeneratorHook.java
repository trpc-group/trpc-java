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

package com.tencent.trpc.codegen;

import com.google.protobuf.Descriptors;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link TRpcCodeGeneratorHook} that does nothing.
 */
public class DefaultCodeGeneratorHook implements TRpcCodeGeneratorHook {

    @Override
    public List<Path> getAdditionalProtoDependencyPaths() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getCustomVariables(List<Descriptors.FileDescriptor> fileDescriptors) {
        return Collections.emptyMap();
    }
}
