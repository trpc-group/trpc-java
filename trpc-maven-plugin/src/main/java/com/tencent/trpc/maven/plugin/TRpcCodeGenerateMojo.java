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

package com.tencent.trpc.maven.plugin;

import com.google.common.collect.ImmutableMap;
import com.tencent.trpc.codegen.Protocol;
import com.tencent.trpc.codegen.TRpcCodeGenerator;
import com.tencent.trpc.codegen.TRpcCodeGeneratorHook;
import com.tencent.trpc.codegen.protoc.Language;
import com.tencent.trpc.codegen.protoc.Protoc;
import com.tencent.trpc.codegen.protoc.ProtocPlugin;
import com.tencent.trpc.codegen.template.CodeFileGenerator;
import com.tencent.trpc.codegen.template.CodeTemplate;
import com.tencent.trpc.codegen.template.DefaultCodeTemplates;
import com.tencent.trpc.codegen.template.FreeMarkerContextProvider;
import com.tencent.trpc.codegen.template.FreeMarkerStringTemplateEngine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maven plugin Mojo for generating tRPC stub & interface codes.
 */
@Mojo(name = "gen-code", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class TRpcCodeGenerateMojo extends AbstractMojo {
    /**
     * default values for protoc & plugin binary maven artifacts
     */
    private static final String PROTOC_GROUP_ID = "com.google.protobuf";
    private static final String PROTOC_ARTIFACT_ID = "protoc";
    private static final String PROTOC_ARTIFACT_VERSION = "3.21.9";
    private static final String PGV_PLUGIN_GROUP_ID = "io.envoyproxy.protoc-gen-validate";
    private static final String PGV_PLUGIN_ARTIFACT_ID = "protoc-gen-validate";
    private static final String PGV_PLUGIN_ARTIFACT_VERSION = "0.6.13";
    /**
     * default protocol
     */
    private static final String DEFAULT_PROTOCOL = "TRPC";

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ResolutionErrorHandler resolutionErrorHandler;

    /**
     * Maven project, injection only
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Maven session, injection only
     */
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession session;

    /**
     * Maven local repository, injection only
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
    private ArtifactRepository localRepository;

    /**
     * Maven remote repositories, injection only
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Protocol
     * @see Protocol
     */
    @Parameter(defaultValue = DEFAULT_PROTOCOL, required = true)
    private String protocol;

    /**
     * Base directory of maven project
     */
    @Parameter(defaultValue = "${basedir}/", required = true, readonly = true)
    private String basedir;

    /**
     * Directory that contains .proto files used to generate code
     */
    @Parameter(defaultValue = "${basedir}/src/main/proto", required = true)
    private String protoSourceRoot;

    /**
     * Directory to place output code files
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/trpc/java", required = true)
    private String outputDirectory;

    /**
     * Temporary working directory
     */
    @Parameter(defaultValue = "${project.build.directory}/trpc-maven-plugin-working", required = true, readonly = true)
    private String workingDirectory;

    /**
     * Local path of executable protoc binary. If set, will be used in precedence to protoc maven artifact
     */
    @Parameter
    private String protocExecutable;

    /**
     * Version of protoc maven artifact(com.google.protobuf:protoc). If set, will overwrite default value
     */
    @Parameter(defaultValue = PROTOC_ARTIFACT_VERSION)
    private String protocArtifactVersion;

    /**
     * Local path of executable protoc-gen-validate binary. If set, will be used in precedence to protoc maven artifact
     */
    @Parameter
    private String pgvPluginExecutable;

    /**
     * Version of protoc-gen-validate maven artifact(o.envoyproxy.protoc-gen-validate:protoc-gen-validate).
     * If set, will overwrite default value
     */
    @Parameter(defaultValue = PGV_PLUGIN_ARTIFACT_VERSION)
    private String pgvPluginArtifactVersion;

    /**
     * Operating system classifier to determine which binary of protoc maven artifact to use.
     * If set, will overwrite default value(which is automatically detect from current OS).
     */
    @Parameter
    private String osClassifier;

    /**
     * Will not generate pom.xml if set to true.
     */
    @Parameter
    private boolean noPom;

    /**
     * Additional custom code templates, for customizing plugin behavior.
     */
    @Parameter
    private List<CustomTemplate> customTemplates;

    /**
     * Full name of a class that implements {@link TRpcCodeGeneratorHook}, for customizing plugin behavior.
     * Specified class could have a public constructor accepts a single {@link Path} argument, otherwise it
     * must have a public, no-arg constructor.
     */
    @Parameter
    private String codeGeneratorHookClass;

    /**
     * Whether to add generated files to compile source root.
     */
    @Parameter
    private boolean attachGeneratedFiles;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        TRpcCodeGenerator codeGenerator = TRpcCodeGenerator.builder()
                .protocol(Protocol.valueOf(protocol))
                .language(Language.JAVA)
                .codeFileGenerator(CodeFileGenerator.<String, Map<String, Object>>builder()
                        .codeTemplateEngine(new FreeMarkerStringTemplateEngine())
                        .templateContextProvider(new FreeMarkerContextProvider())
                        .codeTemplates(prepareCodeTemplates())
                        .noPom(noPom)
                        .build())
                .protoc(prepareProtocBinaries())
                .protoPath(Paths.get(protoSourceRoot))
                .outPath(Paths.get(outputDirectory))
                .tmpPath(Paths.get(workingDirectory))
                .codeGeneratorHook(prepareHook())
                .build();
        try {
            codeGenerator.generateCode();

            if (attachGeneratedFiles) {
                project.addCompileSourceRoot(outputDirectory);
            }
        } catch (RuntimeException e) {
            throw new MojoFailureException("generate code failed", e);
        }
    }

    /**
     * Prepare default code templates and additional custom templates(if any).
     *
     * @return List contains all code templates required
     * @throws MojoExecutionException if loading custom template file failed
     */
    private List<CodeTemplate<String>> prepareCodeTemplates() throws MojoExecutionException {
        List<CodeTemplate<String>> templates = DefaultCodeTemplates.getInstance().getCodeTemplates();
        if (CollectionUtils.isEmpty(customTemplates)) {
            return templates;
        }
        templates = new ArrayList<>(templates);
        for (CustomTemplate template : customTemplates) {
            try {
                templates.add(template.toCodeTemplate());
            } catch (IOException e) {
                throw new MojoExecutionException("failed to prepare customTemplate: " + template, e);
            }
        }
        return templates;
    }

    /**
     * Prepare the {@link Protoc} instance used in current build,
     * which includes protoc binary and required plugin binaries.
     *
     * @return the {@link Protoc} instance
     * @throws MojoExecutionException if failed to prepare binary file
     * @throws MojoFailureException if failed to get artifact from maven
     */
    private Protoc prepareProtocBinaries() throws MojoExecutionException, MojoFailureException {
        String osName = OperatingSystemDetector.detectOsName();
        String classifier = osClassifier;
        if (StringUtils.isEmpty(classifier)) {
            String osArch = OperatingSystemDetector.detectOsArch();
            classifier = osName + "-" + osArch;
        }
        Path protocBinaryPath;
        Path pgvPluginBinaryPath;
        if (StringUtils.isEmpty(protocExecutable)) {
            protocBinaryPath = getBinaryDependencyFromMaven(PROTOC_GROUP_ID, PROTOC_ARTIFACT_ID,
                    protocArtifactVersion, osName, classifier);
        } else {
            protocBinaryPath = Paths.get(protocExecutable);
        }
        if (StringUtils.isEmpty(pgvPluginExecutable)) {
            pgvPluginBinaryPath = getBinaryDependencyFromMaven(PGV_PLUGIN_GROUP_ID, PGV_PLUGIN_ARTIFACT_ID,
                    pgvPluginArtifactVersion, osName, classifier);
        } else {
            pgvPluginBinaryPath = Paths.get(pgvPluginExecutable);
        }
        return new Protoc(protocBinaryPath.toString(), ImmutableMap.of(ProtocPlugin.GEN_VALIDATE_PLUGIN_NAME,
                new ProtocPlugin(ProtocPlugin.GEN_VALIDATE_PLUGIN_NAME, pgvPluginBinaryPath.toString())));
    }

    /**
     * Download required binary from maven, extract to working directory.
     */
    private Path getBinaryDependencyFromMaven(String groupId,
                                              String artifactId,
                                              String version,
                                              String osName,
                                              String classifier) throws MojoExecutionException, MojoFailureException {
        if (classifier.contains(OperatingSystemDetector.UNSUPPORTED)) {
            throw new MojoFailureException("There is no " + artifactId + " binary match current OS in maven, you can:"
                    + "\n1. Specify a local executable path"
                    + "\n2. Specify another osClassifier(e.g. 'linux-x86_64') via <osClassifier>, but you need "
                    + "to make sure that the binary of chosen classifier can run on current OS.");
        }
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setType("exe");
        dependency.setClassifier(classifier);
        dependency.setScope("runtime");

        getLog().info("resolving artifact: " + dependency);
        Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(project.getArtifact())
                .setResolveRoot(false)
                .setResolveTransitively(false)
                .setArtifactDependencies(Collections.singleton(artifact))
                .setManagedVersionMap(Collections.emptyMap())
                .setLocalRepository(localRepository)
                .setRemoteRepositories(remoteRepositories)
                .setOffline(session.isOffline())
                .setForceUpdate(session.getRequest().isUpdateSnapshots())
                .setServers(session.getRequest().getServers())
                .setMirrors(session.getRequest().getMirrors())
                .setProxies(session.getRequest().getProxies());
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        try {
            resolutionErrorHandler.throwErrors(request, result);
        } catch (ArtifactResolutionException e) {
            throw new MojoFailureException("failed to resolve artifact: ", e);
        }
        if (CollectionUtils.isEmpty(result.getArtifacts())) {
            throw new MojoFailureException("failed to resolve artifact " + dependency);
        }
        Artifact resolvedArtifact = result.getArtifacts().iterator().next();

        Path target = Paths.get(workingDirectory).resolve(artifactId);
        if (OperatingSystemDetector.WINDOWS.equals(osName)) {
            // adding .exe suffix to executable file on Windows
            target = target.resolveSibling(target.getFileName() + ".exe");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(resolvedArtifact.getFile().toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MojoExecutionException("copy binary to " + target + " failed: ", e);
        }
        if (!OperatingSystemDetector.WINDOWS.equals(osName) && !target.toFile().setExecutable(true)) {
            throw new MojoExecutionException("failed setting +x to " + target);
        }
        return target;
    }

    /**
     * Prepare the {@link TRpcCodeGeneratorHook} used for customizing plugin behavior.
     *
     * @return instance of {@link TRpcCodeGeneratorHook}, or null if no hook is set
     * @throws MojoExecutionException if failed to instantiate the hook class
     */
    private TRpcCodeGeneratorHook prepareHook() throws MojoExecutionException {
        if (StringUtils.isEmpty(codeGeneratorHookClass)) {
            return null;
        }
        try {
            Constructor<?> noArgConstructor = null;
            Constructor<?> pathArgConstructor = null;
            Constructor<?>[] constructors = Class.forName(codeGeneratorHookClass).getConstructors();
            for (Constructor<?> constructor : constructors) {
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    continue;
                }
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 0) {
                    noArgConstructor = constructor;
                } else if (parameters.length == 1 && parameters[0] == Path.class) {
                    pathArgConstructor = constructor;
                }
            }
            if (pathArgConstructor != null) {
                return (TRpcCodeGeneratorHook) pathArgConstructor.newInstance(Paths.get(basedir));
            } else if (noArgConstructor != null) {
                return (TRpcCodeGeneratorHook) noArgConstructor.newInstance();
            } else {
                throw new MojoExecutionException("cannot find required constructor for " + codeGeneratorHookClass);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MojoExecutionException("failed to instantiate generatorHook " + codeGeneratorHookClass, e);
        }
    }
}
