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

package com.tencent.trpc.core.rpc.def;

/**
 * Remaining timeout.
 */
public class LeftTimeout {

    /**
     * Original timeout.
     */
    private int originTimeout;
    /**
     * Remaining timeout.
     */
    private int leftTimeout;

    public LeftTimeout(int originTimeout, int leftTimeout) {
        this.originTimeout = originTimeout;
        this.leftTimeout = leftTimeout;
    }

    public int getOriginTimeout() {
        return originTimeout;
    }

    public void setOriginTimeout(int originTimeout) {
        this.originTimeout = originTimeout;
    }

    public int getLeftTimeout() {
        return leftTimeout;
    }

    public void setLeftTimeout(int leftTimeout) {
        this.leftTimeout = leftTimeout;
    }

}
