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

import java.nio.file.Path;
import java.util.List;

/**
 * Describe instruction of a protoc plugin execution
 */
public class ProtocPluginInstruction {
    private final String pluginName;
    private final List<String> pluginArguments;
    private final Path pluginOutput;

    public ProtocPluginInstruction(String pluginName, List<String> pluginArguments, Path pluginOutput) {
        this.pluginName = pluginName;
        this.pluginArguments = pluginArguments;
        this.pluginOutput = pluginOutput;
    }

    public String getPluginName() {
        return pluginName;
    }

    public List<String> getPluginArguments() {
        return pluginArguments;
    }

    public Path getPluginOutput() {
        return pluginOutput;
    }
}
