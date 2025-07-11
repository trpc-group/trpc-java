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

package com.tencent.trpc.codegen.protoc;

import java.util.Arrays;
import java.util.List;

/**
 * Defines an execution of a protoc plugin.
 */
public class ProtocPlugin {
    public static final String GEN_VALIDATE_PLUGIN_NAME = "pvg";
    private final String name;
    private final String pluginExecutable;

    public ProtocPlugin(String name, String pluginExecutable) {
        this.name = name;
        this.pluginExecutable = pluginExecutable;
    }

    /**
     * Build the protoc arguments to execute the plugin
     *
     * @param pluginArguments plugin arguments
     * @param pluginOutput plugin output path
     * @return protoc arguments to invoke the plugin
     */
    public List<String> buildProtocArguments(List<String> pluginArguments, String pluginOutput) {
        StringBuilder pluginOutArgument = new StringBuilder("--")
                .append(name)
                .append("_out=");
        pluginArguments.forEach(arg -> pluginOutArgument.append(arg).append(":"));
        pluginOutArgument.append(pluginOutput);
        return Arrays.asList("--plugin=protoc-gen-" + name + "=" + pluginExecutable, pluginOutArgument.toString());
    }
}
