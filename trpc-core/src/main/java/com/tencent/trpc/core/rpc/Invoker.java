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

package com.tencent.trpc.core.rpc;

import java.util.concurrent.CompletionStage;

/**
 * Method invocation external abstraction.
 */
public interface Invoker<T> {

    /**
     * Get the callee interface.
     */
    Class<T> getInterface();

    /***
     * Abstract method invocation, do not return null.
     */
    CompletionStage<Response> invoke(Request request);

}
