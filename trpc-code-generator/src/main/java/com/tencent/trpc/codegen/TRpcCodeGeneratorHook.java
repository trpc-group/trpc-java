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

package com.tencent.trpc.codegen;

import com.google.protobuf.Descriptors;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Providing additional .proto dependency paths, and custom template context variables.
 * Useful for customizing tRPC code generation.
 *
 * <p>Note: When used in trpc-maven-plugin, implementation of this interface can have a constructor method
 * with a single {@link Path} argument. Which can set the basedir of the maven project, to help the
 * implementation to return addition .proto dependency paths without knowing the absolute path: </p>
 * <pre>{@code
 * public class TRpcCodeGeneratorHookImpl {
 *     private final Path basedir;
 *
 *     public TRpcCodeGeneratorHookImpl(Path basedir) {
 *         this.basedir = basedir
 *     }
 *
 *     List<Path> getAdditionalProtoDependencyPaths() {
 *         return Collections.singletonList(basedir.resolve("templates"));
 *     }
 * }
 * }</pre>
 */
public interface TRpcCodeGeneratorHook {
    /**
     * Provides extra directories as .proto external dependencies.
     * <p>By default, two external dependency directories will be set, one contains trpc.proto,\
     * the other contains google/protobuf/*.proto</p>
     * <p>This method is useful when there are more external .proto files need to be imported.</p>
     * <p>Note: please note that .proto files imported as external dependencies will not generate stub codes.
     * If you need to generate stub codes of the imported .proto files, please place them under protoPath. </p>
     *
     * @return absolute paths of the extra import directories
     */
    List<Path> getAdditionalProtoDependencyPaths();

    /**
     * Provides custom variables of template context.
     * Variables returned by this method will be placed under the 'custom' key of the template context.
     *
     * @param fileDescriptors List of {@link Descriptors.FileDescriptor}s containing all information of
     *                        included .proto files
     * @return Variables (key-value pair) to add to the template context
     */
    Map<String, Object> getCustomVariables(List<Descriptors.FileDescriptor> fileDescriptors);
}
