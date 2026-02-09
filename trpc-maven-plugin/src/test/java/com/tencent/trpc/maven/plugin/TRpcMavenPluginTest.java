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

package com.tencent.trpc.maven.plugin;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Plugin test is executed via maven-plugin-testing-harness, each case is an independent maven project.
 */
public class TRpcMavenPluginTest extends AbstractMojoTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Simulate 'mvn trpc:gen-code' on maven project at src/test/resources/TEST-1.
     * Only check the output files structure, since file content is checked by test cases of trpc-code-generator
     */
    @Test
    public void test1() throws Exception {
        executeTest("TEST-1");
        Path base = Paths.get("src", "test", "resources", "TEST-1", "target", "generated-sources",
                "trpc", "java").toAbsolutePath();
        Path output = base.resolve(Paths.get("com", "tencent", "test", "helloworld"));
        Assertions.assertTrue(Files.exists(output));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAsyncAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterSvr.java")));
        Assertions.assertTrue(Files.exists(base.resolve("pom.xml")));
    }

    /**
     * Simulate 'mvn trpc:gen-code' on maven project at src/test/resources/TEST-2.
     */
    @Test
    public void test2() throws Exception {
        executeTest("TEST-2");
        Path base = Paths.get("src", "test", "resources", "TEST-2", "gen").toAbsolutePath();
        Path output = base.resolve(Paths.get("com", "tencent", "test", "helloworld"));
        Assertions.assertTrue(Files.exists(output));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAsyncAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterSvr.java")));
        Assertions.assertFalse(Files.exists(base.resolve("pom.xml")));
    }

    /**
     * Simulate 'mvn trpc:gen-code' on maven project at src/test/resources/TEST-3.
     */
    @Test
    public void test3() throws Exception {
        executeTest("TEST-3");
        Path base = Paths.get("src", "test", "resources", "TEST-3", "target", "generated-sources",
                "trpc", "java").toAbsolutePath();
        Path output = base.resolve(Paths.get("com", "tencent", "test", "helloworld"));
        Assertions.assertTrue(Files.exists(output));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAsyncAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterSvr.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterTest.java")));
        Assertions.assertEquals("Greeter foobar", new String(Files.readAllBytes(output.resolve("GreeterTest.java"))));
        Assertions.assertTrue(Files.exists(base.resolve("pom.xml")));
    }

    /**
     * Simulate 'mvn trpc:gen-code' on maven project at src/test/resources/TEST-4.
     */
    @Test
    public void test4() throws Exception {
        executeTest("TEST-4");
        Path base = Paths.get("src", "test", "resources", "TEST-4", "target", "generated-sources",
                "trpc", "java").toAbsolutePath();
        Path output = base.resolve(Paths.get("com", "tencent", "test", "helloworld"));
        Assertions.assertTrue(Files.exists(output));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterAsyncAPI.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterSvr.java")));
        Assertions.assertTrue(Files.exists(output.resolve("GreeterSvrValidator.java")));
        Assertions.assertTrue(Files.exists(base.resolve("pom.xml")));
    }

    private void executeTest(String root) throws Exception {
        MavenProject project = readMavenProject(new File("src/test/resources/" + root));
        MavenSession session = newMavenSession(project);
        session.getRequest().setLocalRepository(createLocalArtifactRepository());
        lookup(LegacySupport.class).setSession(session);

        TRpcCodeGenerateMojo mojo = (TRpcCodeGenerateMojo) lookupConfiguredMojo(session, newMojoExecution("gen-code"));
        mojo.execute();
    }

    private MavenProject readMavenProject(File basedir) throws Exception {
        File pom = new File(basedir, "pom.xml");
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setLocalRepository(createLocalArtifactRepository());
        MavenProject project = lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        Assertions.assertNotNull(project);
        return project;
    }

    private ArtifactRepository createLocalArtifactRepository() {
        return new MavenArtifactRepository(
                "local",
                RepositorySystem.defaultUserLocalRepository.toURI().toString(),
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                        ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE),
                new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                        ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE)
        );
    }
}
