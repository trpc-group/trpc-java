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

package com.tencent.trpc.opentelemetry.sdk.metrics;

import com.tencent.trpc.core.admin.spi.Admin;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Implementation of {@link Admin} interface. Export prometheus format metrics.
 */
@Path("")
public class MetricsAdmin implements Admin {

    /**
     * Prometheus healthy
     */
    private static final String HEALTHY_RESPONSE = "Exporter is Healthy.";

    @Path("/metrics")
    @GET
    @Produces({"text/plain; version=0.0.4; charset=utf-8"})
    public String metrics004(@HeaderParam("accept") String accept) {
        return OpenTelemetryMetricsReader.getReader().getMetrics(accept);
    }

    @Path("/metrics")
    @GET
    @Produces({"application/openmetrics-text; version=1.0.0; charset=utf-8"})
    public String metrics100(@HeaderParam("accept") String accept) {
        return OpenTelemetryMetricsReader.getReader().getMetrics(accept);
    }

    @Path("/-/healthy")
    @GET
    @Produces({"text/plain; version=0.0.4; charset=utf-8"})
    public String healthy() {
        return HEALTHY_RESPONSE;
    }
}
