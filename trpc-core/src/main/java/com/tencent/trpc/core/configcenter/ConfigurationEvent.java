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

package com.tencent.trpc.core.configcenter;

import java.util.Objects;

/**
 * Configuration change event. Event published when configuration of
 * the remote configuration center changes. {@code ConfigurationListener} will listen to this event.
 *
 * @see com.tencent.trpc.core.configcenter.ConfigurationListener
 */
public class ConfigurationEvent<K, V> {

    private final String groupName;

    private final K key;
    /**
     * New value after change
     */
    private final V value;
    /**
     * Type of change, maybe add/modify/delete...
     */
    private final String type;

    public ConfigurationEvent(String groupName, K key, V value, String type) {
        this.groupName = groupName;
        this.key = key;
        this.value = value;
        this.type = type;
    }

    public String getGroupName() {
        return groupName;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigurationEvent<?, ?> event = (ConfigurationEvent<?, ?>) o;
        return Objects.equals(groupName, event.groupName)
                && Objects.equals(key, event.key)
                && Objects.equals(value, event.value)
                && Objects.equals(type, event.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName, key, value, type);
    }

}