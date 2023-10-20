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

package com.tencent.trpc.codegen.protobuf.source.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describe a service defined in .proto files
 */
public class ProtoService {
    /**
     * service name
     */
    private final String name;
    /**
     * proto package name
     */
    private final String packageName;
    /**
     * option java_package
     */
    private final String javaPackage;
    /**
     * option java_outer_classname
     */
    private final String javaOuterClass;
    /**
     * option java_multiple_classes
     */
    private final boolean multipleClasses;
    /**
     * the fallback classname when multipleClasses is false and javaOuterClass is not set
     */
    private final String fallbackClassname;
    /**
     * the prefix of the java interface name related to this proto service
     */
    private final String interfaceNamePrefix;
    /**
     * methods in service
     */
    private final List<ProtoMethod> methods;
    /**
     * all messageTypes used by this service. fullName -> ProtoMessageType
     */
    private final Map<String, ProtoMessageType> messageTypes;

    private ProtoService(ProtoServiceBuilder builder) {
        this.name = builder.name;
        this.packageName = builder.packageName;
        this.javaOuterClass = builder.javaOuterClass;
        this.multipleClasses = builder.multipleClasses;
        this.fallbackClassname = builder.fallbackClassname;
        this.javaPackage = builder.javaPackage;
        this.interfaceNamePrefix = builder.interfaceNamePrefix;
        this.methods = builder.methods;
        this.messageTypes = extractMessageTypeDependency(methods);
    }

    private Map<String, ProtoMessageType> extractMessageTypeDependency(List<ProtoMethod> methods) {
        Map<String, ProtoMessageType> messageTypes = new LinkedHashMap<>();
        methods.forEach(method -> {
            messageTypes.putIfAbsent(method.getInputType().getFullName(), method.getInputType());
            messageTypes.putIfAbsent(method.getOutputType().getFullName(), method.getOutputType());
        });
        return messageTypes;
    }

    /**
     * service下是否包含clientStreaming或serverStreaming的method
     *
     * @return true of false
     */
    public boolean hasStreamingMethod() {
        for (ProtoMethod method : methods) {
            if (method.isClientStreaming() || method.isServerStreaming()) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public List<ProtoMethod> getMethods() {
        return methods;
    }

    public String getPackageName() {
        return packageName;
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

    public String getInterfaceNamePrefix() {
        return interfaceNamePrefix;
    }

    public Map<String, ProtoMessageType> getMessageTypes() {
        return messageTypes;
    }

    @Override
    public String toString() {
        return "ProtoService{"
                + "name='" + name + '\''
                + ", packageName='" + packageName + '\''
                + ", javaPackage='" + javaPackage + '\''
                + ", javaOuterClass='" + javaOuterClass + '\''
                + ", multipleClasses=" + multipleClasses
                + ", fallbackClassname='" + fallbackClassname + '\''
                + ", methods=" + methods
                + ", messageTypes=" + messageTypes
                + '}';
    }

    public static ProtoServiceBuilder builder() {
        return new ProtoServiceBuilder();
    }

    public static final class ProtoServiceBuilder {
        private String name;
        private String packageName;
        private String javaPackage;
        private String javaOuterClass;
        private boolean multipleClasses;
        private String fallbackClassname;
        private String interfaceNamePrefix;
        private List<ProtoMethod> methods;

        private ProtoServiceBuilder() {
        }

        public ProtoServiceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProtoServiceBuilder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public ProtoServiceBuilder javaPackage(String javaPackage) {
            this.javaPackage = javaPackage;
            return this;
        }

        public ProtoServiceBuilder javaOuterClass(String javaOuterClass) {
            this.javaOuterClass = javaOuterClass;
            return this;
        }

        public ProtoServiceBuilder multipleClasses(boolean multipleClasses) {
            this.multipleClasses = multipleClasses;
            return this;
        }

        public ProtoServiceBuilder fallbackClassname(String fallbackClassname) {
            this.fallbackClassname = fallbackClassname;
            return this;
        }

        public ProtoServiceBuilder interfaceNamePrefix(String interfaceNamePrefix) {
            this.interfaceNamePrefix = interfaceNamePrefix;
            return this;
        }

        public ProtoServiceBuilder methods(List<ProtoMethod> methods) {
            this.methods = methods;
            return this;
        }

        public ProtoService build() {
            return new ProtoService(this);
        }
    }
}
