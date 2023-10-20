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
 * Common APIs for byte array.
 */
public class BytesUtils {

    /**
     * Get the length of the byte array, not the actual usage length.
     *
     * @param bytes byte array
     * @return array length
     */
    public static int bytesLength(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        return bytes.length;
    }

}
