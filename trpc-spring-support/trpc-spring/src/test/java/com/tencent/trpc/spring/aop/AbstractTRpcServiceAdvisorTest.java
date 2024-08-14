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

package com.tencent.trpc.spring.aop;

import org.aopalliance.aop.Advice;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.aop.Pointcut;

public class AbstractTRpcServiceAdvisorTest {

    private AbstractTRpcServiceAdvisor advisor;

    @Mock
    private Pointcut extraPointcut;

    @Before
    public void setUp() {
        advisor = new AbstractTRpcServiceAdvisor() {
            @Override
            public Advice getAdvice() {
                return new Advice() {
                };
            }

            @Override
            protected Pointcut getExtraPointcut() {
                return extraPointcut;
            }
        };
    }

    @Test
    public void testGetPointcut() {
        Pointcut pointcut = advisor.getPointcut();
        Assert.assertNotNull(pointcut);
    }
}