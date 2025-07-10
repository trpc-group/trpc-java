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

package tests.service;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import java.util.Map;

@TRpcService(name = "tencent.trpc.http.GreeterJsonService")
public interface GreeterJsonService {

    @TRpcMethod(name = "sayHelloJson")
    Map sayHelloJson(RpcContext context, Map request);

}
