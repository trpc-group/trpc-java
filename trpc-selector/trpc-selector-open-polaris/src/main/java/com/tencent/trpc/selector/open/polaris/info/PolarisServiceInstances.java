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

package com.tencent.trpc.selector.open.polaris.info;

import com.tencent.polaris.api.plugin.weight.WeightType;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.Node;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolarisServiceInstances implements ServiceInstances {

    private WeightType weightType;

    private List<Instance> instances;

    private boolean isInitialized;

    private String revision;

    private String service;

    private String namespace;

    private Map<String, String> metadata;

    private int totalWeight;

    private Map<String, Instance> idMap;

    private Map<Node, Instance> nodeMap;

    public PolarisServiceInstances() {
    }

    public PolarisServiceInstances(List<Instance> instances) {
        this.instances = Collections.unmodifiableList(instances);
        this.totalWeight = getTotalWeight(instances);
        idMap = new HashMap<>();
        nodeMap = new HashMap<>();
        for (Instance instance : instances) {
            idMap.put(instance.getId(), instance);
            nodeMap.put(new Node(instance.getHost(), instance.getPort()), instance);
        }
    }


    public void setWeightType(WeightType weightType) {
        this.weightType = weightType;
    }

    @Override
    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public Instance getInstance(Node node) {
        return nodeMap.get(node);
    }

    @Override
    public Instance getInstance(String id) {
        return idMap.get(id);
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public ServiceKey getServiceKey() {
        return new ServiceKey(namespace, service);
    }

    @Override
    public int getTotalWeight() {
        return totalWeight;
    }

    private int getTotalWeight(List<Instance> instances) {
        int totalWeight = 0;
        if (CollectionUtils.isNotEmpty(instances)) {
            for (Instance instance : instances) {
                totalWeight += instance.getWeight();
            }
        }
        return totalWeight;
    }

    public void setTotalWeight(int totalWeight) {
        this.totalWeight = totalWeight;
    }
}
