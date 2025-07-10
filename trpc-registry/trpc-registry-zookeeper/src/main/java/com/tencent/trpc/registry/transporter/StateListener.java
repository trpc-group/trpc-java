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

package com.tencent.trpc.registry.transporter;

/**
 * Zookeeper connection state listener, triggered when the state changes
 */
public interface StateListener {


    void stateChanged(State state);

    enum State {

        /**
         * Zookeeper connection hangs until connection is re-established
         */
        SUSPENDED,

        /**
         * Connection to zookeeper is lost
         */
        DISCONNECTED,

        /**
         * Connected to zookeeper for the first time
         */
        CONNECTED,

        /**
         * Reconnected to zookeeper
         */
        RECONNECTED;
    }

}
