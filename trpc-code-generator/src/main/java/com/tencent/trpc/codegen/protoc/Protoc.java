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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wrapper of a protoc binary executable file.
 */
public class Protoc {
    private static final Logger log = LoggerFactory.getLogger(Protoc.class);
    private static final String PROTOC = "protoc";
    private final String protocExecutable;
    private final Map<String, ProtocPlugin> plugins;

    public Protoc() {
        this(PROTOC);
    }

    public Protoc(String protocExecutable) {
        this(protocExecutable, Collections.emptyMap());
    }

    public Protoc(String protocExecutable, Map<String, ProtocPlugin> plugins) {
        this.protocExecutable = protocExecutable;
        this.plugins = plugins;
    }

    /**
     * Read .proto files and generate descriptor set file.
     *
     * @param instruction protoc instruction
     * @return {@link ProtocExecutionResult}
     */
    public ProtocExecutionResult generateDescriptorSet(ProtocInstruction instruction) {
        return runProtoc(assembleProtocProcess(instruction.getSourceDirectory(),
                instruction.getSourceFiles(), instruction.getImportPaths(), instruction.getPluginInstructions(),
                "--descriptor_set_out=" + instruction.getOutput(), "--include_imports"));
    }

    /**
     * Read .proto files and generate protoc stub codes.
     *
     * @param instruction protoc instruction
     * @return {@link ProtocExecutionResult}
     */
    public ProtocExecutionResult generateStub(ProtocInstruction instruction) {
        return runProtoc(assembleProtocProcess(instruction.getSourceDirectory(), instruction.getSourceFiles(),
                instruction.getImportPaths(), instruction.getPluginInstructions(),
                String.format("--%s_out=%s",
                        instruction.getLanguage().toString().toLowerCase(), instruction.getOutput())));
    }

    private ProcessBuilder assembleProtocProcess(Path sourceDirectory,
                                                 Collection<String> sourceFiles,
                                                 List<Path> importPaths,
                                                 List<ProtocPluginInstruction> pluginInstructions,
                                                 String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(protocExecutable);
        cmd.add("--proto_path=.");
        if (importPaths != null) {
            cmd.addAll(importPaths.stream()
                    .map(p -> "--proto_path=" + p)
                    .collect(Collectors.toList()));
        }
        pluginInstructions.forEach(instruction -> {
            ProtocPlugin plugin = plugins.get(instruction.getPluginName());
            if (plugin == null) {
                log.warn("cannot find plugin {}, skipped", instruction.getPluginName());
                return;
            }
            cmd.addAll(plugin.buildProtocArguments(instruction.getPluginArguments(),
                    instruction.getPluginOutput().toString()));
        });
        cmd.addAll(Arrays.asList(args));
        cmd.addAll(sourceFiles);
        return new ProcessBuilder().command(cmd).directory(sourceDirectory.toFile());
    }

    private ProtocExecutionResult runProtoc(ProcessBuilder processBuilder) {
        log.debug("executing protoc cmd: {}", processBuilder.command());
        Process p;
        try {
            p = processBuilder.start();
        } catch (IOException e) {
            return ProtocExecutionResult.fail("execute protoc failed", null, e);
        }
        return fetchProcessResult(p);
    }

    private ProtocExecutionResult fetchProcessResult(Process p) {
        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();

        // Using threads to asynchronously read output and error streams.
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                log.error("failed to read output stream", e);
            }
        });

        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorBuilder.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                log.error("failed to read error stream", e);
            }
        });

        outputThread.start();
        errorThread.start();

        try {
            int code = p.waitFor();
            outputThread.join();
            errorThread.join();

            if (code == 0) {
                return ProtocExecutionResult.success(outputBuilder.toString());
            } else {
                return ProtocExecutionResult.fail("run protoc failed with exit-code " + code,
                        errorBuilder.toString(), null);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProtocExecutionResult.fail("execute protoc failed", null, e);
        }
    }

    @Override
    public String toString() {
        return "Protoc{protocExecutable='" + protocExecutable + "'}";
    }
}
