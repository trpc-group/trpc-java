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

package com.tencent.trpc.admin;

import com.google.common.collect.Maps;
import com.tencent.trpc.admin.dto.rpc.RpcStatsClientDto;
import com.tencent.trpc.admin.dto.rpc.RpcStatsDto;
import com.tencent.trpc.admin.dto.rpc.RpcStatsServiceDto;
import com.tencent.trpc.admin.impl.RpcStatsAdmin;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.config.BackendConfig;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.ServiceConfig;
import com.tencent.trpc.core.management.ForkJoinPoolMXBeanImpl;
import com.tencent.trpc.core.management.ForkJoinPoolMXBean;
import com.tencent.trpc.core.management.ThreadPoolMXBeanImpl;
import com.tencent.trpc.core.worker.WorkerPoolManager;
import com.tencent.trpc.core.worker.handler.TrpcThreadExceptionHandler;
import com.tencent.trpc.core.worker.spi.WorkerPool;
import com.tencent.trpc.core.worker.support.thread.ForkJoinWorkerPool;
import com.tencent.trpc.core.worker.support.thread.ThreadWorkerPool;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * RpcStatsAdminTest
 */
public class RpcStatsAdminTest {
    private MockedStatic<WorkerPoolManager> mockedWorkerPoolManager;

    @BeforeEach
    public void setUp() {
        mockedWorkerPoolManager = Mockito.mockStatic(WorkerPoolManager.class);

    }
    @AfterEach
    void tearDown() {
        if (mockedWorkerPoolManager != null) {
            mockedWorkerPoolManager.close();
        }
    }

    @Test
    public void testRpcStatsAdminEmptyList() {
        List<WorkerPool> workerPoolList = new ArrayList<>();
        WorkerPool workerPool = Mockito.mock(ThreadWorkerPool.class);

        ThreadPoolMXBeanImpl threadPoolMXBean = Mockito.mock(ThreadPoolMXBeanImpl.class);
        Mockito.when(workerPool.report()).thenReturn(threadPoolMXBean);
        workerPoolList.add(workerPool);

        ForkJoinPool forkJoinPool = new ForkJoinPool(1,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null, false);
        WorkerPool forkJoinWorkerPool = Mockito.mock(ForkJoinWorkerPool.class);
        ForkJoinPoolMXBean forkJoinPoolMXBean = new ForkJoinPoolMXBeanImpl(forkJoinPool);
        Mockito.when(forkJoinWorkerPool.report()).thenReturn(forkJoinPoolMXBean);
        workerPoolList.add(forkJoinWorkerPool);
        mockedWorkerPoolManager.when(() -> WorkerPoolManager.getAllInitializedExtension()).thenReturn(workerPoolList);

        AtomicLong atomicLong = new AtomicLong();
        Mockito.when(workerPool.getUncaughtExceptionHandler())
                .thenReturn(new TrpcThreadExceptionHandler(atomicLong, atomicLong, atomicLong));
        ServiceConfig serviceConfig = Mockito.mock(ServiceConfig.class);
        Mockito.when(serviceConfig.getWorkerPoolObj()).thenReturn(workerPool);
        ConfigManager.getInstance().getServerConfig().getServiceMap().put("trpc_test", serviceConfig);

        ClientConfig clientConfig = Mockito.mock(ClientConfig.class);
        Map<String, BackendConfig> backendConfigMap = new HashMap<>();
        BackendConfig backendConfig = Mockito.mock(BackendConfig.class);
        Mockito.when(backendConfig.getWorkerPoolObj()).thenReturn(workerPool);
        backendConfigMap.put("aaa", backendConfig);
        Mockito.when(clientConfig.getBackendConfigMap()).thenReturn(backendConfigMap);
        ConfigManager.getInstance().setClientConfig(clientConfig);
        RpcStatsAdmin rpcStatsAdmin = new RpcStatsAdmin();
        RpcStatsDto rpcStats = rpcStatsAdmin.rpc();
    }

    @Test
    public void testRpcStatsAdmin() {
        WorkerPool workerPool = Mockito.mock(ThreadWorkerPool.class);
        AtomicLong atomicLong = new AtomicLong();
        Mockito.when(workerPool.getUncaughtExceptionHandler())
                .thenReturn(new TrpcThreadExceptionHandler(atomicLong, atomicLong, atomicLong));
        ServiceConfig serviceConfig = Mockito.mock(ServiceConfig.class);
        Mockito.when(serviceConfig.getWorkerPoolObj()).thenReturn(workerPool);
        ConfigManager.getInstance().getServerConfig().getServiceMap().put("trpc_test", serviceConfig);

        ClientConfig clientConfig = Mockito.mock(ClientConfig.class);
        Map<String, BackendConfig> backendConfigMap = new HashMap<>();
        BackendConfig backendConfig = Mockito.mock(BackendConfig.class);
        Mockito.when(backendConfig.getWorkerPoolObj()).thenReturn(workerPool);
        backendConfigMap.put("aaa", backendConfig);
        Mockito.when(clientConfig.getBackendConfigMap()).thenReturn(backendConfigMap);
        ConfigManager.getInstance().setClientConfig(clientConfig);
        RpcStatsAdmin rpcStatsAdmin = new RpcStatsAdmin();
        RpcStatsDto rpcStats = rpcStatsAdmin.rpc();
    }

    @Test
    public void testRpcStatsDto() {
        RpcStatsDto rpcStatsDto = new RpcStatsDto();
        rpcStatsDto.setRpcServiceMap(Maps.newHashMap());
        rpcStatsDto.setRpcVersion("1");
        rpcStatsDto.setRpcFrameThreadCount(1L);
        rpcStatsDto.setErrorcode("1");
        rpcStatsDto.setMessage("1");
        rpcStatsDto.getRpcVersion();
        rpcStatsDto.getRpcFrameThreadCount();
        rpcStatsDto.getRpcServiceMap();
        rpcStatsDto.getRpcClientMap();
        rpcStatsDto.getMessage();
        rpcStatsDto.getRpcServiceCount();
        rpcStatsDto.getErrorcode();
    }

    @Test
    public void testRpcStatsClientDto() {
        WorkerPool workerPool = Mockito.mock(ThreadWorkerPool.class);
        AtomicLong atomicLong = new AtomicLong();
        Mockito.when(workerPool.getUncaughtExceptionHandler())
                .thenReturn(new TrpcThreadExceptionHandler(atomicLong, atomicLong, atomicLong));
        RpcStatsClientDto rpcStatsClientDto = new RpcStatsClientDto(workerPool);
        rpcStatsClientDto.setErrorTotal(1L);
        rpcStatsClientDto.setConnectionCount(1);
        rpcStatsClientDto.getConnectionCount();
        rpcStatsClientDto.setLatency99(BigDecimal.ONE);
        rpcStatsClientDto.setLatencyAvg(Maps.newHashMap());
        rpcStatsClientDto.setLatencyP1(BigDecimal.ONE);
        rpcStatsClientDto.setLatencyP3(BigDecimal.ONE);
        rpcStatsClientDto.setLatencyP2(BigDecimal.ONE);
        rpcStatsClientDto.setReqTotal(1L);
        rpcStatsClientDto.setRspTotal(1L);
        rpcStatsClientDto.setLatency999(BigDecimal.ONE);
        rpcStatsClientDto.setReqActive(1);
        rpcStatsClientDto.getReqActive();
        rpcStatsClientDto.getErrorTotal();
        rpcStatsClientDto.getLatency99();
        rpcStatsClientDto.getLatency999();
        rpcStatsClientDto.getLatencyAvg();
        rpcStatsClientDto.getReqTotal();
        rpcStatsClientDto.getLatencyP3();
        rpcStatsClientDto.getLatencyP2();
        rpcStatsClientDto.getLatencyP1();
        rpcStatsClientDto.getRspTotal();
    }

    @Test
    public void testRpcStatsServiceDto() {
        WorkerPool workerPool = Mockito.mock(ThreadWorkerPool.class);
        AtomicLong atomicLong = new AtomicLong();
        Mockito.when(workerPool.getUncaughtExceptionHandler())
                .thenReturn(new TrpcThreadExceptionHandler(atomicLong, atomicLong, atomicLong));
        RpcStatsServiceDto rpcStatsServiceDto = new RpcStatsServiceDto(
                workerPool);
        rpcStatsServiceDto.setRspAvgLen(1D);
        rpcStatsServiceDto.setErrorTotal(1L);
        rpcStatsServiceDto.setBusinessError(1L);
        rpcStatsServiceDto.setLatency999(BigDecimal.ONE);
        rpcStatsServiceDto.setConnectionCount(1);
        rpcStatsServiceDto.setReqTotal(1L);
        rpcStatsServiceDto.setProtocolError(1L);
        rpcStatsServiceDto.setQps(1L);
        rpcStatsServiceDto.setLatency9999(BigDecimal.ONE);
        rpcStatsServiceDto.setReqAvgLen(1D);
        rpcStatsServiceDto.setReqActive(1);
        rpcStatsServiceDto.setRspTotal(1L);
        rpcStatsServiceDto.setLatencyP1(BigDecimal.ONE);
        rpcStatsServiceDto.setLatencyP2(BigDecimal.ONE);
        rpcStatsServiceDto.setLatencyP3(BigDecimal.ONE);
        rpcStatsServiceDto.setLatencyAvg(Maps.newHashMap());
        rpcStatsServiceDto.getBusinessError();
        rpcStatsServiceDto.getLatencyP1();
        rpcStatsServiceDto.getReqActive();
        rpcStatsServiceDto.getProtocolError();
        rpcStatsServiceDto.getReqTotal();
        rpcStatsServiceDto.getReqAvgLen();
        rpcStatsServiceDto.getLatency9999();
        rpcStatsServiceDto.getLatencyAvg();
        rpcStatsServiceDto.getLatency999();
        rpcStatsServiceDto.getConnectionCount();
        rpcStatsServiceDto.getLatencyP1();
        rpcStatsServiceDto.getLatencyP2();
        rpcStatsServiceDto.getLatencyP3();
        rpcStatsServiceDto.getErrorTotal();
        rpcStatsServiceDto.getQps();
        rpcStatsServiceDto.getRspTotal();
        rpcStatsServiceDto.getRspAvgLen();

        WorkerPool forkJoinPool = Mockito.mock(ForkJoinWorkerPool.class);
        Mockito.when(forkJoinPool.getUncaughtExceptionHandler())
                .thenReturn(new TrpcThreadExceptionHandler(atomicLong, atomicLong, atomicLong));
        RpcStatsServiceDto serviceDto = new RpcStatsServiceDto(
                forkJoinPool);
    }
}
