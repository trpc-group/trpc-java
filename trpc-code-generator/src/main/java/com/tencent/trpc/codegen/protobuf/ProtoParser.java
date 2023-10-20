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

package com.tencent.trpc.codegen.protobuf;

import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.tencent.trpc.codegen.extension.TrpcExtensionProto;
import com.tencent.trpc.codegen.protobuf.source.model.ProtoMessageType;
import com.tencent.trpc.codegen.protobuf.source.model.ProtoMethod;
import com.tencent.trpc.codegen.protobuf.source.model.ProtoService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides abilities to parse .proto files
 */
public class ProtoParser {

    /**
     * parse a descriptor set file (.pb) and returns a {@link FileDescriptorSet} describes it
     *
     * @param descriptorSetFile path of the descriptor set file (.pb)
     * @return {@link FileDescriptorSet}
     * @throws IOException if error occurred reading the file
     */
    public static FileDescriptorSet parseDescriptorSetFile(Path descriptorSetFile) throws IOException {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        registry.add(TrpcExtensionProto.alias);
        try (InputStream in = Files.newInputStream(descriptorSetFile)) {
            return FileDescriptorSet.parseFrom(in, registry);
        }
    }

    /**
     * parse a list of {@link FileDescriptor}s and returns a {@link ProtoSourceInfo} describes it
     *
     * @param descriptors List of {@link FileDescriptor}
     * @return {@link ProtoSourceInfo}
     */
    public static ProtoSourceInfo parseFileDescriptors(List<FileDescriptor> descriptors) {
        Map<String, ProtoMessageType> messageTypeMap = new HashMap<>();
        List<ProtoService> services = new ArrayList<>();
        boolean usingValidator = false;
        for (FileDescriptor fd : descriptors) {
            extractMessageTypes(fd)
                    .forEach(protoMessageType ->
                            messageTypeMap.put(protoMessageType.getFullName(), protoMessageType));
            services.addAll(extractServices(fd, messageTypeMap));
            if ("validate.proto".equals(fd.getName())) {
                usingValidator = true;
            }
        }
        return new ProtoSourceInfo(services, usingValidator);
    }

    private static List<ProtoMessageType> extractMessageTypes(FileDescriptor fd) {
        return fd.getMessageTypes().stream()
                .map(descriptor -> ProtoMessageType.builder()
                        .name(descriptor.getName())
                        .packageName(fd.getPackage())
                        .javaPackage(fd.getOptions().getJavaPackage())
                        .javaOuterClass(fd.getOptions().getJavaOuterClassname())
                        .multipleClasses(fd.getOptions().getJavaMultipleFiles())
                        .fallbackClassname(getFallbackClassname(fd))
                        .build())
                .collect(Collectors.toList());
    }

    private static List<ProtoService> extractServices(FileDescriptor fd,
                                                      Map<String, ProtoMessageType> messageTypeMap) {
        return fd.getServices().stream()
                .map(service -> ProtoService.builder()
                        .name(service.getName())
                        .packageName(fd.getPackage())
                        .javaPackage(fd.getOptions().getJavaPackage())
                        .javaOuterClass(fd.getOptions().getJavaOuterClassname())
                        .multipleClasses(fd.getOptions().getJavaMultipleFiles())
                        .fallbackClassname(getFallbackClassname(fd))
                        .interfaceNamePrefix(toBigCamelCase(service.getName()))
                        .methods(extractMethods(service, messageTypeMap))
                        .build())
                .collect(Collectors.toList());
    }

    private static List<ProtoMethod> extractMethods(ServiceDescriptor service,
                                                    Map<String, ProtoMessageType> messageTypeMap) {
        return service.getMethods().stream()
                .map(method -> ProtoMethod.builder()
                        .name(method.getName())
                        .inputType(getProtoMessageType(method.getInputType().getFullName(), messageTypeMap))
                        .outputType(getProtoMessageType(method.getOutputType().getFullName(), messageTypeMap))
                        .clientStreaming(method.isClientStreaming())
                        .serverStreaming(method.isServerStreaming())
                        .alias(method.getOptions().getExtension(TrpcExtensionProto.alias))
                        .build())
                .collect(Collectors.toList());
    }

    private static ProtoMessageType getProtoMessageType(String fullName,
                                                        Map<String, ProtoMessageType> messageTypeMap) {
        return Optional.ofNullable(messageTypeMap.get(fullName))
                .orElseThrow(() -> new ProtobufDescriptorException("unknown messageType " + fullName));
    }

    private static String getFallbackClassname(FileDescriptor fd) {
        String[] arr = StringUtils.removeEnd(fd.getName(), ".proto").split("/");
        return toBigCamelCase(arr[arr.length - 1]);
    }

    private static String toBigCamelCase(String value) {
        if (value.contains("_")) {
            return CaseUtils.toCamelCase(value, true, '_');
        } else {
            return StringUtils.capitalize(value);
        }
    }
}
