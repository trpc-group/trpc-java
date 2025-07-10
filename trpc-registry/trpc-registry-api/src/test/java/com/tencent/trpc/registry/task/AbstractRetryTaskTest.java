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

package com.tencent.trpc.registry.task;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.common.timer.Timeout;
import com.tencent.trpc.core.common.timer.Timer;
import com.tencent.trpc.core.common.timer.TimerTask;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import com.tencent.trpc.registry.center.AbstractFailedRetryRegistryCenter;
import com.tencent.trpc.registry.center.NotifyListener;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;


/**
 * Test class for registry failed retry.
 */
public class AbstractRetryTaskTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRetryTaskTest.class);

    AbstractFailedRetryRegistryCenter registry = null;

    AbstractRetryTask abstractRetryTask = null;
    Timeout timeout = new Timeout() {
        @Override
        public Timer timer() {
            return null;
        }

        @Override
        public TimerTask task() {
            return null;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public boolean cancel() {
            return false;
        }
    };

    /**
     * Test class for registry failed retry.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("test", "test");
        PluginConfig pluginConfig = new PluginConfig("id", ThreadWorkerPool.class, properties);
        registry = new AbstractFailedRetryRegistryCenter() {
            @Override
            public void doRegister(RegisterInfo registerInfo) {

            }

            @Override
            public void doUnregister(RegisterInfo registerInfo) {

            }

            @Override
            public void doSubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

            }

            @Override
            public void doUnsubscribe(RegisterInfo registerInfo, NotifyListener notifyListener) {

            }

            @Override
            public void init() throws TRpcExtensionException {

            }

            @Override
            public boolean isAvailable() {
                return false;
            }

        };

        registry.setPluginConfig(pluginConfig);

        abstractRetryTask = new AbstractRetryTask(registry, buildRegisterInfo()) {

            @Override
            protected void doRetry(Timeout timeout) {
            }
        };
    }

    private RegisterInfo buildRegisterInfo() {
        return new RegisterInfo("trpc", "0.0.0.0", 12001,
                "test.service1");
    }

    @Test
    public void retryAgain() {
        try {
            abstractRetryTask.retryAgain(null, 1L);
        } catch (Exception e) {
            LOGGER.error("abstractRetryTask retryAgain ex:", e);
        }

        abstractRetryTask.retryAgain(timeout, 1L);
    }

    @Test
    public void testRetryAgain() {
        abstractRetryTask.retryAgain(timeout);
    }

    @Test
    public void run() {

        Timeout timeout = new Timeout() {
            @Override
            public Timer timer() {
                return null;
            }

            @Override
            public TimerTask task() {
                return null;
            }

            @Override
            public boolean isExpired() {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return true;
            }

            @Override
            public boolean cancel() {
                return true;
            }
        };
        try {
            abstractRetryTask.run(timeout);
        } catch (Exception e) {
            LOGGER.error("abstractRetryTask run ex:", e);
        }
    }

}