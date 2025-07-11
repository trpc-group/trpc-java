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

package com.tencent.trpc.demo.example.yaml;

import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.server.main.Main;
import java.util.Objects;

public class ServerTest {

    public static void main(String[] args) {
        String confPath = Objects.requireNonNull(ServerTest.class.getClassLoader()
                .getResource("trpc_java_server.yaml")).getPath();
        TRpcSystemProperties.setProperties(TRpcSystemProperties.CONFIG_PATH, confPath);
        Main.main(args);
        System.out.println(">>>[server] started");

        // If you want to stop the server programmatically, uncomment the next line.
//        ConfigManager.getInstance().stop();
    }

}
