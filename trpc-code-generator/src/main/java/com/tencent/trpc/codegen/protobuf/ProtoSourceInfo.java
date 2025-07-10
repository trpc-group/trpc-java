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

package com.tencent.trpc.codegen.protobuf;

import com.tencent.trpc.codegen.protobuf.source.model.ProtoService;
import java.util.List;

/**
 * Information of the provided .proto files.
 */
public class ProtoSourceInfo {
    private final List<ProtoService> services;
    private final boolean usingValidator;

    public ProtoSourceInfo(List<ProtoService> services, boolean usingValidator) {
        this.services = services;
        this.usingValidator = usingValidator;
    }

    public List<ProtoService> getServices() {
        return services;
    }

    public boolean isUsingValidator() {
        return usingValidator;
    }

    @Override
    public String toString() {
        return "ProtoSourceInfo{"
                + "services=" + services
                + ", usingValidator=" + usingValidator
                + '}';
    }
}
