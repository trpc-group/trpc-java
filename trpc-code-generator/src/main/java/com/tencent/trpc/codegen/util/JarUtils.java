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

package com.tencent.trpc.codegen.util;

import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Jar file related utils
 */
public class JarUtils {
    /**
     * Extract a directory from jar
     *
     * @param jar jar file
     * @param prefix the directory in jar to extract, ends with '/', e.g. "some/folder/"
     * @param outputPath parent directory of the extracted directory
     * @throws IOException if file I/O error
     */
    public static void extractJarFolder(JarFile jar, String prefix, Path outputPath) throws IOException {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(prefix)) {
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath.resolve(entry.getName()));
                } else {
                    extractFileFromJar(jar, entry, outputPath.resolve(entry.getName()));
                }
            }
        }
    }

    /**
     * Extract a file from jar
     *
     * @param jar jar file
     * @param entry file in jar to extract
     * @param outputPath parent directory of the extracted file
     * @throws IOException if file I/O error
     */
    public static void extractFileFromJar(JarFile jar, JarEntry entry, Path outputPath) throws IOException {
        if (entry.isDirectory()) {
            throw new IOException("JarEntry is a directory");
        }
        try (InputStream in = jar.getInputStream(entry)) {
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                IOUtils.copy(in, out);
            }
        }
    }
}
