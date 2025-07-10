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

package com.tencent.trpc.core.selector.discovery;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.selector.ServiceId;
import com.tencent.trpc.core.selector.ServiceInstance;
import com.tencent.trpc.core.utils.FutureUtils;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ListDiscovery extends AbstractDiscovery implements InitializingExtension {

    private Cache<String, List<ServiceInstance>> listIpsCache;

    private static final String SERVICE_NAME_SPLIT = ",";
    private static final String IP_PORT_SPLIT = ":";
    private static final Integer LIST_IPS_CACHE_MAXIMUM_SIZE = 100;
    private static final Integer LIST_IPS_CACHE_EXPIRE_AFTER_ACCESS_MINUTES = 10;

    /**
     * Build ServiceInstanceList form serviceName,the serviceName like 'ip:port,ip:port'
     * */
    private static List<ServiceInstance> parseServiceId2Instances(String serviceName) {
        List<ServiceInstance> instances = Lists.newArrayList();
        String[] split = serviceName.split(SERVICE_NAME_SPLIT);
        for (String each : split) {
            int index = each.lastIndexOf(IP_PORT_SPLIT);
            String ip = each.substring(0, index);
            int port = Integer.parseInt(each.substring(index + 1));
            instances.add(new ServiceInstance(ip, port));
        }
        return instances;
    }

    @Override
    public void init() throws TRpcExtensionException {
        listIpsCache =
                CacheBuilder.newBuilder().maximumSize(LIST_IPS_CACHE_MAXIMUM_SIZE)
                        .expireAfterAccess(LIST_IPS_CACHE_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
                        .build();
    }

    @Override
    public CompletionStage<List<ServiceInstance>> asyncList(ServiceId serviceId,
            Executor executor) {
        CompletionStage<List<ServiceInstance>> future = FutureUtils.newFuture();
        future.toCompletableFuture().complete(list(serviceId));
        return future;
    }

    @Override
    public List<ServiceInstance> list(ServiceId serviceId) {
        try {
            Objects.requireNonNull(serviceId, "serviceId is null");
            return listIpsCache
                    .get(serviceId.getServiceName(),
                            () -> parseServiceId2Instances(serviceId.getServiceName()));
        } catch (Exception ex) {
            throw new RuntimeException(
                    "discovery exception, serviceId(" + serviceId.toSimpleString() + ")", ex);
        }
    }
}
