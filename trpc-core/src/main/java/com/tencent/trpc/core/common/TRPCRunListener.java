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

package com.tencent.trpc.core.common;

import com.tencent.trpc.core.extension.Extensible;
import com.tencent.trpc.core.filter.Ordered;

/**
 * TRPC container startup listener.
 *
 * <p>When the TRPC container starts, this interface callback will be triggered.</p>
 */
@Extensible
public interface TRPCRunListener extends Ordered {

    /**
     * TRPC container startup listener, executed before the TRPC container starts the default configuration settings.
     */
    default void starting() {
    }

}
