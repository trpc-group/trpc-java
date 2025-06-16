package com.tencent.trpc.spring.demo.server.impl;

import com.tencent.trpc.GreeterService3API;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GreeterServiceImpl3 implements GreeterService3API {

    private static final Logger logger = LoggerFactory.getLogger(GreeterServiceImpl3.class);

    @Override
    public <T> Map sayHelloParameterized(RpcContext context, RequestParameterizedBean<T> request) {
        logger.info("got hello json request, request is '{}'", request);

        return Collections.singletonMap("message", "Hi:" + request.getData());
    }
}
