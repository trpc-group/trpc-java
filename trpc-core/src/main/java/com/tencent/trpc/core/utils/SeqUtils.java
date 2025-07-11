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

package com.tencent.trpc.core.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sequence utility, recommended to use only int.
 */
public class SeqUtils {

    /**
     * The initial default value is 1.
     */
    private static final AtomicInteger INTEGER_SEQ = new AtomicInteger(1);

    public static int genIntegerSeq() {
        return (INTEGER_SEQ.getAndIncrement() & Integer.MAX_VALUE);
    }

}
