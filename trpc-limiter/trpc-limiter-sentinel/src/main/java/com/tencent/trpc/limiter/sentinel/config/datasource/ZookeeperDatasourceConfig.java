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

package com.tencent.trpc.limiter.sentinel.config.datasource;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.zookeeper.ZookeeperDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.trpc.core.exception.LimiterDataSourceException;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.StringUtils;
import java.util.List;

/**
 * Configuration class for using Zookeeper as the flow control rule data source.
 */
public class ZookeeperDatasourceConfig extends AbstractDatasourceConfig {

    /**
     * Zookeeper address.
     */
    private String remoteAddress;
    /**
     * Zookeeper node path for flow control rules.
     */
    private String path;

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    protected void validate() {
        if (StringUtils.isEmpty(remoteAddress)) {
            throw new LimiterDataSourceException("sentinel zookeeper datasource config error, remote address is empty");
        }
        if (StringUtils.isEmpty(path)) {
            throw new LimiterDataSourceException("sentinel zookeeper datasource config error, path is empty");
        }
    }

    @Override
    public void register() {
        super.register();
        logger.warn("start to register zookeeper as sentinel flow rule data source, remoteAddress = {}, path = {}",
                remoteAddress, path);
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ZookeeperDataSource<>(remoteAddress, path,
                source -> JsonUtils.fromJson(source, new TypeReference<List<FlowRule>>() {
                }));
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        logger.warn("succeed to register zookeeper as sentinel flow rule datasource");
    }

}
