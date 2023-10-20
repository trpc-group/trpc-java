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

package com.tencent.trpc.spring.demo.controller;

import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.demo.proto.GreeterService2API;
import com.tencent.trpc.demo.proto.GreeterServiceAPI;
import com.tencent.trpc.demo.proto.HelloRequestProtocol;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    @Autowired
    private GreeterServiceAPI greeterService;

    @Autowired
    private GreeterService2API greeterService2;

    @RequestMapping("/sayHello")
    public Map<String, ?> sayHello(String name) {
        HelloRequestProtocol.HelloRequest request = HelloRequestProtocol.HelloRequest.newBuilder()
                .setMessage(name).build();
        HelloRequestProtocol.HelloResponse response = greeterService.sayHello(new RpcClientContext(), request);
        return Collections.singletonMap("msg", "[SpringMvc with tRPC] " + response.getMessage());
    }

    @RequestMapping("/sayHi")
    public Map<String, ?> sayHi(String name) {
        HelloRequestProtocol.HelloRequest request = HelloRequestProtocol.HelloRequest.newBuilder()
                .setMessage(name).build();
        HelloRequestProtocol.HelloResponse response = greeterService2.sayHi(new RpcClientContext(), request);
        return Collections.singletonMap("msg", "[SpringMvc with tRPC] " + response.getMessage());
    }

}
