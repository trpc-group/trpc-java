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

import com.google.protobuf.ApiOrBuilder;
import com.tencent.trpc.codegen.protoc.Protoc;
import com.tencent.trpc.codegen.protoc.ProtocTest;
import com.tencent.trpc.codegen.util.JarUtils;
import org.junit.Assert;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.jar.JarFile;

public class CodegenTestHelper {
    public static final Path TEST_ROOT = getTestClassesRoot(); // target/test-classes
    public static final Path IMPORT_ROOT = getImportRoot(); // target/classes/imports
    public static final String PROTOC_EXECUTABLE = getProtocExecutable();
    public static final String PROTOC_GEN_VALIDATE_EXECUTABLE = getProtocGenValidateExecutable();

    private static Path getTestClassesRoot() {
        URL url = ProtocTest.class.getClassLoader().getResource("TEST-1/hello.proto");
        Assert.assertNotNull(url);
        return Paths.get(url.getPath()).getParent().getParent();
    }

    private static Path getImportRoot() {
        URL url = Protoc.class.getClassLoader().getResource("imports/trpc.proto");
        Assert.assertNotNull(url);
        Path importRoot = Paths.get(url.getPath()).getParent();
        try {
            extractCommonProtoFiles(importRoot);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("failed to extract common proto files from protobuf-java.jar");
        }
        return importRoot;
    }

    private static String getProtocExecutable() {
        Iterator<Path> iterator;
        try {
            iterator = Files.newDirectoryStream(TEST_ROOT, "protoc-3*.exe").iterator();
        } catch (IOException e) {
            throw new RuntimeException("cannot find protoc executable");
        }
        Path protoc = iterator.next().toAbsolutePath().normalize();
        if (iterator.hasNext()) {
            throw new RuntimeException("multiple protoc executables found");
        }
        if (!protoc.toFile().setExecutable(true)) {
            throw new RuntimeException("set +x to " + protoc + " failed");
        }
        return protoc.toString();
    }

    private static void extractCommonProtoFiles(Path importRoot) throws URISyntaxException, IOException {
        JarFile jar = new JarFile(new File(
                ApiOrBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
        JarUtils.extractJarFolder(jar, "google/", importRoot);
    }

    private static String getProtocGenValidateExecutable() {
        Iterator<Path> iterator;
        try {
            iterator = Files.newDirectoryStream(TEST_ROOT, "protoc-gen-validate-*.exe").iterator();
        } catch (IOException e) {
            throw new RuntimeException("cannot find protoc-gen-validate executable");
        }
        Path protocGenValidate = iterator.next().toAbsolutePath().normalize();
        if (iterator.hasNext()) {
            throw new RuntimeException("multiple protoc-gen-validate executables found");
        }
        if (!protocGenValidate.toFile().setExecutable(true)) {
            throw new RuntimeException("set +x to " + protocGenValidate + " failed");
        }
        return protocGenValidate.toString();
    }
}
