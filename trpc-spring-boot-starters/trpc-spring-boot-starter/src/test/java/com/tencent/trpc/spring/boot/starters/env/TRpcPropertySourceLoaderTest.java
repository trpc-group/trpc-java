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

package com.tencent.trpc.spring.boot.starters.env;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

public class TRpcPropertySourceLoaderTest {

    @Test
    public void test() {
        DeferredLog log = new DeferredLog();
        StandardEnvironment environment = new StandardEnvironment();

        TRpcPropertySourceLoader loader = new TRpcPropertySourceLoader(log, new DefaultResourceLoader());

        // empty locations
        loader.loadInto(environment);
        // wrong locations
        loader.loadInto(environment, "non-existed.yml");
        // wrong extension locations
        loader.loadInto(environment, "non-existed.not_supported_extension");

        log.switchTo(TRpcPropertySourceLoaderTest.class);

        Assert.assertEquals(environment.getPropertySources().size(), 2);
    }
}
