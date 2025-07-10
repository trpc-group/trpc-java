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

package com.tencent.trpc.admin.impl;

import com.tencent.trpc.admin.ApplicationConfigOverview;
import com.tencent.trpc.admin.dto.ConfigOverviewDto;
import com.tencent.trpc.core.admin.spi.Admin;
import com.tencent.trpc.core.common.ConfigManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/cmds")
public class ConfigAdmin implements Admin {

    public ConfigAdmin() {
        ConfigManager applicationConfig = ConfigManager.getInstance();
        ApplicationConfigOverview.init(applicationConfig);
    }

    @Path("/config")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public ConfigOverviewDto getServerConfigInfo() {
        return new ConfigOverviewDto(ApplicationConfigOverview.getInstance());
    }

}
