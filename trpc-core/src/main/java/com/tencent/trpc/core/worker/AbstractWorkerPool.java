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

package com.tencent.trpc.core.worker;

import com.tencent.trpc.core.worker.spi.WorkerPool;
import java.util.concurrent.Executor;

public abstract class AbstractWorkerPool implements WorkerPool {

    public AbstractWorkerPool() {
        super();
    }

    @Override
    public Executor toExecutor() {
        throw new UnsupportedOperationException(
                this.getClass() + " not support toThreadPoolExecutor");
    }
    
}
