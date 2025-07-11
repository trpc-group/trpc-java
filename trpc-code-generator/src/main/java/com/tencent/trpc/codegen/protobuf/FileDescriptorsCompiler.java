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

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compile {@link FileDescriptorProto}s to {@link FileDescriptor}s:
 * <p>Find dependency {@code FileDescriptorProto}s, compile them first, then the dependant.</p>
 * <p>Perform above actions recursively until all {@code FileDescriptorProto}s are compiled.</p>
 * <p>The provided {@link FileDescriptorProto}s should be self-contained, that is,
 * it should contains all dependency protos</p>
 */
public class FileDescriptorsCompiler {
    /**
     * .proto files, filename -> FileDescriptorProto
     */
    private final Map<String, FileDescriptorProto> protoMap;
    /**
     * compiled .proto files, filename -> FileDescriptor
     */
    private final Map<String, FileDescriptor> compiledProtoMap;

    public FileDescriptorsCompiler(List<FileDescriptorProto> protos) {
        compiledProtoMap = new LinkedHashMap<>();
        // 预置已经预编译好的公共依赖
        PrecompiledDescriptors.getAll().forEach(descriptor -> compiledProtoMap.put(descriptor.getName(), descriptor));
        protoMap = new LinkedHashMap<>();
        protos.forEach(proto -> protoMap.put(proto.getName(), proto));
    }

    /**
     * Compile to {@link FileDescriptor}s
     *
     * @return List of {@link FileDescriptor}
     */
    public List<FileDescriptor> compile() {
        return protoMap.values().stream()
                .map(FileDescriptorProto::getName)
                .map(this::compileProto)
                .collect(Collectors.toList());
    }

    private FileDescriptor compileProto(String name) throws ProtobufDescriptorException {
        FileDescriptor fd = compiledProtoMap.get(name);
        if (fd != null) {
            return fd;
        }
        FileDescriptorProto proto = protoMap.get(name);
        if (proto == null) {
            throw new ProtobufDescriptorException("proto '" + name + "' not found");
        }
        try {
            return FileDescriptor.buildFrom(proto, getDependencies(proto), false);
        } catch (DescriptorValidationException e) {
            throw new ProtobufDescriptorException("compile proto '" + name + "' failed", e);
        }
    }

    private FileDescriptor[] getDependencies(FileDescriptorProto proto) {
        return proto.getDependencyList().stream().map(this::compileProto).toArray(FileDescriptor[]::new);
    }
}
