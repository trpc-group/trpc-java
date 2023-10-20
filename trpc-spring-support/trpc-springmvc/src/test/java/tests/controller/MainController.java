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

package tests.controller;

import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tests.proto.HelloRequestProtocol;

@RestController
public class MainController {

    @RequestMapping("/hello")
    public Map<String, ?> index(@RequestParam String name) {
        return Collections.singletonMap("msg", "Hello " + name);
    }

    @PostMapping("/test")
    public HelloRequestProtocol.HelloResponse test(@RequestBody HelloRequestProtocol.HelloRequest request) {
        return HelloRequestProtocol.HelloResponse.newBuilder()
                .setMessage(request.getMessage() + "test")
                .build();
    }

}
