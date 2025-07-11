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

import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Util class for detecting OS related info
 */
public class OperatingSystemDetector {
    public static final String UNSUPPORTED = "unsupported";
    public static final String LINUX = "linux";
    public static final String OSX = "osx";
    public static final String WINDOWS = "windows";
    public static final String X86_32 = "x86_32";
    public static final String X86_64 = "x86_64";
    public static final String AARCH_64 = "aarch_64";
    public static final String PPCLE_64 = "ppcle_64";
    public static final String S390_64 = "s390_64";
    private static final Map<String, String> osNamePrefixToNormalizedName;
    private static final Map<String, String> osArchToNormalizedArch;

    static {
        osNamePrefixToNormalizedName = new HashMap<>();
        osNamePrefixToNormalizedName.put("linux", LINUX);
        osNamePrefixToNormalizedName.put("mac", OSX);
        osNamePrefixToNormalizedName.put("osx", OSX);
        osNamePrefixToNormalizedName.put("windows", WINDOWS);

        osArchToNormalizedArch = new HashMap<>();
        osArchToNormalizedArch.put("x8632", X86_32);
        osArchToNormalizedArch.put("x86", X86_32);
        osArchToNormalizedArch.put("x32", X86_32);
        osArchToNormalizedArch.put("ia32", X86_32);
        osArchToNormalizedArch.put("i386", X86_32);
        osArchToNormalizedArch.put("i486", X86_32);
        osArchToNormalizedArch.put("i586", X86_32);
        osArchToNormalizedArch.put("i686", X86_32);
        osArchToNormalizedArch.put("x8664", X86_64);
        osArchToNormalizedArch.put("x64", X86_64);
        osArchToNormalizedArch.put("amd64", X86_64);
        osArchToNormalizedArch.put("ia32e", X86_64);
        osArchToNormalizedArch.put("em64t", X86_64);
        osArchToNormalizedArch.put("aarch64", AARCH_64);
        osArchToNormalizedArch.put("ppc64le", PPCLE_64);
        osArchToNormalizedArch.put("s390x", S390_64);
    }

    /**
     * Detect current OS name series.
     *
     * @return normalized name of current OS
     */
    public static String detectOsName() {
        return normalizeOsName(System.getProperty("os.name"));
    }

    /**
     * Detect current OS architect series.
     *
     * @return normalized architect name of current OS
     */
    public static String detectOsArch() {
        return normalizeOsArch(System.getProperty("os.arch"));
    }

    private static String normalizeOsName(String osName) {
        osName = normalize(osName);
        for (Map.Entry<String, String> e : osNamePrefixToNormalizedName.entrySet()) {
            if (osName.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return UNSUPPORTED;
    }

    private static String normalizeOsArch(String osArch) {
        return osArchToNormalizedArch.getOrDefault(normalize(osArch), UNSUPPORTED);
    }

    private static String normalize(String value) {
        return StringUtils.defaultString(value).toLowerCase().replaceAll("[^a-z0-9]+", "");
    }
}
