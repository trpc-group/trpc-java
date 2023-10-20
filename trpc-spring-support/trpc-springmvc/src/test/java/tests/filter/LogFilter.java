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

package tests.filter;

import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
public class LogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LogFilter.class);

    public LogFilter() {
        logger.info(">>>>>>>>>>>>>!!!!!!!inited!!!!!!!!<<<<<<<<<<<<<<");
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request req) {
        logger.info("!!!!!!!!!!!!! do trpc filter, invoker info: {}, req: {}", invoker, req);

        return invoker.invoke(req);
    }

}
