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

package tests.service.impl1;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import java.util.Collections;
import java.util.Map;
import tests.service.GreeterJsonService;

public class GreeterJsonServiceImpl1 implements GreeterJsonService {

    private static final Logger logger = LoggerFactory.getLogger(GreeterJsonServiceImpl1.class);

    @Override
    public Map sayHelloJson(RpcContext context, Map request) {
        logger.info("got hello json request, request is '{}'", request);

        return Collections.singletonMap("message", "Hi, " + request.get("message"));
    }

}
