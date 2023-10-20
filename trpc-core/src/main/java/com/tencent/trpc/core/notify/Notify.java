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

package com.tencent.trpc.core.notify;

import java.io.Serializable;
import java.util.EventObject;

public abstract class Notify<T> extends EventObject implements Serializable {

    private final long timestamp = System.currentTimeMillis();

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public Notify(T source) {
        super(source);
    }

    /**
     * Get timestamp on Notify
     */
    public final long getTimestamp() {
        return this.timestamp;
    }

}