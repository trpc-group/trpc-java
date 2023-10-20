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

package tests.service;

import com.tencent.trpc.core.rpc.RpcServerContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;

/**
 * Java Bean-based test service.
 */
@TRpcService(name = "trpc.TestApp.TestServer.GreeterJavaBeanService")
public interface GreeterJavaBeanService {


    @TRpcMethod(name = "sayHello")
    ResponseBean sayHello(RpcServerContext context, RequestBean request);

    class ResponseBean {

        String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    class RequestBean {

        String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
