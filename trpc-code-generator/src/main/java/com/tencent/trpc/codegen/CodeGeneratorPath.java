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

import java.nio.file.Path;

/**
 * tRPC code generator path. Responsible for recording generated code files path.
 */
public class CodeGeneratorPath {

    /**
     * Directory of where the .proto files in
     */
    private final Path protoPath;

    /**
     * Directory of generated code files
     */
    private final Path outPath;

    /**
     * Temporary directory used during code generation
     */
    private final Path tmpPath;

    public CodeGeneratorPath(Path protoPath, Path outPath, Path tmpPath) {
        this.protoPath = protoPath;
        this.outPath = outPath;
        this.tmpPath = tmpPath;
    }

    public Path getProtoPath() {
        return protoPath;
    }

    public Path getOutPath() {
        return outPath;
    }

    public Path getTmpPath() {
        return tmpPath;
    }
}
