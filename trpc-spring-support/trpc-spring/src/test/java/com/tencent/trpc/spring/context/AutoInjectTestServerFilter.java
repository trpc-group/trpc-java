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

package com.tencent.trpc.spring.context;

import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.spring.context.TRpcConfigAutoRegistryTest.InjectByFieldBean;
import com.tencent.trpc.spring.context.TRpcConfigAutoRegistryTest.InjectBySetterBean;
import java.util.concurrent.CompletionStage;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;

public class AutoInjectTestServerFilter implements Filter {

    @Resource
    private InjectByFieldBean injectByFieldBean;
    @Autowired
    private InjectByFieldBean autowiredByFieldBean;

    private InjectBySetterBean injectBySetterBean;

    public InjectByFieldBean getAutowiredByFieldBean() {
        return autowiredByFieldBean;
    }

    public InjectByFieldBean getInjectByFieldBean() {
        return injectByFieldBean;
    }

    public InjectBySetterBean getInjectBySetterBean() {
        return injectBySetterBean;
    }

    @Autowired
    public void setInjectBySetterBean(InjectBySetterBean injectBySetterBean) {
        this.injectBySetterBean = injectBySetterBean;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> filterChain, Request req) {
        return filterChain.invoke(req);
    }
}
