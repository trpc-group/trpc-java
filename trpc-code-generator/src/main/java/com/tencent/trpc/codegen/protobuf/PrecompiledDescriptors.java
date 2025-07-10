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

package com.tencent.trpc.codegen.protobuf;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.tencent.trpc.codegen.extension.TrpcExtensionProto;
import java.util.Arrays;
import java.util.List;

/**
 * pre-compiled protobuf Descriptors
 */
public class PrecompiledDescriptors {
    private static final List<FileDescriptor> precompiledDescriptors =
            Arrays.asList(DescriptorProtos.getDescriptor(), TrpcExtensionProto.getDescriptor());

    /**
     * Get all pre-compiled protobuf Descriptors
     *
     * @return List of {@link FileDescriptor}
     */
    public static List<FileDescriptor> getAll() {
        return precompiledDescriptors;
    }
}
