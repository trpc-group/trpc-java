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

package com.tencent.trpc.core.trace.spi;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.telemetry.spi.TelemetryFactory;
import io.opentracing.Tracer;

/**
 * OpenTracing link tracking specification plugin, it is recommended to use OpenTelemetry {@link TelemetryFactory}
 * plugin.
 */
@Extensible("tjg")
@Deprecated
public interface TracerFactory {

    /**
     * Get the tracer.
     *
     * @param serverName the server name
     * @param port the server port
     * @return the Tracer instance
     */
    Tracer getTracer(String serverName, Integer port);

}