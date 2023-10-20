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

package com.tencent.trpc.core.compressor.spi;

import com.tencent.trpc.core.extension.Extensible;
import java.io.IOException;

@Extensible
public interface Compressor {

    byte[] compress(byte[] src) throws IOException;

    byte[] decompress(byte[] src) throws IOException;

    /**
     * Using the 0-127 framework.
     */
    int type();

    String name();

}
