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

package com.tencent.trpc.admin.custom;

import com.tencent.trpc.core.admin.spi.Admin;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/cmds")
public class TestAdmin implements Admin {

    @Path("/test")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public TestDto report() {
        return new TestDto("hello world!");
    }

}