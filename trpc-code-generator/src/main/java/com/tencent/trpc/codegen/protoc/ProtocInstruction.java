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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Describe the instruction of a protoc execution
 */
public class ProtocInstruction {
    /**
     * Directory contains .proto files
     */
    private final Path sourceDirectory;
    /**
     * Name of the .proto files, relative to sourceDirectory
     */
    private final Collection<String> sourceFiles;
    /**
     * Directories of imported external .proto files
     */
    private final List<Path> importPaths;
    /**
     * Code language
     */
    private final Language language;
    /**
     * Path of the output descriptor set file
     */
    private final Path output;
    /**
     * Protoc plugin instructions
     */
    private final List<ProtocPluginInstruction> pluginInstructions;

    private ProtocInstruction(Path sourceDirectory,
                              Collection<String> sourceFiles,
                              List<Path> importPaths,
                              Language language,
                              Path output,
                              List<ProtocPluginInstruction> pluginInstructions) {
        this.sourceDirectory = sourceDirectory;
        this.sourceFiles = sourceFiles;
        this.importPaths = importPaths;
        this.language = language;
        this.output = output;
        this.pluginInstructions = pluginInstructions;
    }

    public static ProtocInstructionBuilder builder() {
        return new ProtocInstructionBuilder();
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public Collection<String> getSourceFiles() {
        return sourceFiles;
    }

    public List<Path> getImportPaths() {
        return importPaths;
    }

    public Language getLanguage() {
        return language;
    }

    public Path getOutput() {
        return output;
    }

    public List<ProtocPluginInstruction> getPluginInstructions() {
        return pluginInstructions;
    }

    public static final class ProtocInstructionBuilder {
        private Path sourceDirectory;
        private Collection<String> sourceFiles;
        private List<Path> importPaths;
        private Language language;
        private Path output;
        private final List<ProtocPluginInstruction> pluginInstructions = new ArrayList<>();

        private ProtocInstructionBuilder() {
        }

        /**
         * Set proto source Directory
         */
        public ProtocInstructionBuilder sourceDirectory(Path sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
            return this;
        }

        /**
         * Set proto source filenames, relative to sourceDirectory
         */
        public ProtocInstructionBuilder sourceFiles(Collection<String> sourceFiles) {
            this.sourceFiles = sourceFiles;
            return this;
        }

        /**
         * Set directories of imported external .proto files
         */
        public ProtocInstructionBuilder importPaths(List<Path> importPaths) {
            this.importPaths = importPaths;
            return this;
        }

        /**
         * Set code language
         */
        public ProtocInstructionBuilder language(Language language) {
            this.language = language;
            return this;
        }

        /**
         * Set code output path
         */
        public ProtocInstructionBuilder output(Path output) {
            this.output = output;
            return this;
        }

        /**
         * Set plugin instruction
         */
        public ProtocInstructionBuilder pluginInstruction(ProtocPluginInstruction pluginInstruction) {
            this.pluginInstructions.add(pluginInstruction);
            return this;
        }

        public ProtocInstruction build() {
            return new ProtocInstruction(sourceDirectory, sourceFiles, importPaths, language,
                    output, pluginInstructions);
        }
    }
}
