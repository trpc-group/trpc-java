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

import com.google.protobuf.ApiOrBuilder;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.tencent.trpc.codegen.protobuf.FileDescriptorsCompiler;
import com.tencent.trpc.codegen.protobuf.ProtoParser;
import com.tencent.trpc.codegen.protobuf.ProtoSourceInfo;
import com.tencent.trpc.codegen.protoc.Language;
import com.tencent.trpc.codegen.protoc.Protoc;
import com.tencent.trpc.codegen.protoc.ProtocExecutionResult;
import com.tencent.trpc.codegen.protoc.ProtocInstruction;
import com.tencent.trpc.codegen.protoc.ProtocPlugin;
import com.tencent.trpc.codegen.protoc.ProtocPluginInstruction;
import com.tencent.trpc.codegen.template.CodeFileGenerator;
import com.tencent.trpc.codegen.util.JarUtils;
import io.envoyproxy.pgv.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * tRPC code generator. Responsible for generating protoc stub code and tRPC interfaces.
 */
public class TRpcCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(TRpcCodeGenerator.class);
    private static final String DEFAULT_DESCRIPTOR_FILENAME = "descriptor.pb";
    private static final String DEFAULT_TMP_OUTPUT_FOLDER = "generated";
    private static final String DEFAULT_TMP_IMPORT_FOLDER = "imports";
    /**
     * CodeFileGenerator for generating templated codes
     */
    private final CodeFileGenerator<?, ?> codeFileGenerator;
    /**
     * Transfer protocol (tRPC/gRPC)
     */
    private final Protocol protocol;
    /**
     * Code language
     */
    private final Language language;
    /**
     * Representing protoc executable file
     */
    private final Protoc protoc;
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
    /**
     * Temporary directory for storing generated code files (must be subfolder of tmpPath)
     */
    private final Path tmpOutPath;
    /**
     * A hook for customizing code generation
     */
    private final TRpcCodeGeneratorHook codeGeneratorHook;

    private TRpcCodeGenerator(CodeFileGenerator<?, ?> codeFileGenerator,
            Protocol protocol,
            Language language,
            Protoc protoc,
            CodeGeneratorPath codeGeneratorPath,
            TRpcCodeGeneratorHook codeGeneratorHook) {
        this.codeFileGenerator = codeFileGenerator;
        this.protocol = protocol;
        this.language = language;
        this.protoc = protoc;
        this.protoPath = codeGeneratorPath.getProtoPath();
        this.outPath = codeGeneratorPath.getOutPath();
        this.tmpPath = codeGeneratorPath.getTmpPath();
        this.codeGeneratorHook = codeGeneratorHook;
        this.tmpOutPath = tmpPath.resolve(DEFAULT_TMP_OUTPUT_FOLDER);
    }

    /**
     * Run the code generation process
     *
     * @throws TRpcCodeGenerateException if error occurred while generating code
     */
    public void generateCode() throws TRpcCodeGenerateException {
        log.info("generating code... language={}, protoPath={}, outPath={}, protoc={}",
                language, protoPath, outPath, protoc);
        try {
            Files.createDirectories(tmpPath);
            List<Path> importPaths = prepareImportPaths();
            Path descriptorFile = generateDescriptorFile(getProtoFiles(false), importPaths);
            Files.createDirectories(tmpOutPath);
            List<Descriptors.FileDescriptor> fdList = compileDescriptorSet(descriptorFile);
            Map<String, Object> customVariables = codeGeneratorHook.getCustomVariables(fdList);
            ProtoSourceInfo protoSourceInfo = ProtoParser.parseFileDescriptors(fdList);
            codeFileGenerator.generateCodeFiles(protocol, protoSourceInfo, tmpOutPath, customVariables);
            generateStub(getProtoFiles(false), importPaths, protoSourceInfo.isUsingValidator());
            assembleOutputs();
            log.info("code generate complete, exported to {}", outPath);
        } catch (IOException e) {
            throw new TRpcCodeGenerateException("IOException", e);
        } finally {
            FileUtils.deleteQuietly(tmpPath.toFile());
        }
    }

    /**
     * Prepare the directories contain global proto dependencies(imported .proto files).
     * By default, the returned directories will contain trpc.proto and google/protobuf/**.proto
     *
     * @return the imported directories
     */
    private List<Path> prepareImportPaths() throws IOException {
        List<Path> importPaths = new ArrayList<>();
        importPaths.add(prepareProtoDependencies());
        List<Path> additionalDependencies = codeGeneratorHook.getAdditionalProtoDependencyPaths();
        if (additionalDependencies != null) {
            importPaths.addAll(additionalDependencies);
        }
        return importPaths;
    }

    private Path prepareProtoDependencies() throws IOException {
        Path importPath = tmpPath.resolve(DEFAULT_TMP_IMPORT_FOLDER);
        Files.createDirectories(importPath);
        Path filePath = importPath.resolve("trpc.proto");
        try (InputStream trpcProto = getClass().getClassLoader().getResourceAsStream("imports/trpc.proto");
                OutputStream out = Files.newOutputStream(filePath)) {
            if (trpcProto == null) {
                // normally shouldn't happen
                throw new TRpcCodeGenerateException("cannot find 'imports/trpc.proto' in classpath");
            }
            IOUtils.copy(trpcProto, out);
            extractCommonProtoFiles(importPath);
        } catch (IOException | URISyntaxException e) {
            throw new TRpcCodeGenerateException("extract common .proto files failed", e);
        }
        return importPath;
    }

    private void extractCommonProtoFiles(Path outputPath) throws URISyntaxException, IOException {
        // extract google/protobuf/*.proto from protobuf-java.jar
        JarFile jar = new JarFile(new File(
                ApiOrBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
        JarUtils.extractJarFolder(jar, "google/", outputPath);

        // extract validate.proto from pgv-java-stub.jar
        jar = new JarFile(
                new File(Validator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
        JarUtils.extractFileFromJar(jar, new JarEntry("validate/validate.proto"),
                outputPath.resolve("validate.proto"));
    }

    /**
     * Get .proto filenames under protoPath.
     *
     * @param shallow true: only get .proto files directly under protoPath.
     *         false: traverse protoPath recursively to get all .proto files
     * @return a list of .proto filenames, including subfolder names
     */
    private List<String> getProtoFiles(boolean shallow) throws IOException {
        List<String> protoFiles = walkDirectoryForProtoFiles(protoPath, shallow).stream()
                .map(protoPath::relativize)
                .map(Path::toString)
                .collect(Collectors.toList());
        if (protoFiles.isEmpty()) {
            throw new TRpcCodeGenerateException("no .proto files found in " + protoPath);
        }
        return protoFiles;
    }

    private List<Path> walkDirectoryForProtoFiles(Path directory, boolean shallow) throws IOException {
        List<Path> protoFiles = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                if (!shallow && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    protoFiles.addAll(walkDirectoryForProtoFiles(path, false));
                } else if (path.getFileName().toString().endsWith(".proto") && Files.isRegularFile(path)) {
                    protoFiles.add(path);
                }
            }
        }
        return protoFiles;
    }

    /**
     * Generate protoc descriptor set file.
     *
     * @param protoFiles list of .proto filenames (relative to protoPath)
     * @param importPaths list of import directories
     * @return {@code Path} of the descriptor set file
     */
    private Path generateDescriptorFile(List<String> protoFiles, List<Path> importPaths) {
        log.info("generating descriptor set for {}", protoFiles);
        Path descriptorPath = tmpPath.resolve(DEFAULT_DESCRIPTOR_FILENAME);
        ProtocExecutionResult result = protoc.generateDescriptorSet(ProtocInstruction.builder()
                .sourceDirectory(protoPath)
                .sourceFiles(protoFiles)
                .importPaths(importPaths)
                .output(descriptorPath)
                .build());
        if (!result.isSuccess()) {
            throw new TRpcCodeGenerateException("generate descriptor set failed: " + result, result.getCause());
        }
        return descriptorPath;
    }

    private List<Descriptors.FileDescriptor> compileDescriptorSet(Path descriptorPath) throws IOException {
        DescriptorProtos.FileDescriptorSet set = ProtoParser.parseDescriptorSetFile(descriptorPath);
        FileDescriptorsCompiler compiler = new FileDescriptorsCompiler(set.getFileList());
        return compiler.compile();
    }

    /**
     * invoke protoc to generate stub codes. Generated code files are placed under tmpOutPath
     *
     * @param protoFiles list of .proto filenames (relative to protoPath)
     * @param importPaths list of import directories
     * @param withValidatePlugin if protoc-gen-validate plugin is required
     */
    private void generateStub(List<String> protoFiles,
            List<Path> importPaths,
            boolean withValidatePlugin) {
        log.info("generating protobuf stub code for {}", protoFiles);
        ProtocInstruction.ProtocInstructionBuilder instructionBuilder = ProtocInstruction.builder()
                .sourceDirectory(protoPath)
                .sourceFiles(protoFiles)
                .importPaths(importPaths)
                .language(language)
                .output(tmpOutPath);
        if (withValidatePlugin) {
            instructionBuilder.pluginInstruction(new ProtocPluginInstruction(ProtocPlugin.GEN_VALIDATE_PLUGIN_NAME,
                    Collections.singletonList("lang=" + language.toString().toLowerCase()), tmpOutPath));
        }
        ProtocExecutionResult result = protoc.generateStub(instructionBuilder.build());
        if (!result.isSuccess()) {
            throw new TRpcCodeGenerateException("generate stub failed: " + result, result.getCause());
        }
    }

    /**
     * assemble generated code files, copy them to outPath.
     */
    private void assembleOutputs() throws IOException {
        log.info("assembling outputs...");
        Files.createDirectories(outPath);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(tmpOutPath)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    FileUtils.copyDirectoryToDirectory(path.toFile(), outPath.toFile());
                } else {
                    FileUtils.copyFileToDirectory(path.toFile(), outPath.toFile());
                }
            }
        }
    }

    /**
     * Builder for {@link TRpcCodeGenerator}
     *
     * @return Builder for {@link TRpcCodeGenerator}
     */
    public static TRpcCodeGeneratorBuilder builder() {
        return new TRpcCodeGeneratorBuilder();
    }


    public static final class TRpcCodeGeneratorBuilder {

        private CodeFileGenerator<?, ?> codeFileGenerator;
        private Protocol protocol = Protocol.TRPC;
        private Language language = Language.JAVA;
        private Protoc protoc;
        private Path protoPath;
        private Path outPath;
        private Path tmpPath;
        private TRpcCodeGeneratorHook codeGeneratorHook;

        private TRpcCodeGeneratorBuilder() {
        }

        /**
         * Set {@link CodeFileGenerator} used for generating templated codes.
         */
        public TRpcCodeGeneratorBuilder codeFileGenerator(CodeFileGenerator<?, ?> codeFileGenerator) {
            this.codeFileGenerator = codeFileGenerator;
            return this;
        }

        /**
         * Set {@link Protocol}, default is TRPC.
         */
        public TRpcCodeGeneratorBuilder protocol(Protocol protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Set {@link Language}, default is JAVA.
         */
        public TRpcCodeGeneratorBuilder language(Language language) {
            this.language = language;
            return this;
        }

        /**
         * Set {@link Protoc} representing an executable protoc binary file.
         */
        public TRpcCodeGeneratorBuilder protoc(Protoc protoc) {
            this.protoc = protoc;
            return this;
        }

        /**
         * Set .proto files directory
         */
        public TRpcCodeGeneratorBuilder protoPath(Path protoPath) {
            this.protoPath = protoPath;
            return this;
        }

        /**
         * Set code output directory
         */
        public TRpcCodeGeneratorBuilder outPath(Path outPath) {
            this.outPath = outPath;
            return this;
        }

        /**
         * Set temporary working directory, a randomly named directory will be used if not specified
         */
        public TRpcCodeGeneratorBuilder tmpPath(Path tmpPath) {
            this.tmpPath = tmpPath;
            return this;
        }

        /**
         * Set the {@link TRpcCodeGeneratorHook} for customizing.
         * {@link DefaultCodeGeneratorHook} will be used if not specified.
         */
        public TRpcCodeGeneratorBuilder codeGeneratorHook(TRpcCodeGeneratorHook codeGeneratorHook) {
            this.codeGeneratorHook = codeGeneratorHook;
            return this;
        }

        /**
         * Build {@link TRpcCodeGenerator}
         */
        public TRpcCodeGenerator build() {
            if (codeFileGenerator == null) {
                throw new IllegalArgumentException("codeFileGenerator must not be null");
            }
            if (protoc == null) {
                throw new IllegalArgumentException("protoc must not be null");
            }
            if (protoPath == null) {
                throw new IllegalArgumentException("protoPath must not be null");
            }
            if (outPath == null) {
                throw new IllegalArgumentException("outPath must not be null");
            }
            if (tmpPath == null) {
                tmpPath = protoPath.getParent().resolve("tmp" + RandomStringUtils.randomAlphanumeric(6));
            }
            if (codeGeneratorHook == null) {
                codeGeneratorHook = new DefaultCodeGeneratorHook();
            }
            CodeGeneratorPath codeGeneratorPath = new CodeGeneratorPath(protoPath, outPath, tmpPath);
            return new TRpcCodeGenerator(codeFileGenerator, protocol, language, protoc, codeGeneratorPath,
                    codeGeneratorHook);
        }
    }
}
