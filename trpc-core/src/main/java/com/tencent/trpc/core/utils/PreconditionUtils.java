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

package com.tencent.trpc.core.utils;

/**
 * Assertion utility.
 */
public class PreconditionUtils {

    /**
     * Checks if the given expression is true, and if not, throws an IllegalArgumentException with the specified
     * message.
     *
     * @param express the expression to evaluate
     * @param format the message format string
     * @param args the arguments to the message format string
     * @throws IllegalArgumentException if the expression is false
     */
    public static void checkArgument(boolean express,
            String format,
            Object... args
    ) {
        if (!express) {
            throw new IllegalArgumentException(String.format(format, args));
        }
    }

}
