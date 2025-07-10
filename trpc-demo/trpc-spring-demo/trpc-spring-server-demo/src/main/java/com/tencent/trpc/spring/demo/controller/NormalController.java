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

package com.tencent.trpc.spring.demo.controller;

import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/normal")
public class NormalController {

    @RequestMapping("/hello")
    public Map<String, ?> sayHello(@RequestParam String name) {
        return Collections.singletonMap("msg", "[Normal SpringMvc] hello " + name);
    }

    @RequestMapping("/hi")
    public Map<String, ?> sayHi(@RequestParam String name) {
        return Collections.singletonMap("msg", "[Normal SpringMvc] hi " + name);
    }

}
