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
 * Describe a rpc method defined in .proto files
 */
public class ProtoMethod {
    /**
     * method name
     */
    private final String name;
    /**
     * method alias (set by option(trpc.alias))
     */
    private final String alias;
    /**
     * input message type
     */
    private final ProtoMessageType inputType;
    /**
     * output message type
     */
    private final ProtoMessageType outputType;
    /**
     * if the input message is streaming
     */
    private final boolean clientStreaming;
    /**
     * if the output message is streaming
     */
    private final boolean serverStreaming;

    private ProtoMethod(String name,
                       String alias,
                       ProtoMessageType inputType,
                       ProtoMessageType outputType,
                       boolean clientStreaming,
                       boolean serverStreaming) {
        this.name = name;
        this.alias = alias;
        this.inputType = inputType;
        this.outputType = outputType;
        this.clientStreaming = clientStreaming;
        this.serverStreaming = serverStreaming;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public ProtoMessageType getInputType() {
        return inputType;
    }

    public ProtoMessageType getOutputType() {
        return outputType;
    }

    public boolean isClientStreaming() {
        return clientStreaming;
    }

    public boolean isServerStreaming() {
        return serverStreaming;
    }

    @Override
    public String toString() {
        return "ProtoMethod{"
                + "name='" + name + '\''
                + ", alias='" + alias + '\''
                + ", inputType=" + inputType
                + ", outputType=" + outputType
                + ", clientStreaming=" + clientStreaming
                + ", serverStreaming=" + serverStreaming
                + '}';
    }

    public static ProtoMethodBuilder builder() {
        return new ProtoMethodBuilder();
    }

    public static final class ProtoMethodBuilder {
        private String name;
        private String alias;
        private ProtoMessageType inputType;
        private ProtoMessageType outputType;
        private boolean clientStreaming;
        private boolean serverStreaming;

        private ProtoMethodBuilder() {
        }

        public ProtoMethodBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ProtoMethodBuilder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public ProtoMethodBuilder inputType(ProtoMessageType inputType) {
            this.inputType = inputType;
            return this;
        }

        public ProtoMethodBuilder outputType(ProtoMessageType outputType) {
            this.outputType = outputType;
            return this;
        }

        public ProtoMethodBuilder clientStreaming(boolean clientStreaming) {
            this.clientStreaming = clientStreaming;
            return this;
        }

        public ProtoMethodBuilder serverStreaming(boolean serverStreaming) {
            this.serverStreaming = serverStreaming;
            return this;
        }

        public ProtoMethod build() {
            return new ProtoMethod(name, alias, inputType, outputType, clientStreaming, serverStreaming);
        }
    }
}
