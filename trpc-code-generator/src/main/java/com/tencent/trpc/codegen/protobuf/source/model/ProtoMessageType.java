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

package com.tencent.trpc.codegen.protobuf.source.model;

/**
 * Describe a messages defined in .proto files
 */
public class ProtoMessageType {
    /**
     * messageType name
     */
    private final String name;
    /**
     * proto package name
     */
    private final String packageName;
    /**
     * fullName (packageName.name)
     */
    private final String fullName;
    /**
     * option java_package
     */
    private final String javaPackage;
    /**
     * option java_outer_classname
     */
    private final String javaOuterClass;
    /**
     * option java_outer_classname
     */
    private final boolean multipleClasses;
    /**
     * the fallback classname when multipleClasses is false and javaOuterClass is not set
     */
    private final String fallbackClassname;

    private ProtoMessageType(String name,
                             String packageName,
                             String javaPackage,
                             String javaOuterClass,
                             boolean multipleClasses,
                             String fallbackClassname) {
        this.name = name;
        this.packageName = packageName;
        this.fullName = packageName + "." + name;
        this.javaPackage = javaPackage;
        this.javaOuterClass = javaOuterClass;
        this.multipleClasses = multipleClasses;
        this.fallbackClassname = fallbackClassname;
    }

    public static ProtoMessageTypeBuilder builder() {
        return new ProtoMessageTypeBuilder();
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public String getJavaOuterClass() {
        return javaOuterClass;
    }

    public boolean isMultipleClasses() {
        return multipleClasses;
    }

    public String getFallbackClassname() {
        return fallbackClassname;
    }

    @Override
    public String toString() {
        return "ProtoMessageType{"
                + "name='" + name + '\''
                + ", packageName='" + packageName + '\''
                + ", fullName='" + fullName + '\''
                + ", javaPackage='" + javaPackage + '\''
                + ", javaOuterClass='" + javaOuterClass + '\''
                + ", multipleClasses=" + multipleClasses
                + ", fallbackClassname='" + fallbackClassname + '\''
                + '}';
    }

    public static final class ProtoMessageTypeBuilder {
        private String name;
        private String packageName;
        private String javaPackage;
        private String javaOuterClass;
        private boolean multipleClasses;
        private String fallbackClassname;

        private ProtoMessageTypeBuilder() {
        }

        public ProtoMessageTypeBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProtoMessageTypeBuilder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public ProtoMessageTypeBuilder javaPackage(String javaPackage) {
            this.javaPackage = javaPackage;
            return this;
        }

        public ProtoMessageTypeBuilder javaOuterClass(String javaOuterClass) {
            this.javaOuterClass = javaOuterClass;
            return this;
        }

        public ProtoMessageTypeBuilder multipleClasses(boolean multipleClasses) {
            this.multipleClasses = multipleClasses;
            return this;
        }

        public ProtoMessageTypeBuilder fallbackClassname(String fallbackClassname) {
            this.fallbackClassname = fallbackClassname;
            return this;
        }

        public ProtoMessageType build() {
            return new ProtoMessageType(name, packageName, javaPackage, javaOuterClass, multipleClasses,
                    fallbackClassname);
        }
    }
}
