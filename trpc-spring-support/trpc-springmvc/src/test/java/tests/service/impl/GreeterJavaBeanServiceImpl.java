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

package tests.service.impl;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcServerContext;
import org.springframework.stereotype.Service;
import tests.service.GreeterJavaBeanService;

/**
 * Java Bean-based test case.
 */
@Service
public class GreeterJavaBeanServiceImpl implements GreeterJavaBeanService {

    private static final Logger logger = LoggerFactory.getLogger(GreeterJavaBeanServiceImpl.class);


    @Override
    public ResponseBean sayHello(RpcServerContext context, RequestBean request) {
        logger.info("got hello request, message is '{}'", request.getMessage());
        ResponseBean responseBean = new ResponseBean();
        responseBean.setMessage(request.getMessage());
        return responseBean;
    }
}
